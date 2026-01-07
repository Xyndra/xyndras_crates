package de.xyndra.xyndras_crates.screenhandlers.admin.crateconfig

import de.xyndra.xyndras_crates.lootcrates.CrateConfigManager
import de.xyndra.xyndras_crates.lootcrates.CrateTransformer
import de.xyndra.xyndras_crates.screenhandlers.admin.cratelist.CrateListAbstractContainerMenu
import net.minecraft.ChatFormatting
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class CrateConfigAbstractContainerMenu(
    syncId: Int, player: Player, crateName: String
) : ChestMenu(MenuType.GENERIC_9x3, syncId, player.inventory, SimpleContainer(9 * 3), 3) {

    init {
        val inventory = container
        for (i in 0 until inventory.containerSize) {
            inventory.setItem(i,
                ItemStack(Items.GRAY_STAINED_GLASS_PANE).apply { set(DataComponents.CUSTOM_NAME, Component.literal("")) })
        }

        inventory.setItem(12, ItemStack(Items.PAPER).apply {
            set(
                DataComponents.CUSTOM_NAME, Component.literal("Get Crate").withStyle(ChatFormatting.GOLD)
            )
        })

        inventory.setItem(13, ItemStack(Items.TRIPWIRE_HOOK).apply {
            set(DataComponents.CUSTOM_NAME, Component.literal("Get Key").withStyle(ChatFormatting.GOLD))
        })

        inventory.setItem(14, ItemStack(Items.ITEM_FRAME).apply {
            set(
                DataComponents.CUSTOM_NAME, Component.literal("Configure Prize (Web Editor)").withStyle(ChatFormatting.GOLD)
            )
        })

        inventory.setItem(18, ItemStack(Items.ARROW).apply {
            set(DataComponents.CUSTOM_NAME, Component.literal("Back").withStyle(ChatFormatting.RED))
        })
    }

    val crateConfigManager = CrateConfigManager
    val crateConfig = crateConfigManager.getCrateConfig(crateName)
    override fun clicked(slotIndex: Int, button: Int, actionType: ClickType, player: Player) {
        if (actionType == ClickType.THROW || actionType == ClickType.CLONE || actionType == ClickType.SWAP || actionType == ClickType.PICKUP_ALL) {
            return
        }

        val crateTransformer = CrateTransformer(crateConfig!!.crateName, player!!)
        if (slotIndex == 18) {
            player.openMenu(SimpleMenuProvider({ syncId, _, p ->
                CrateListAbstractContainerMenu(syncId, p)
            }, Component.literal("Crate Management")))
        }

        if (slotIndex == 12) {
            crateTransformer.giveTransformer()
        }

        if (slotIndex == 13) {
            crateTransformer.giveKey(1, player)
        }

        if (slotIndex == 14) {
            val url = "https://pebblescrate.sethi.tech/"
            val clickableLink =
                Component.Serializer.fromJson(("{\"text\":\"$url\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"$url\"}}"), RegistryAccess.EMPTY)!!
            player.displayClientMessage(
                Component.literal("To edit the config on the web UI, navigate to: ").withStyle(ChatFormatting.GOLD)
                    .append(clickableLink), false
            )
        }
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

}
