package de.xyndra.xyndras_crates.screenhandlers.admin

import de.xyndra.xyndras_crates.lootcrates.CrateConfigManager
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore

class IndividualCrateConfigAbstractContainerMenu(syncId: Int, private val player: Player, private val crateName: String) :
    ChestMenu(
        MenuType.GENERIC_9x6, syncId, player.inventory, SimpleContainer(9 * 6), 6
    ) {

        private val crateConfigManager = CrateConfigManager

    init {
        val existingCrates = crateConfigManager.loadCrateConfigs()
        val currentCrateConfig = existingCrates.first { it.crateName == crateName }

        val inventory = container

        val crateItems = currentCrateConfig.prize

        for (i in 0 until inventory.containerSize) {
            inventory.setItem(i, ItemStack.EMPTY)
        }

        for ((index, prize) in crateItems.withIndex()) {
            val materialResourceLocation = ResourceLocation.tryParse(prize.material)
            if (materialResourceLocation != null) {
                val item = BuiltInRegistries.ITEM.get(materialResourceLocation)
                if (item != Items.AIR) {
                    val itemStack = ItemStack(item, prize.amount)
                    val nbt = CompoundTag().apply {
                        putString("XyndrasCratesNBT", prize.nbt ?: "")
                    }
                    itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
                    setLore(itemStack, prize.commands.map { Component.literal(it) })
                    inventory.setItem(index, itemStack)
                }
            }
        }
    }

    private fun setLore(itemStack: ItemStack, lore: List<Component>) {
        val itemLore = ItemLore(lore)
        itemStack.set(DataComponents.LORE, itemLore)
    }
}
