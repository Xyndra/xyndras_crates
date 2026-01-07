package de.xyndra.xyndras_crates.lootcrates

import com.mojang.brigadier.ParseResults
import com.mojang.serialization.Dynamic
import de.xyndra.xyndras_crates.XyndrasCrates
import de.xyndra.xyndras_crates.XyndrasCrates.Companion.server
import de.xyndra.xyndras_crates.particles.CrateParticles
import de.xyndra.xyndras_crates.util.FloatingPrizeItemEntity
import de.xyndra.xyndras_crates.util.ParseableMessage
import de.xyndra.xyndras_crates.util.Task
import de.xyndra.xyndras_crates.util.WorldBlockPos
import net.minecraft.ChatFormatting
import net.minecraft.SharedConstants
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*


class CrateEventHandler(
    private val world: Level,
    private val worldBlockPos: WorldBlockPos,
    private val player: ServerPlayer,
    private val prizes: List<Prize>,
    private val cratesInUse: MutableSet<WorldBlockPos>,
    private val playerCooldowns: MutableMap<UUID, Long>,
    private val crateName: String
) {
    // Extract the BlockPos for convenience
    private val pos: BlockPos = worldBlockPos.pos
    companion object {
        const val COOLDOWN_TIME = 8000L
    }

    private var lastFloatingPrizeItemEntity: FloatingPrizeItemEntity? = null

    private val random = Random()
    fun weightedRandomSelection(prizes: List<Prize>): Prize {
        val totalWeight = prizes.sumOf { it.chance }
        val randomValue = random.nextInt(totalWeight)
        var cumulativeWeight = 0

        for (prize in prizes) {
            cumulativeWeight += prize.chance
            if (randomValue < cumulativeWeight) {
                return prize
            }
        }

        throw IllegalStateException("No prize could be selected.")
    }


    private fun spawnFloatingItem(prize: Prize) {
        if (world is ServerLevel) {
            CrateParticles.rewardParticles(player, pos)

            revealPrize(prize, isFinalPrize = true)
            if (prize.messageToOpener != null && prize.messageToOpener != "") {
                val message = prize.messageToOpener.replace("{prize_name}", prize.name)
                ParseableMessage(message, player, prize.name).send()
            }

            if (prize.broadcast != null && prize.broadcast != "") {
                var broadcast = prize.broadcast.replace("{prize_name}", prize.name)
                broadcast = broadcast.replace("{player_name}", player.name.string)
                broadcast = broadcast.replace("{crate_name}", crateName)
                if (broadcast != "") {
                    ParseableMessage(broadcast, player, prize.name).sendToAll()
                }
            }
        }
    }

    private fun revealPrize(prize: Prize, isFinalPrize: Boolean) {
        // Remove any previous floating item
        removeFloatingItem()

        val parsedPrize = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(prize.material))
        var itemStack = ItemStack(parsedPrize)

        if (prize.nbt?.isNotBlank() == true && prize.nbt != "{}" && prize.nbt != "null") {
                val parsedNbt = TagParser.parseTag(prize.nbt)

                val namespacedKeyPattern = Regex("^[a-z0-9_.-]+:[a-z0-9_/.-]+$")

                val isLegacy = parsedNbt.allKeys.any { !namespacedKeyPattern.matches(it) }
                if (isLegacy) {
                    val legacyNbt = CompoundTag().apply {
                        putString("id", itemStack.itemHolder.registeredName)
                        putInt("Count", prize.amount)
                        put("tag", parsedNbt)
                    }

                    val updatedNbt = server?.fixerUpper?.update(
                        References.ITEM_STACK,
                        Dynamic(XyndrasCrates.nbtOps, legacyNbt),
                        3700,
                        SharedConstants.getCurrentVersion().dataVersion.version
                    )?.value

                    itemStack = ItemStack.CODEC.parse(XyndrasCrates.nbtOps, updatedNbt).result().orElse(ItemStack.EMPTY)
                } else {
                    val updatedNbt =
                        DataComponentPatch.CODEC.parse(XyndrasCrates.nbtOps, TagParser.parseTag(prize.nbt)).result()
                            .orElse(null)
                    itemStack.applyComponents(updatedNbt)
                    itemStack.count = prize.amount
                }
        }

        itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(prize.name))

        var height = pos.y.toDouble()

        if (isFinalPrize) {
            height = pos.y + 1.0
        }
        val spawnPos = Vec3(pos.x + 0.5, height + 1.5, pos.z + 0.5)

        val floatingPrizeItemEntity = FloatingPrizeItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, itemStack)
        world.addFreshEntity(floatingPrizeItemEntity)

        // Store the last spawned floating prize item entity
        synchronized(floatingPrizeItemEntityLock) {
            lastFloatingPrizeItemEntity = floatingPrizeItemEntity
        }
    }




    fun showPrizesAnimation(finalPrize: Prize) {
        if (world is ServerLevel) {
            val currentTime = System.currentTimeMillis()
            val lastCrateOpenTime = playerCooldowns[player.uuid] ?: 0L

            if (currentTime - lastCrateOpenTime < COOLDOWN_TIME) {
                val remainingCooldown = (COOLDOWN_TIME - (currentTime - lastCrateOpenTime)) / 1000
                player.displayClientMessage(
                    Component.literal("You can open another crate in $remainingCooldown seconds.").withStyle(
                        ChatFormatting.RED),
                    false
                )
                return
            }

            cratesInUse.add(worldBlockPos)

            val animationPrizesCount = 10
            val delayBetweenPrizes = 6L // 300 milliseconds converted to ticks

            for (i in 0 until animationPrizesCount) {
                val randomPrize = weightedRandomSelection(prizes)
                addTask(world, delayBetweenPrizes * i) { showRandomPrizeRunnable(randomPrize).run() }
            }

            // Delay the final prize reveal so that the last random prize is shown for a while
            val finalPrizeDelay = delayBetweenPrizes * (animationPrizesCount + 1)

            addTask(world, finalPrizeDelay) {
                revealPrize(finalPrize, true)
                spawnFloatingItem(finalPrize) // Display the final prize as a floating item

                for (command in finalPrize.commands) {
                    val cmd = command.replace("{player_name}", player.name.string)
                    try {
                        val parseResults: ParseResults<CommandSourceStack> =
                            player.server.commands.dispatcher.parse(cmd, player.server.createCommandSourceStack())
                        player.server.commands.dispatcher.execute(parseResults)
                    } catch (e: Exception) {
                        player.displayClientMessage(Component.literal("Error executing command: $command"), false)
                    }
                }
            }

            val removeCrateDelay = finalPrizeDelay + 100 // 5 seconds converted to ticks
            addTask(world, removeCrateDelay) { cratesInUse.remove(worldBlockPos) }
        }
    }


    private fun showRandomPrizeRunnable(prize: Prize) = Runnable {
        revealPrize(prize, false)
        world.playSound(
            null, pos, SoundEvents.NOTE_BLOCK_BANJO.value(), SoundSource.BLOCKS, 0.5f, 1.0f
        )
    }


    private val floatingPrizeItemEntityLock = Any()
    private fun removeFloatingItem() {
        synchronized(floatingPrizeItemEntityLock) {
            lastFloatingPrizeItemEntity?.kill()
            lastFloatingPrizeItemEntity = null
        }
    }

    fun canOpenCrate(): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastCrateOpenTime = playerCooldowns[player.uuid] ?: 0L

        if (currentTime - lastCrateOpenTime < COOLDOWN_TIME) {
            val remainingCooldown = (COOLDOWN_TIME - (currentTime - lastCrateOpenTime)) / 1000
            player.displayClientMessage(
                Component.literal("You can open another crate in $remainingCooldown seconds.").withStyle(ChatFormatting.RED),
                false
            )
            return false
        }
        return true
    }

    fun updatePlayerCooldown() {
        val currentTime = System.currentTimeMillis()
        playerCooldowns[player.uuid] = currentTime
    }

    fun addTask(world: ServerLevel, tickDelay: Long, action: () -> Unit) {
        val currentTick = world.gameTime
        val taskTick = currentTick + tickDelay
        val task = Task(world, taskTick, action)
        XyndrasCrates.tasks.getOrPut(taskTick) { mutableListOf() }.add(task)
    }
}
