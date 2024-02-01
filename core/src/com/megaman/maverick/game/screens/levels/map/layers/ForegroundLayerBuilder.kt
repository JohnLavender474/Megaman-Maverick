package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder

class ForegroundLayerBuilder(private val params: MegaMapLayerBuildersParams) :
    ITiledMapLayerBuilder {

    override fun build(layer: MapLayer, returnProps: Properties) {}
}
