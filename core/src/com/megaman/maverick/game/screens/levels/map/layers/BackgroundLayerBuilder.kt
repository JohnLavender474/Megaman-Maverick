package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.TEXTURE_ASSET_PREFIX
import com.megaman.maverick.game.drawables.sprites.Background

class BackgroundLayerBuilder(private val params: MegaMapLayerBuildersParams) :
    ITiledMapLayerBuilder {

  override fun build(layer: MapLayer, returnProps: Properties) {
    val backgrounds = Array<Background>()

    val iter = layer.objects.iterator()
    while (iter.hasNext()) {
      val o = iter.next()

      if (o !is RectangleMapObject) continue

      val bkgReg =
          params.game.assMan.getTextureRegion(
              TEXTURE_ASSET_PREFIX + o.properties.get(ConstKeys.ATLAS) as String,
              o.properties.get(ConstKeys.REGION) as String)

      // TODO: backgrounds.add(Background(bkgReg, o))
    }

    returnProps.put(ConstKeys.BACKGROUNDS, backgrounds)
  }
}
