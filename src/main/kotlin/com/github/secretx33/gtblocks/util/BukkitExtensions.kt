package com.github.secretx33.gtblocks.util.extension

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.WordUtils
import org.bukkit.Material
import org.bukkit.block.Block
import java.util.UUID

fun String.toUuid(): UUID = UUID.fromString(this)

fun String.normalizeSpaces(): String = StringUtils.normalizeSpace(this)

fun String.capitalizeFully(): String = WordUtils.capitalizeFully(this)

fun Block.isAir(): Boolean = type == Material.AIR

fun Block.coordinates(): String = "${location.x.toLong()} ${location.y.toLong()} ${location.z.toLong()}"

val Block.displayType: String get() = type.name.replace('_', ' ').capitalizeFully()
