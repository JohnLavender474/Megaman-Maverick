package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys

class PlayerLayerBuilder : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "PlayerLayerBuilder"
    }

    override fun build(layer: MapLayer, returnProps: Properties) {
        val spawns = Array<RectangleMapObject>()
        layer.objects.forEach { if (it is RectangleMapObject) spawns.add(it) }
        returnProps.put("${ConstKeys.PLAYER}_${ConstKeys.SPAWNS}", spawns)
    }
}
