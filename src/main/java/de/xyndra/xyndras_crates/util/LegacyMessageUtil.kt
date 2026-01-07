package de.xyndra.xyndras_crates.util

import de.xyndra.xyndras_crates.util.legacyserializer.LegacyComponentSerializer
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.core.RegistryAccess
import net.minecraft.server.level.ServerPlayer

fun parse(text: String, vararg placeholders: Any): Component {
    val legecySerializer = LegacyComponentSerializer.legacyAmpersand()
    val formattedComponent = String.format(text, *placeholders)
    return legecySerializer.deserialize(formattedComponent).decoration(TextDecoration.ITALIC, false)
}

fun parseMessageWithStyles(text: String, prizeName: String): Component {
    val legecySerializer = LegacyComponentSerializer.legacyAmpersand()
    val styledPrizeName = legecySerializer.deserialize(prizeName).decoration(TextDecoration.ITALIC, false)

    val parts = text.split(Regex.escape("{prize_name}"))

    val styledParts = mutableListOf<Component>()

    for (i in parts.indices) {
        val styledPart = legecySerializer.deserialize(parts[i])
        styledParts.add(styledPart)
        if (i < parts.size - 1) {
            styledParts.add(styledPrizeName)
        }
    }

    return Component.join(JoinConfiguration.noSeparators(), styledParts)
}

class ParseableMessage(
    private val message: String,
    private val player : ServerPlayer? = null,
    private val prizeName: String,
) {
    fun sendToAll() {
        val component = parseMessageWithStyles(message, prizeName)
        val serverAudiences = MinecraftServerAudiences.of(player!!.server)
        serverAudiences.all().sendMessage(component)
    }

    fun send() {
        val component = parseMessageWithStyles(message, prizeName)
        val serverAudiences = MinecraftServerAudiences.of(player!!.server)
        serverAudiences.player(player.uuid).sendMessage(component)
    }

    fun returnMessageAsStyledComponent(): net.minecraft.network.chat.MutableComponent {
        val component = parseMessageWithStyles(message, prizeName)
        val gson = GsonComponentSerializer.gson()
        val json = gson.serialize(component)
        return net.minecraft.network.chat.Component.Serializer.fromJson(json, RegistryAccess.EMPTY) as net.minecraft.network.chat.MutableComponent
    }
}

class ParseableName(
    private val name: String,
) {
    fun returnMessageAsStyledComponent(): net.minecraft.network.chat.MutableComponent {
        val component = parseMessageWithStyles(name, "")
        val gson = GsonComponentSerializer.gson()
        val json = gson.serialize(component)
        return net.minecraft.network.chat.Component.Serializer.fromJson(json, RegistryAccess.EMPTY) as net.minecraft.network.chat.MutableComponent
    }
}
