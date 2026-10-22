package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayer
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder

class ForegroundLayerBuilder(private val params: MegaMapLayerBuildersParams) :
    ITiledMapLayerBuilder {

    override fun build(layer: MapLayer, returnProps: Properties) {}
}
