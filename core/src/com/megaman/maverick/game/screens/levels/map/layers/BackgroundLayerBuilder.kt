package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.getPosition
import com.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.TEXTURE_ASSET_PREFIX
import com.megaman.maverick.game.drawables.sprites.Background
import com.megaman.maverick.game.drawables.sprites.Stars
import com.megaman.maverick.game.drawables.sprites.WindyClouds
import com.megaman.maverick.game.utils.toProps

class BackgroundLayerBuilder(private val params: MegaMapLayerBuildersParams) : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "BackgroundLayerBuilder"
        private val PRESET_BGS = objectSetOf("WindyClouds", "Stars")
    }

    override fun build(layer: MapLayer, returnProps: Properties) {
        val backgrounds = Array<Background>()

        val iter = layer.objects.iterator()
        while (iter.hasNext()) {
            val o = iter.next()
            if (o !is RectangleMapObject) continue

            GameLogger.debug(TAG, "Building background ${o.name} with props ${o.properties.toProps()}")

            GameLogger.debug(
                TAG,
                "Checking if background is a preset contained in the set: $PRESET_BGS. Contained: ${
                    PRESET_BGS.contains(o.name)
                }"
            )
            if (PRESET_BGS.contains(o.name)) {
                GameLogger.debug(TAG, "Building preset background ${o.name}")
                backgrounds.add(getPresetBackground(o.name, o))
                continue
            }

            GameLogger.debug(TAG, "Building custom background ${o.name}")

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
                    o.rectangle.width,
                    o.rectangle.height,
                    rows,
                    columns
                )
            )
        }

        returnProps.put(ConstKeys.BACKGROUNDS, backgrounds)
    }

    private fun getPresetBackground(name: String, o: RectangleMapObject): Background {
        val bounds = o.rectangle
        return when (name) {
            "WindyClouds" -> WindyClouds(params.game, bounds.getPosition(), bounds.width, bounds.height)
            "Stars" -> Stars(params.game, bounds.getPosition())
            else -> throw IllegalArgumentException("Invalid background name: $name")
        }
    }
}

