package de.xyndra.xyndras_crates.util

import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3


class FloatingPrizeItemEntity(
    world: Level,
    x: Double,
    y: Double,
    z: Double,
    stack: ItemStack,
) : ItemEntity(world, x, y, z, stack) {
    private var ticksElapsed = 0

    init {
        isNoGravity = true
        setPickUpDelay(Int.MAX_VALUE)
        isInvisible = false
        isInvulnerable = true
        deltaMovement = Vec3.ZERO
    }

    override fun tick() {
        super.tick()
        ticksElapsed++
        if (ticksElapsed >= 100) {
            this.kill()
        }
    }

    override fun shouldBeSaved() = false
}
