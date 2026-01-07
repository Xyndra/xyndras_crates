package de.xyndra.xyndras_crates.screenhandlers.admin

import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.math.BigDecimal

class IndividualRewardEditingAbstractContainerMenu(
    syncId: Int,
    player: Player,
    private val crateName: String,
    private val previewItem: ItemStack,
    private val weight: BigDecimal
) : ChestMenu(MenuType.GENERIC_9x1, syncId, player.inventory, SimpleContainer(9), 1) {

    init {
        // Add slots for the reward items or command rewards
        for (i in 0 until 9) {
            addSlot(Slot(container, i, 8 + i * 18, 18))
        }
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }
}
