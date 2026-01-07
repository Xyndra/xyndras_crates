package de.xyndra.xyndras_crates.util

import net.minecraft.server.level.ServerLevel
data class Task(val world: ServerLevel, val tick: Long, val action: () -> Unit)
