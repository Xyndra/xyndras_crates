package de.xyndra.xyndras_crates

import com.mojang.logging.LogUtils
import de.xyndra.xyndras_crates.commands.CrateCommand
import de.xyndra.xyndras_crates.lootcrates.BlacklistConfigManager
import de.xyndra.xyndras_crates.lootcrates.CrateConfigManager
import de.xyndra.xyndras_crates.lootcrates.CrateDataManager
import de.xyndra.xyndras_crates.lootcrates.CrateEventHandler
import de.xyndra.xyndras_crates.particles.CrateParticles
import de.xyndra.xyndras_crates.screenhandlers.PrizeDisplayAbstractContainerMenuFactory
import de.xyndra.xyndras_crates.util.ParseableName
import de.xyndra.xyndras_crates.util.Task
import de.xyndra.xyndras_crates.util.WorldBlockPos
import de.xyndra.xyndras_crates.util.setLore
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.RegistryOps
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import org.slf4j.Logger
import java.util.*

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(XyndrasCrates.MODID)
class XyndrasCrates(modEventBus: IEventBus, modContainer: ModContainer) {
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    init {
        LOGGER.info("XyndrasCrates init")
        CrateConfigManager.createCratesFolder()
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { event ->
            CrateCommand.register(event.dispatcher)
        }
        NeoForge.EVENT_BUS.addListener(XyndrasCrates::onServerTick)
        NeoForge.EVENT_BUS.addListener(XyndrasCrates::onPlayerRightClickBlock)
        NeoForge.EVENT_BUS.addListener(XyndrasCrates::onBlockBreak)
        NeoForge.EVENT_BUS.addListener(XyndrasCrates::onPostServerTick)
        NeoForge.EVENT_BUS.addListener(XyndrasCrates::onServerStarting)
    }

    companion object {
        // EVENTS
        fun onServerTick(event: ServerTickEvent.Pre) {
            val currentTick = event.server.allLevels.first().gameTime
            tasks[currentTick]?.let { tasksList ->
                for (task in tasksList) {
                    task.action()
                }
                tasks.remove(currentTick) // Remove the executed tasks from the storage
            }
        }

        fun onPlayerRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
            // Handled in the init block
            val player = event.entity
            val world = event.level
            val hand = event.hand
            val hitResult = event.hitVec
            if (world.isClientSide || hand != InteractionHand.MAIN_HAND) {
                event.cancellationResult = InteractionResult.PASS
                return
            }

            val crateDataManager = CrateDataManager()
            val savedCrateData = crateDataManager.loadCrateData().toMutableMap()

            // Create a world-aware position for the clicked block
            val worldId = getWorldId(world)
            val worldBlockPos = WorldBlockPos(worldId, hitResult.blockPos)

            // Check if the clicked position is in the crate data
            if (worldBlockPos in savedCrateData) {
                var crateName = savedCrateData[worldBlockPos]
                val crateConfig = CrateConfigManager.getCrateConfig(crateName!!)

                if (crateConfig != null && crateConfig.screenName != null) {
                    crateName = crateConfig.screenName
                }

                val parsedKey = BuiltInRegistries.ITEM.get(
                    ResourceLocation.tryParse(
                        crateConfig?.crateKey?.material ?: "minecraft:gold_nugget"
                    )
                )
                val parseKeyStack = ItemStack(parsedKey)
                val crateKeyLore = crateConfig?.crateKey?.lore?.map { Component.literal(it) }
                if (crateKeyLore != null) {
                    setLore(parseKeyStack, crateKeyLore)
                }
                if (crateConfig != null) {
                    val nbt = CustomData.of(CompoundTag().apply { putString("CrateName", crateName) })
                    parseKeyStack.set(DataComponents.CUSTOM_DATA, nbt)
                }

                if (crateConfig != null) {
                    val heldStack = player.mainHandItem
                    val heldStackNbt = heldStack.get(DataComponents.CUSTOM_DATA)?.copyTag()
                    if (heldStack.item == parseKeyStack.item && heldStackNbt != null && heldStackNbt.getString(
                            "CrateName"
                        ) == crateConfig.crateName
                    ) {
                        if (cratesInUse.contains(worldBlockPos)) {
                            player.displayClientMessage(
                                Component.literal("Someone is already using this crate!").withStyle(ChatFormatting.RED), false
                            )
                            event.cancellationResult = InteractionResult.SUCCESS
                            return
                        }

                        val crateEventHandler = CrateEventHandler(
                            world,
                            worldBlockPos,
                            player as ServerPlayer,
                            crateConfig.prize,
                            cratesInUse,
                            playerCooldowns,
                            crateName
                        )

                        if (crateEventHandler.canOpenCrate()) {
                            heldStack.shrink(1)
                            val finalPrize = crateEventHandler.weightedRandomSelection(crateConfig.prize)
                            crateEventHandler.showPrizesAnimation(finalPrize)
                            crateEventHandler.updatePlayerCooldown()
                        }


                        // Floating item will be spawned in the CrateEventHandler's init block
                    } else {
                        // Open crate preview GUI
                        player.openMenu(
                            PrizeDisplayAbstractContainerMenuFactory(
                                ParseableName(crateName).returnMessageAsStyledComponent(), crateConfig
                            )
                        )
                    }
                    event.cancellationResult = InteractionResult.SUCCESS
                    return
                }
            } else {
                // Assign a new crate if the player is holding a named paper
                val heldStack = player.mainHandItem
                if (heldStack.item == Items.PAPER && heldStack.componentsPatch.get(DataComponents.CUSTOM_NAME) != null && heldStack.get(
                        DataComponents.CUSTOM_DATA
                    )?.copyTag()?.contains(
                        "CrateName"
                    ) == true
                ) {
                    val crateName = heldStack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getString("CrateName")
                    if (crateName == null) {
                        event.cancellationResult = InteractionResult.PASS
                        return
                    }
                    savedCrateData[worldBlockPos] = crateName
                    crateDataManager.saveCrateData(savedCrateData)

                    player.displayClientMessage(
                        Component.literal("Assigned a $crateName crate to the block at ${hitResult.blockPos} in $worldId")
                            .withStyle(ChatFormatting.GRAY), false
                    )
                    event.cancellationResult = InteractionResult.SUCCESS
                    return
                }
            }

            event.cancellationResult = InteractionResult.PASS
        }

