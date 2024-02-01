package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys

/** This class is responsible for building the player spawns layer. */
class PlayerLayerBuilder : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "PlayerLayerBuilder"
    }

    /**
     * Builds the player spawns layer.
     *
     * @param layer the [MapLayer] to build.
     * @param returnProps the [Properties] to use as a return container.
     */
    override fun build(layer: MapLayer, returnProps: Properties) {
        val spawns = Array<RectangleMapObject>()
        layer.objects.forEach { if (it is RectangleMapObject) spawns.add(it) }

        val printedSpawns = spawns.map { "[${it.name}, ${it.rectangle}]" }
        println("build(): Player spawns: $printedSpawns")

        returnProps.put("${ConstKeys.PLAYER}_${ConstKeys.SPAWNS}", spawns)
    }
}
