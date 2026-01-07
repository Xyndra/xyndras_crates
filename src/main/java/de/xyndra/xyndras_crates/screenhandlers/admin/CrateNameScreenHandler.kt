package de.xyndra.xyndras_crates.screenhandlers.admin

import de.xyndra.xyndras_crates.lootcrates.CrateConfigManager
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class CrateNameAbstractContainerMenu(syncId: Int, private val player: Player) : ChestMenu(
    MenuType.GENERIC_9x6, syncId, player.inventory, SimpleContainer(9), 1
) {

    init {
        val inventory = container
        val crateConfigManager = CrateConfigManager

        // Fill the inventory with paper with modified name
        for (i in 1 until 9) {
            val paper = ItemStack(Items.PAPER)
            paper.set(DataComponents.CUSTOM_NAME, Component.literal("Add item to empty slot to name crate"))
            inventory.setItem(i, paper)
        }
        val existingCrates = crateConfigManager.loadCrateConfigs()

        // Add the crate name slot
        addSlot(Slot(inventory, 0, 8, 18))
    }


    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun clicked(slotIndex: Int, clickData: Int, actionType: ClickType, player: Player) {
        if (slotIndex == 0 && container.getItem(0).item == Items.NAME_TAG) {
            val crateName = container.getItem(0).displayName.string
            // Check if the crate name is already taken
            val existingCrates = CrateConfigManager.loadCrateConfigs()
            for (crateConfig in existingCrates) {
                if (crateConfig.crateName == crateName) {
                    player.displayClientMessage(Component.literal("Crate name already taken"), false)
                    return
                }
            }
            player.openMenu(SimpleMenuProvider({ syncId, _, p ->
                PreviewIconScreenHandler(syncId, p, crateName)
            }, Component.literal("Loot Icon Editor")))
        } else {
            super.clicked(slotIndex, clickData, actionType, player)
        }
    }
}
