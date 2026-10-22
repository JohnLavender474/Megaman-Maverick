package com.maverick.game.engine.screens.levels.tiledmap.builders

import com.badlogic.gdx.maps.MapLayers
import com.maverick.game.engine.common.objects.Properties

fun interface ITiledMapLayerBuilders {

    fun build(layers: MapLayers, returnProps: Properties)
}
