package com.maverick.game.engine.screens.levels.tiledmap.builders

import com.badlogic.gdx.maps.MapLayer
import com.maverick.game.engine.common.objects.Properties

fun interface ITiledMapLayerBuilder {

    fun build(layer: MapLayer, returnProps: Properties)
}
