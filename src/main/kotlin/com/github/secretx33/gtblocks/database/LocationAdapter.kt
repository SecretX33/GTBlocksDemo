package com.github.secretx33.gtblocks.database

import com.github.secretx33.gtblocks.util.extension.toUuid
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import java.lang.reflect.Type

class LocationAdapter : JsonSerializer<Location>, JsonDeserializer<Location> {

    override fun serialize(location: Location, type: Type, context: JsonSerializationContext): JsonElement {
        val world = location.world ?: throw IllegalStateException("cannot serialize location with null world, location = $location")
        return JsonObject().apply {
            addProperty("world_uuid", world.uid.toString())
            addProperty("x", location.blockX)
            addProperty("y", location.blockY)
            addProperty("z", location.blockZ)
        }
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Location {
        val root = json.asJsonObject

        root.run {
            val world = Bukkit.getWorld(get("world_uuid").asString.toUuid())
            val x = get("x").asDouble
            val y = get("y").asDouble
            val z = get("z").asDouble
            return Location(world, x, y, z)
        }
    }
}
