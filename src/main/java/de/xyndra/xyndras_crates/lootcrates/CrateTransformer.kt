package de.xyndra.xyndras_crates.lootcrates

import com.mojang.serialization.Dynamic
import de.xyndra.xyndras_crates.XyndrasCrates
import de.xyndra.xyndras_crates.XyndrasCrates.Companion.server
import de.xyndra.xyndras_crates.util.ParseableMessage
import de.xyndra.xyndras_crates.util.setLore
import net.minecraft.ChatFormatting
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData

class CrateTransformer(val crateName: String, val player: Player) {

    val crateConfig = CrateConfigManager.getCrateConfig(crateName)

    private val crateItemStack = ItemStack(Items.PAPER)

    fun giveTransformer() {
        // create MutableComponent list with instructions
        val instructions = mutableListOf<Component>()

        // add instructions to list
        instructions.add(Component.literal("Right click any block to").withStyle(ChatFormatting.GOLD))
        instructions.add(Component.literal("transform it into a $crateName").withStyle(ChatFormatting.GOLD))
        setLore(crateItemStack, instructions)

        val nbt = CustomData.of(CompoundTag().apply {
            putString("CrateName", crateName)
        })
        crateItemStack.set(DataComponents.CUSTOM_DATA, nbt)
        crateItemStack.set(DataComponents.CUSTOM_NAME, Component.literal(crateName))

        player.displayClientMessage(Component.literal("Giving $crateName to ${player.name.string}"), false)

        player.addItem(crateItemStack)
        val message = "Successfully gave $crateName to ${player.name.string}"
        ParseableMessage(message, player as ServerPlayer, "placeholder").send()
    }

    fun giveKey(amount: Int = 1, admin: Player) {
        val materialResourceLocation = ResourceLocation.tryParse(crateConfig!!.crateKey.material)
        if (materialResourceLocation != null) {
            val item = BuiltInRegistries.ITEM.get(materialResourceLocation)
            if (item != Items.AIR) {
                var crateKeyItemStack = ItemStack(item, amount)
                val parsedName = ParseableMessage(
                    crateConfig.crateKey.name, player as ServerPlayer, "placeholder"
                ).returnMessageAsStyledComponent()

                if (!crateConfig.crateKey.nbt.isNullOrEmpty() && crateConfig.crateKey.nbt != "{}") {
                    val parsedNbt = TagParser.parseTag(crateConfig.crateKey.nbt)

                    val namespacedKeyPattern = Regex("^[a-z0-9_.-]+:[a-z0-9_/.-]+$")

                    val isLegacy = parsedNbt.allKeys.any { !namespacedKeyPattern.matches(it) }
                    if (isLegacy) {
                        val legacyNbt = CompoundTag().apply {
                            putString("id", crateKeyItemStack.itemHolder.registeredName)
                            putInt("Count", amount)
                            put("tag", parsedNbt)
                        }

                        val updatedNbt = server?.fixerUpper?.update(
                            References.ITEM_STACK,
                            Dynamic(XyndrasCrates.nbtOps, legacyNbt),
                            3700,
                            SharedConstants.getCurrentVersion().dataVersion.version
                        )?.value

                        crateKeyItemStack = ItemStack.CODEC.parse(XyndrasCrates.nbtOps, updatedNbt).result().orElse(ItemStack.EMPTY)
                    } else {
                        val updatedNbt =
                            DataComponentPatch.CODEC.parse(XyndrasCrates.nbtOps, TagParser.parseTag(crateConfig.crateKey.nbt)).result()
                                .orElse(null)
                        crateKeyItemStack.applyComponents(updatedNbt)
                        crateKeyItemStack.count = amount
                    }
                }

                val compoundTag = crateKeyItemStack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.apply {
                    putString("CrateName", crateConfig.crateName)
                } ?: CompoundTag().apply {
                    putString("CrateName", crateConfig.crateName)
                }

                crateKeyItemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(compoundTag))

                crateKeyItemStack.set(DataComponents.CUSTOM_NAME, parsedName)
                // Set the lore for the crate key item
                val crateKeyLore = crateConfig.crateKey.lore
                val parsedCrateKeyLore = crateKeyLore.map {
                    ParseableMessage(it, player, "placeholder").returnMessageAsStyledComponent()
                }
                setLore(crateKeyItemStack, parsedCrateKeyLore)

                player.inventory.placeItemBackInInventory(crateKeyItemStack)

                val message = "You received $amount ${crateConfig.crateKey.name} for ${crateConfig.crateName}!"
                ParseableMessage(message, player, "placeholder").send()
            }
        }
    }
}
