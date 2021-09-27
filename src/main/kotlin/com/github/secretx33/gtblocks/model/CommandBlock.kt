package com.github.secretx33.gtblocks.model

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import java.util.Objects
import java.util.UUID

data class CommandBlock(
    val worldUuid: UUID,
    val x: Int,
    val y: Int,
    val z: Int,
    val material: Material,
    val leftClickCommands: List<String> = emptyList(),
    val rightClickCommands: List<String> = emptyList(),
) {
    val location: Location = Location(Bukkit.getWorld(worldUuid), x.toDouble(), y.toDouble(), z.toDouble())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandBlock

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (worldUuid != other.worldUuid) return false
        if (material != other.material) return false

        return true
    }

    override fun hashCode(): Int = Objects.hash(worldUuid, x, y, z, material)

    companion object {
        fun adapt(block: Block): CommandBlock {
            val world = block.world ?: throw IllegalArgumentException("world from block cannot be null")
            return CommandBlock(
                worldUuid = world.uid,
                x = block.x,
                y = block.y,
                z = block.z,
                material = block.type,
            )
        }
    }
}
