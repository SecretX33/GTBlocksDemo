package com.github.secretx33.gtblocks.model

import java.util.UUID

/**
 * Represents the result of a "purge" operation in the database, containing how many entries from
 * unloaded worlds were removed, and how many blocks from loaded worlds were removed (that didn't
 * match their original material, probably because they got broken when the plugin was disabled
 * or something).
 *
 * @property removedWorlds Map<UUID, Int> A map containing all the removed (unloaded) worlds mapped to the number of entries of each world that were removed from the database. The key is the world UUID.
 * @property removedBlocks Map<UUID, Int> A map containing how many blocks were removed from each world that didn't match their original, stored material. The key is the world UUID.
 */
data class PurgeResult(
    val removedWorlds: Map<UUID, Int>,
    val removedBlocks: Map<UUID, Int>,
)