        fun onBlockBreak(event: BlockEvent.BreakEvent) {
            val world = event.level as? Level ?: return
            if (world.isClientSide) {
                return
            }

            val player = event.player
            val pos = event.pos

            // Load the saved crate data
            val crateDataManager = CrateDataManager()
            val savedCrateData = crateDataManager.loadCrateData().toMutableMap()

            // Create a world-aware position for the broken block
            val worldId = getWorldId(world)
            val worldBlockPos = WorldBlockPos(worldId, pos)

            // Check if the broken block position is in the crate data
            if (worldBlockPos in savedCrateData) {
                // Remove the crate data for this position
                savedCrateData.remove(worldBlockPos)
                crateDataManager.saveCrateData(savedCrateData)

                // Send a message to the player for debugging purposes
                player.displayClientMessage(
                    Component.literal("Crate data removed for position: $pos in $worldId").withStyle(ChatFormatting.GRAY), false
                )
            }
        }

        fun onPostServerTick(event: ServerTickEvent.Post) {
            val server = event.server
            for (world in server.allLevels) {
                if (world is ServerLevel) {
                    spawnParticlesForAllCrates(world)
                }
            }
            CrateParticles.updateTimers()
        }

        fun onServerStarting(event: ServerStartingEvent) {
            LOGGER.info("Xyndra's Crates starting")
            server = event.server
            nbtOps = event.server.registryAccess().createSerializationContext(NbtOps.INSTANCE)
        }

        // Define mod id in a common place for everything to reference
        const val MODID: String = "xyndras_crates"

        // Directly reference a slf4j logger
        val LOGGER: Logger = LogUtils.getLogger()
        val cratesInUse = Collections.synchronizedSet(mutableSetOf<WorldBlockPos>())
        val playerCooldowns: MutableMap<UUID, Long> = Collections.synchronizedMap(mutableMapOf())
        val tasks: MutableMap<Long, MutableList<Task>> = mutableMapOf()
        var server: MinecraftServer? = null
        var nbtOps: RegistryOps<Tag>? = null

        /**
         * Gets the world ResourceLocation string from a World object.
         * Format: "namespace:path" (e.g., "minecraft:overworld", "minecraft:the_nether")
         */
        fun getWorldId(world: Level): String {
            return world.dimension().location().toString()
        }

        private fun spawnParticlesForAllCrates(world: ServerLevel) {
            val crateDataManager = CrateDataManager()
            val savedCrateData = crateDataManager.loadCrateData()
            val blacklist = BlacklistConfigManager().getBlacklist()

            // Get the world ID for the current world
            val currentWorldId = getWorldId(world)

            for (worldBlockPos in savedCrateData.keys) {
                // Only process crates in this world
                if (worldBlockPos.worldId != currentWorldId) continue

                // Skip crates in the blacklist
                if (worldBlockPos in blacklist) continue

                val pos = worldBlockPos.pos
                world.getChunk(pos.x shr 4, pos.z shr 4)

                val playersNearby =
                    world.getPlayersByDistance(pos, 16.0) // Only get players within 16 blocks of the crate block
                for (player in playersNearby) {
                    CrateParticles.spawnCrossSpiralsParticles(player, pos, world)
                }
            }
        }

        private fun ServerLevel.getPlayersByDistance(pos: BlockPos, distance: Double): List<ServerPlayer> {
            return this.players().filter { player ->
                player.distanceToSqr(
                    Vec3(
                        pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
                    )
                ) <= distance * distance
            }
        }
    }
}

