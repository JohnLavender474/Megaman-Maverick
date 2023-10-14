package com.test.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.ITiledMapLayerBuilder
import com.test.game.ConstKeys
import com.test.game.screens.levels.map.MapLayerBuildersParams

class PlayerLayerBuilder(private val params: MapLayerBuildersParams) : ITiledMapLayerBuilder {

  override fun build(layer: MapLayer, returnProps: Properties) {
    val spawns = Array<RectangleMapObject>()
    layer.objects.forEach { if (it is RectangleMapObject) spawns.add(it) }
    returnProps.put("${ConstKeys.PLAYER}_${ConstKeys.SPAWNS}", spawns)
  }
}
