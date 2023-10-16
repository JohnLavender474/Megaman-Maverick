package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block

/**
 * This class is responsible for building the blocks layer.
 *
 * @param params the [MegaMapLayerBuildersParams] to use to initialize this [BlocksLayerBuilder]
 *   instance.
 */
class BlocksLayerBuilder(private val params: MegaMapLayerBuildersParams) : ITiledMapLayerBuilder {

  /**
   * Builds the blocks layer.
   *
   * @param layer the [MapLayer] to build.
   * @param returnProps the [Properties] to use as a return container.
   * @see [Block]
   */
  override fun build(layer: MapLayer, returnProps: Properties) {
    layer.objects.forEach {
      if (it is RectangleMapObject) {
        val block = Block(params.game)
        returnProps.put("${ConstKeys.BLOCKS}_${it.name}", block)
      }
    }
  }
}
