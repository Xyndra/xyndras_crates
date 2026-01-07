package de.xyndra.xyndras_crates.screenhandlers.admin.cratelist

import de.xyndra.xyndras_crates.XyndrasCrates
import de.xyndra.xyndras_crates.XyndrasCrates.Companion.server
import de.xyndra.xyndras_crates.lootcrates.BlacklistConfigManager
import de.xyndra.xyndras_crates.lootcrates.CrateDataManager
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments

class ActiveCrateList(syncId: Int, val player: Player) :
    ChestMenu(MenuType.GENERIC_9x6, syncId, player.inventory, SimpleContainer(9 * 6), 6) {

    private val activeCrates = CrateDataManager().loadCrateData()

    private val blacklistManager = BlacklistConfigManager()

    init {
        initializeInventory()
    }


    override fun stillValid(player: Player): Boolean {
        return true
    }

    private fun initializeInventory() {
        val blacklist = blacklistManager.getBlacklist()
        for ((index, crateName) in activeCrates.values.withIndex()) {
            val worldBlockPos = activeCrates.keys.elementAt(index)

            // Try to get the block from the correct world
            val world = server?.allLevels?.find { XyndrasCrates.getWorldId(it) == worldBlockPos.worldId }
            val crateItem = if (world != null) {
                val blockOnPos = world.getBlockState(worldBlockPos.pos).block
                blockOnPos.asItem().defaultInstance
            } else {
                // World not loaded, show a placeholder
                Items.BARRIER.defaultInstance
            }

            // Show world info in the name
            val worldName = worldBlockPos.worldId.substringAfter(":")
            crateItem.set(DataComponents.CUSTOM_NAME, Component.literal("[$worldName] ${worldBlockPos.pos.x}, ${worldBlockPos.pos.y}, ${worldBlockPos.pos.z} - $crateName"))

            if (!blacklist.contains(worldBlockPos)) {
                val vanishingEnchant = server!!.allLevels.first().registryAccess().registry(Registries.ENCHANTMENT)
                    .get().getHolder(Enchantments.VANISHING_CURSE).get()
                crateItem.enchant(vanishingEnchant, 1)
            }
            container.setItem(index, crateItem)
        }
    }

    override fun clicked(slotIndex: Int, button: Int, actionType: ClickType, player: Player) {
        if (actionType == ClickType.THROW || actionType == ClickType.CLONE || actionType == ClickType.SWAP || actionType == ClickType.PICKUP_ALL) {
            return
        }

        if (slotIndex >= activeCrates.size) {
            return
        }

        val worldBlockPos = activeCrates.keys.elementAt(slotIndex)

        val blacklist = blacklistManager.getBlacklist()
        if (blacklist.contains(worldBlockPos)) {
            blacklistManager.removeFromBlacklist(worldBlockPos)
        } else {
            blacklistManager.addToBlacklist(worldBlockPos)
        }

        // close and reopen screen
        player.containerMenu.removed(player)
        player.openMenu(SimpleMenuProvider({ syncId, _, p ->
            ActiveCrateList(syncId, p)
        }, Component.literal("Blacklist Particles")))

        return
    }
}
