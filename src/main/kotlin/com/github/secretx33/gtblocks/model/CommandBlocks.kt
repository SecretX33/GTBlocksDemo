package com.github.secretx33.gtblocks.model

import org.jetbrains.exposed.dao.id.IntIdTable

object CommandBlocks : IntIdTable() {
    val worldUuid = char("world_uuid", 36).index()
    val x = integer("block_x").index()
    val y = integer("block_y").index()
    val z = integer("block_z").index()
    val material = varchar("block_material", 80)
    val leftClickCommands = varchar("left_click_commands", 3500)
    val rightClickCommands = varchar("right_click_commands", 3500)
}
