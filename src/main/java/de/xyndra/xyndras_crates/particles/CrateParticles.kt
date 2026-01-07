package de.xyndra.xyndras_crates.particles

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import kotlin.math.cos
import kotlin.math.sin

object CrateParticles {
    private var stepX = 1

    private const val particles = 2
    private const val particlesPerRotation = 20
    private const val radius = 1

    fun updateTimers() {
        stepX++
    }

    fun spawnSpiralParticles(player: ServerPlayer, pos: BlockPos, world: ServerLevel) {
        for (stepY in 0 until 60 step (120 / particles)) {
            val dx = -(cos(((stepX + stepY) / particlesPerRotation.toDouble()) * Math.PI * 2)) * radius
            val dy = stepY / particlesPerRotation.toDouble() / 2.0
            val dz = -(sin(((stepX + stepY) / particlesPerRotation.toDouble()) * Math.PI * 2)) * radius

            val x = pos.x + 0.5 + dx
            val y = pos.y + 0.5 + dy
            val z = pos.z + 0.5 + dz

            val particlePacket = ClientboundLevelParticlesPacket(
                ParticleTypes.SOUL_FIRE_FLAME, false, x, y, z, 0.0f, 0.0f, 0.0f, 0.0f, 1
            )
            player.connection.send(particlePacket)
        }
    }

    fun spawnCrossSpiralsParticles(player: ServerPlayer, pos: BlockPos, world: ServerLevel) {
        for (stepY in 0 until 60 step (120 / particles)) {
            val dx = -(cos(((stepX + stepY) / particlesPerRotation.toDouble()) * Math.PI * 2)) * radius
            val dy = stepY / particlesPerRotation.toDouble() / 2.0
            val dz = -(sin(((stepX + stepY) / particlesPerRotation.toDouble()) * Math.PI * 2)) * radius

            val x = pos.x + 0.5 + dx
            val y = pos.y + 1.5 + dy
            val z = pos.z + 0.5 + dz

            val particlePacket = ClientboundLevelParticlesPacket(
                ParticleTypes.FIREWORK, false, x, y, z, 0.0f, 0.0f, 0.0f, 0.0f, 1
            )
            player.connection.send(particlePacket)
        }
    }

    fun rewardParticles(player: ServerPlayer, pos: BlockPos) {
        val world = player.level()

        for (i in 0 until 5) {
            world.playSound(
                null as ServerPlayer?, pos, SoundEvents.ALLAY_DEATH, SoundSource.BLOCKS, 0.5f, 0.5f
            )
            world.playSound(
                null as ServerPlayer?, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5f, 1f
            )
        }

        val offsetX = 0.5
        val offsetY = 0.2
        val offsetZ = 0.5

        val particlePacket = ClientboundLevelParticlesPacket(
            ParticleTypes.SCULK_SOUL,
            false,
            pos.x + offsetX,
            pos.y + 0.5 + offsetY,
            pos.z + offsetZ,
            0.0f,
            0.0f,
            0.0f,
            0.1f,
            50
        )
        player.connection.send(particlePacket)
    }
}
