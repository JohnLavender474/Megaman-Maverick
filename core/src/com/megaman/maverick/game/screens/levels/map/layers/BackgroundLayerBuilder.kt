package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.TEXTURE_ASSET_PREFIX
import com.megaman.maverick.game.drawables.sprites.Background

class BackgroundLayerBuilder(private val params: MegaMapLayerBuildersParams) :
    ITiledMapLayerBuilder {

    companion object {
        const val TAG = "BackgroundLayerBuilder"
    }

    override fun build(layer: MapLayer, returnProps: Properties) {
        val backgrounds = Array<Background>()

        val iter = layer.objects.iterator()
        while (iter.hasNext()) {
            val o = iter.next()
            if (o !is RectangleMapObject) continue

            GameLogger.debug(TAG, "Building background: ${o.name}")

            val backgroundRegion =
                params.game.assMan.getTextureRegion(
                    TEXTURE_ASSET_PREFIX + o.properties.get(ConstKeys.ATLAS) as String,
                    o.properties.get(ConstKeys.REGION) as String
                )
            val rows = o.properties.get(ConstKeys.ROWS) as Int
            val columns = o.properties.get(ConstKeys.COLUMNS) as Int

            backgrounds.add(
                Background(
                    o.rectangle.x,
                    o.rectangle.y,
                    backgroundRegion,
                    DrawingPriority(DrawingSection.BACKGROUND, 0),
                    o.rectangle.width,
                    o.rectangle.height,
                    rows,
                    columns
                )
            )
        }

        returnProps.put(ConstKeys.BACKGROUNDS, backgrounds)
    }
}
