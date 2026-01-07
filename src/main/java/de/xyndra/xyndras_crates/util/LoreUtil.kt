package de.xyndra.xyndras_crates.util

import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore


fun setLore(itemStack: ItemStack, lore: List<Component>) {
    val ItemLore = ItemLore(lore)
    itemStack.set(DataComponents.LORE, ItemLore)
}
