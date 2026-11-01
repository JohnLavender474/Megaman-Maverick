package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapProperties
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.screens.levels.camera.CameraScrollSection
import com.megaman.maverick.game.screens.levels.camera.CameraZone

/**
 * Reads the `camera_sections` layer. Objects with `type=zone` become a `CameraZone`, objects with `type=scroll` become a
 * `CameraScrollSection`, and objects with `type=crush_surface` go to the zone or section in their `section` reference. Other
 * objects are references for the limits. See those classes for the properties, what is automatic, and the edge tables.
 */
class CameraSectionsLayerBuilder : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "CameraSectionsLayerBuilder"
        const val ZONE = "zone"
        const val SCROLL = "scroll"
        const val CRUSH_SURFACE = "crush_surface"
        const val STOP = "stop"
        const val ARRIVE = "arrive"
        const val MIN_X = "min_x"
        const val MAX_X = "max_x"
        const val MIN_Y = "min_y"
        const val MAX_Y = ConstKeys.MAX_Y
        const val EDGE_BLOCKS = "edge_blocks"
    }

    override fun build(layer: MapLayer, returnProps: Properties) {
        val zones = Array<CameraZone>()
        val scrolls = Array<CameraScrollSection>()
        val scrollsByObject = ObjectMap<RectangleMapObject, CameraScrollSection>()
        val zonesByObject = ObjectMap<RectangleMapObject, CameraZone>()
        val surfaces = Array<RectangleMapObject>()

        layer.objects.forEach {
            if (it !is RectangleMapObject) return@forEach

            val properties = it.properties
            val type = properties.get(ConstKeys.TYPE, String::class.java)

            if (type == CRUSH_SURFACE) {
                surfaces.add(it)
                return@forEach
            }

            if (type == SCROLL) {
                val section = CameraScrollSection(
                    it.rectangle,
                    Direction.valueOf(properties.get(ConstKeys.DIRECTION, String::class.java).uppercase()),
                    properties.tiles(ConstKeys.SPEED)!!,
                    properties.get(STOP, String::class.java) == ARRIVE,
                    if (properties.containsKey(EDGE_BLOCKS)) properties.edgeBlocks() else null
                )
                scrolls.add(section)
                scrollsByObject.put(it, section)
                return@forEach
            }

            if (type != ZONE) return@forEach

            val zone = CameraZone(
                it.rectangle,
                properties.limitObject(MIN_X)?.x,
                properties.limitObject(MAX_X)?.let { limit -> limit.x + limit.width },
                properties.limitObject(MIN_Y)?.y,
                properties.limitObject(MAX_Y)?.let { limit -> limit.y + limit.height },
                properties.tiles(ConstKeys.SPEED),
                properties.edgeBlocks()
            )
            zones.add(zone)
            zonesByObject.put(it, zone)
        }

        surfaces.forEach {
            val owner = it.properties.get(ConstKeys.SECTION, RectangleMapObject::class.java)
            val ownerSurfaces = scrollsByObject[owner]?.crushSurfaces ?: zonesByObject[owner]?.crushSurfaces
                ?: throw IllegalStateException("Crush surface ${it.name} has no section")
            ownerSurfaces.add(GameRectangle(it.rectangle))
        }

        scrollsByObject.forEach { entry ->
            entry.value.next = entry.key.properties.get(ConstKeys.NEXT, RectangleMapObject::class.java)
                ?.let { scrollsByObject[it] }
        }

        GameLogger.debug(TAG, "build(): zones=${zones.size}, scrolls=${scrolls.size}")

        returnProps.put(ConstKeys.CAMERA_SECTIONS, zones)
        returnProps.put(ConstKeys.CAMERA_SCROLLS, scrolls)
    }

    private fun MapProperties.edgeBlocks(): ObjectSet<Direction> {
        val edges = ObjectSet<Direction>()
        get(EDGE_BLOCKS, String::class.java)?.split(",")?.forEach { edges.add(Direction.valueOf(it.trim().uppercase())) }
        return edges
    }

    private fun MapProperties.limitObject(key: String) = get(key, RectangleMapObject::class.java)?.rectangle

    private fun MapProperties.tiles(key: String) = (get(key) as Number?)?.let { it.toFloat() * ConstVals.PPM }
}
