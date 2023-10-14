package com.test.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.ITiledMapLayerBuilder
import com.test.game.screens.levels.map.MapLayerBuildersParams

class ForegroundLayerBuilder(private val params: MapLayerBuildersParams) : ITiledMapLayerBuilder {

  override fun build(layer: MapLayer, returnProps: Properties) {}
}
