package com.megaman.maverick.game.screens.levels.map.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.getCenter
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
import java.util.*

class BackgroundLayerBuilder(private val params: MegaMapLayerBuildersParams) : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "BackgroundLayerBuilder"
        const val ANIMATED_BACKGROUND = "AnimatedBackground"
    }

    private val presetBKGMap: ObjectMap<String, (RectangleMapObject) -> Background> = objectMapOf(
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
        },
        "GlacierBKG" pairTo {
            AnimatedBackground(
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_4.source, "GlacierBKG_v2"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = 1,
                columns = 30,
                animRows = 2,
                animColumns = 2,
                duration = 0.25f,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 1),
                initPos = Vector2(it.rectangle.getCenter().x, it.rectangle.getCenter().y - 2f * ConstVals.PPM),
                parallaxX = 0.1f,
                parallaxY = 0f
            )
        },
        "GlacierCloudsBKG" pairTo {
            Background(
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_5.source, "GlacierCloudsBKG"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = 1,
                columns = 30,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 0),
                initPos = Vector2(it.rectangle.getCenter().x, it.rectangle.getCenter().y),
                parallaxX = 0.075f,
                parallaxY = 0f
            )
        }
    )

    override fun build(layer: MapLayer, returnProps: Properties) {
        val backgrounds = PriorityQueue<Background>()

        val iter = layer.objects.iterator()
        while (iter.hasNext()) {
            val o = iter.next()
            if (o !is RectangleMapObject) continue

            GameLogger.debug(TAG, "Building background ${o.name} with props ${o.properties.toProps()}")

            if (o.name != null && presetBKGMap.containsKey(o.name)) {
                GameLogger.debug(TAG, "Building preset background ${o.name}")
                val supplier = presetBKGMap[o.name]!!
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
            val parallaxX =
                props.getOrDefault("${ConstKeys.PARALLAX}_${ConstKeys.X}", ConstVals.DEFAULT_PARALLAX_X, Float::class)
            val parallaxY =
                props.getOrDefault("${ConstKeys.PARALLAX}_${ConstKeys.Y}", ConstVals.DEFAULT_PARALLAX_Y, Float::class)
            val rotatable = props.getOrDefault(ConstKeys.ROTATION, true, Boolean::class)

            val background = if (o.name == ANIMATED_BACKGROUND) {
                val animRows = props.get("${ConstKeys.ANIMATION}_${ConstKeys.ROWS}", Int::class)!!
                val animColumns = props.get("${ConstKeys.ANIMATION}_${ConstKeys.COLUMNS}", Int::class)!!
                val duration = props.get(ConstKeys.DURATION, Float::class)!!
                AnimatedBackground(
                    o.rectangle.x,
                    o.rectangle.y,
                    backgroundRegion,
                    o.rectangle.width,
                    o.rectangle.height,
                    rows,
                    columns,
                    animRows,
                    animColumns,
                    duration,
                    initPos = Vector2(o.rectangle.getCenter().x + offsetX, o.rectangle.getCenter().y + offsetY),
                    parallaxX = parallaxX,
                    parallaxY = parallaxY,
                    rotatable = rotatable
                )
            } else Background(
                o.rectangle.x,
                o.rectangle.y,
                backgroundRegion,
                o.rectangle.width,
                o.rectangle.height,
                rows,
                columns,
                initPos = Vector2(o.rectangle.getCenter().x + offsetX, o.rectangle.getCenter().y + offsetY),
                parallaxX = parallaxX,
                parallaxY = parallaxY,
                rotatable = rotatable
            )
            backgrounds.add(background)
        }

        returnProps.put(ConstKeys.BACKGROUNDS, backgrounds)
    }
}

