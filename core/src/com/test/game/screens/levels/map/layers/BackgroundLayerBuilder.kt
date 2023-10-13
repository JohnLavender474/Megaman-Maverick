package com.test.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.engine.IGame2D
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.screens.levels.tiledmap.ITiledMapLayerBuilder
import com.test.game.ConstKeys
import com.test.game.assets.TEXTURE_ASSET_PREFIX
import com.test.game.drawables.sprites.Background

class BackgroundLayerBuilder(private val game: IGame2D) : ITiledMapLayerBuilder {

  override fun build(layer: MapLayer, returnProps: Properties) {
    val backgrounds = Array<Background>()

    val iter = layer.objects.iterator()
    while (iter.hasNext()) {
      val o = iter.next()

      if (o !is RectangleMapObject) {
        continue
      }

      val bkgReg =
          game.assMan.getTextureRegion(
              TEXTURE_ASSET_PREFIX + o.properties.get(ConstKeys.ATLAS) as String,
              o.properties.get(ConstKeys.REGION) as String)

      backgrounds.add(Background(bkgReg, o))
    }

    returnProps.put(ConstKeys.BACKGROUNDS, backgrounds)
  }
}
