package com.mega.game.engine.screens.levels.tiledmap.builders

import com.badlogic.gdx.maps.MapLayers
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.objects.Properties

abstract class TiledMapLayerBuilders {

    companion object {
        const val TAG = "TiledMapLayerBuilders"
    }

    abstract val layerBuilders: OrderedMap<String, ITiledMapLayerBuilder>

    open fun build(layers: MapLayers, returnProps: Properties) {
        val layersMap = layers.associateBy { it.name }
        layerBuilders.forEach {
            val layer = layersMap[it.key]
            if (layer != null) it.value.build(layer, returnProps)
        }
    }
}
