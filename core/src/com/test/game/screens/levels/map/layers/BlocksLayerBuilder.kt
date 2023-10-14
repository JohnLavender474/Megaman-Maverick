package com.test.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.IGame2D
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.ITiledMapLayerBuilder
import com.test.game.ConstKeys
import com.test.game.entities.blocks.Block
import com.test.game.screens.levels.map.MapLayerBuildersParams

class BlocksLayerBuilder(private val params: MapLayerBuildersParams) : ITiledMapLayerBuilder {

  override fun build(layer: MapLayer, returnProps: Properties) {
    layer.objects.forEach {
      if (it is RectangleMapObject) {
        val block = Block(params.game)
        returnProps.put("${ConstKeys.BLOCKS}_${it.name}", block)
      }
    }
  }
}
