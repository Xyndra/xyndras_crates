package de.xyndra.xyndras_crates.util

import com.google.gson.*
import com.mojang.serialization.Dynamic
import de.xyndra.xyndras_crates.XyndrasCrates
import de.xyndra.xyndras_crates.XyndrasCrates.Companion.server
import net.minecraft.SharedConstants
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.item.ItemStack
import java.lang.reflect.Type

data class ItemConfig(
    val itemId: String, val nbt: String?, val amount: Int, val displayName: String?
) {
    fun toItemStack(): ItemStack {
        val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId))
        var itemStack = ItemStack(item, amount)

        // Apply NBT data if present
        if (nbt != null) {
            val parsedNbt = TagParser.parseTag(nbt)

            val namespacedKeyPattern = Regex("^[a-z0-9_.-]+:[a-z0-9_/.-]+$")

            val isLegacy = parsedNbt.allKeys.any { !namespacedKeyPattern.matches(it) }
            if (isLegacy) {
                val legacyNbt = CompoundTag().apply {
                    putString("id", itemStack.itemHolder.registeredName)
                    putInt("Count", amount)
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
                    DataComponentPatch.CODEC.parse(XyndrasCrates.nbtOps, TagParser.parseTag(nbt)).result()
                        .orElse(null)
                itemStack.applyComponents(updatedNbt)
                itemStack.count = amount
            }
        }

        if (displayName != null) {
            itemStack.set(DataComponents.CUSTOM_NAME, Component.Serializer.fromJson(displayName, RegistryAccess.EMPTY))
        }

        return itemStack
    }
}


class ItemStackTypeAdapter : JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
    override fun serialize(
        itemStack: ItemStack, type: Type, jsonSerializationContext: JsonSerializationContext
    ): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty("itemId", itemStack.itemHolder.registeredName)
        jsonObject.addProperty("amount", itemStack.count)
        if (itemStack.get(DataComponents.CUSTOM_NAME) != null) {
            jsonObject.addProperty("displayName", itemStack.displayName.string)
        }
        // Save NBT data
        if (itemStack.componentsPatch.size() > 0) {
            val nbtString = DataComponentPatch.CODEC.encodeStart(XyndrasCrates.nbtOps, itemStack.componentsPatch).result().orElse(null)
            jsonObject.addProperty("nbt", nbtString.asString)
        }
        if (itemStack.componentsPatch.size() > 0 && itemStack.get(DataComponents.LORE)?.lines?.isNotEmpty() == true
        ) {
            val loreJsonArray = JsonArray()
            val loreNbtList = itemStack.get(DataComponents.LORE)?.lines ?: return jsonObject
            for (i in 0 until loreNbtList.size) {
                loreJsonArray.add(loreNbtList[i].string)
            }
            jsonObject.add("lore", loreJsonArray)
        }
        return jsonObject
    }

    override fun deserialize(
        jsonElement: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext
    ): ItemStack {
        val jsonObject = jsonElement.asJsonObject
        val itemId = jsonObject.get("itemId").asString
        val amount = jsonObject.get("amount").asInt
        val displayName = if (jsonObject.has("displayName")) jsonObject.get("displayName").asString else null
        val nbt = if (jsonObject.has("nbt")) jsonObject.get("nbt").asString else null
        val itemConfig = ItemConfig(itemId, nbt, amount, displayName)
        return itemConfig.toItemStack()
    }
}
