package com.mega.game.engine.screens.levels.tiledmap.builders

import com.badlogic.gdx.maps.MapLayer
import com.mega.game.engine.common.objects.Properties

fun interface ITiledMapLayerBuilder {

    fun build(layer: MapLayer, returnProps: Properties)
}
