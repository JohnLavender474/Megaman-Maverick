package com.mega.game.engine.screens.levels.tiledmap.builders

import com.badlogic.gdx.maps.MapLayers
import com.mega.game.engine.common.objects.Properties

/**
 * Builds the level from the layers of a Tiled map. The implementation decides which layers to build and which
 * [ITiledMapLayerBuilder] builds each one.
 */
fun interface ITiledMapLayerBuilders {

    fun build(layers: MapLayers, returnProps: Properties)
}
