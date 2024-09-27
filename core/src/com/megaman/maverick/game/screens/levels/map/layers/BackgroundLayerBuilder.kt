package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.getPosition
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TEXTURE_ASSET_PREFIX
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.sprites.*
import com.megaman.maverick.game.utils.toProps

class BackgroundLayerBuilder(private val params: MegaMapLayerBuildersParams) : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "BackgroundLayerBuilder"
        const val ANIMATED_BACKGROUND = "AnimatedBackground"
    }

    private val presetBGMap: ObjectMap<String, (RectangleMapObject) -> Background> = objectMapOf(
        "WindyClouds" pairTo {
            WindyClouds(
                params.game,
                it.rectangle.getPosition(),
                it.rectangle.width,
                it.rectangle.height
            )
        },
        "AnimatedStars" pairTo {
            AnimatedStars(
                params.game,
                it.rectangle.getPosition()
            )
        },
        "ScrollingStars" pairTo {
            ScrollingStars(
                params.game,
                it.rectangle.getPosition()
            )
        },
        "ForestBKG" pairTo {
            Background(
                it.rectangle.x,
                it.rectangle.y,
                params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_3.source, "ForestBKG"),
                it.rectangle.width,
                it.rectangle.height,
                it.properties.get(ConstKeys.ROWS) as Int,
                it.properties.get(ConstKeys.COLUMNS) as Int,
                DrawingPriority(DrawingSection.BACKGROUND, 1)
            )
        }
    )

    override fun build(layer: MapLayer, returnProps: Properties) {
        val backgrounds = Array<Background>()

        val iter = layer.objects.iterator()
        while (iter.hasNext()) {
            val o = iter.next()
            if (o !is RectangleMapObject) continue

            GameLogger.debug(TAG, "Building background ${o.name} with props ${o.properties.toProps()}")

            if (o.name != null && presetBGMap.containsKey(o.name)) {
                GameLogger.debug(TAG, "Building preset background ${o.name}")
                val supplier = presetBGMap[o.name]!!
                val background = supplier.invoke(o)
                backgrounds.add(background)
                continue
            }

            GameLogger.debug(TAG, "Building custom background ${o.name}")

            val props = o.properties.toProps()
            val backgroundRegion =
                params.game.assMan.getTextureRegion(
                    TEXTURE_ASSET_PREFIX + props.get(ConstKeys.ATLAS, String::class)!!,
                    props.get(ConstKeys.REGION, String::class)!!
                )
            val rows = props.get(ConstKeys.ROWS, Int::class)!!
            val columns = props.get(ConstKeys.COLUMNS, Int::class)!!
            val offsetX = props.getOrDefault(ConstKeys.OFFSET_X, 0f, Float::class) * ConstVals.PPM
            val offsetY = props.getOrDefault(ConstKeys.OFFSET_Y, 0f, Float::class) * ConstVals.PPM

            val background = if (o.name == ANIMATED_BACKGROUND) {
                val animRows = props.get("${ConstKeys.ANIMATION}_${ConstKeys.ROWS}", Int::class)!!
                val animColumns = props.get("${ConstKeys.ANIMATION}_${ConstKeys.COLUMNS}", Int::class)!!
                val duration = props.get(ConstKeys.DURATION, Float::class)!!
                AnimatedBackground(
                    o.rectangle.x + offsetX,
                    o.rectangle.y + offsetY,
                    backgroundRegion,
                    o.rectangle.width,
                    o.rectangle.height,
                    rows,
                    columns,
                    animRows,
                    animColumns,
                    duration
                )
            } else Background(
                o.rectangle.x + offsetX,
                o.rectangle.y + offsetY,
                backgroundRegion,
                o.rectangle.width,
                o.rectangle.height,
                rows,
                columns
            )
            backgrounds.add(background)
        }

        returnProps.put(ConstKeys.BACKGROUNDS, backgrounds)
    }
}

