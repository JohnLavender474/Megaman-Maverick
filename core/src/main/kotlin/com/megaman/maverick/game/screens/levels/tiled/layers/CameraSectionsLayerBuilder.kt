package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapProperties
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.screens.levels.camera.CameraZone

class CameraSectionsLayerBuilder : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "CameraSectionsLayerBuilder"
        const val ZONE = "zone"
        const val MIN_X = "min_x"
        const val MAX_X = "max_x"
        const val MIN_Y = "min_y"
        const val MAX_Y = ConstKeys.MAX_Y
        const val EDGE_BLOCKS = "edge_blocks"
    }

    override fun build(layer: MapLayer, returnProps: Properties) {
        val zones = Array<CameraZone>()

        layer.objects.forEach {
            if (it !is RectangleMapObject) return@forEach
            if (it.properties.get(ConstKeys.TYPE, String::class.java) != ZONE) return@forEach

            val properties = it.properties
            zones.add(
                CameraZone(
                    it.rectangle,
                    properties.limitObject(MIN_X)?.x,
                    properties.limitObject(MAX_X)?.let { limit -> limit.x + limit.width },
                    properties.limitObject(MIN_Y)?.y,
                    properties.limitObject(MAX_Y)?.let { limit -> limit.y + limit.height },
                    properties.tiles(ConstKeys.SPEED),
                    properties.edgeBlocks()
                )
            )
        }

        GameLogger.debug(TAG, "build(): zones=${zones.size}")

        returnProps.put(ConstKeys.CAMERA_SECTIONS, zones)
    }

    private fun MapProperties.edgeBlocks(): ObjectSet<Direction> {
        val edges = ObjectSet<Direction>()
        get(EDGE_BLOCKS, String::class.java)?.split(",")?.forEach { edges.add(Direction.valueOf(it.trim().uppercase())) }
        return edges
    }

    private fun MapProperties.limitObject(key: String) = get(key, RectangleMapObject::class.java)?.rectangle

    private fun MapProperties.tiles(key: String) = (get(key) as Number?)?.let { it.toFloat() * ConstVals.PPM }
}
