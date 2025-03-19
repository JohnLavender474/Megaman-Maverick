package com.megaman.maverick.game.screens.levels.tiled.layers

import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilder
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TEXTURE_ASSET_PREFIX
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.drawables.backgrounds.*
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toProps

class BackgroundLayerBuilder(private val params: MegaMapLayerBuildersParams) : ITiledMapLayerBuilder {

    companion object {
        const val TAG = "BackgroundLayerBuilder"
        const val ANIMATED_BACKGROUND = "AnimatedBackground"
    }

    private val presetBKGMap: ObjectMap<String, (RectangleMapObject) -> Background> = objectMapOf(
        "UndergroundPipes" pairTo { UndergroundPipes(params.game.assMan, it) },
        "DesertCanyon" pairTo { DesertCanyon(params.game.assMan, it) },
        "DesertNoSunSky" pairTo { DesertNoSunSky(params.game.assMan, it) },
        "DesertSunSky" pairTo { DesertSunSky(params.game.assMan, it) },
        "Space" pairTo { Space(params.game.assMan, it) },
        "EarthBackdrop" pairTo { EarthBackdrop(params.game.assMan, it) },
        "Moon" pairTo { Moon(params.game.assMan, it) },
        "WindyClouds" pairTo {
            WindyClouds(
                params.game,
                it.rectangle.getPosition(GameObjectPools.fetch(Vector2::class)),
                it.rectangle.width,
                it.rectangle.height
            )
        },
        "AnimatedStars" pairTo {
            AnimatedStars(
                params.game,
                it.rectangle.getPosition(GameObjectPools.fetch(Vector2::class))
            )
        },
        "ScrollingStars" pairTo {
            ScrollingStars(
                params.game,
                it.rectangle.getPosition(GameObjectPools.fetch(Vector2::class))
            )
        },
        "CrystalBKG" pairTo {
            Background(
                key = it.name,
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, "CrystalBKG"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = it.properties.get(ConstKeys.ROWS) as Int,
                columns = it.properties.get(ConstKeys.COLUMNS) as Int,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 1)
            )
        },
        "InfernoBKG" pairTo {
            AnimatedBackground(
                key = it.name,
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, "InfernoBKG"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = it.properties.get(ConstKeys.ROWS) as Int,
                columns = it.properties.get(ConstKeys.COLUMNS) as Int,
                animRows = 2,
                animColumns = 1,
                duration = 0.2f,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 1),
            )
        },
        "ForestBKG" pairTo {
            Background(
                it.name,
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
        "ForestBKG_v2" pairTo {
            Background(
                key = it.name,
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_3.source, "ForestBKG_v2"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = it.properties.get(ConstKeys.ROWS) as Int,
                columns = it.properties.get(ConstKeys.COLUMNS) as Int,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 1),
                parallaxX = 0.075f,
                parallaxY = 0f
            )
        },
        "GlacierBKG" pairTo {
            AnimatedBackground(
                it.name,
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_4.source, "GlacierBKG_v2"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = 1,
                columns = 30,
                animRows = 2,
                animColumns = 2,
                duration = 0.2f,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 1),
                initPos = Vector2(it.rectangle.getCenter().x, it.rectangle.getCenter().y - 2f * ConstVals.PPM),
                parallaxX = 0.075f,
                parallaxY = 0f
            )
        },
        "GlacierCloudsBKG" pairTo {
            Background(
                it.name,
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_5.source, "GlacierCloudsBKG"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = 1,
                columns = 30,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 0),
                initPos = Vector2(it.rectangle.getCenter().x, it.rectangle.getCenter().y),
                parallaxX = 0.05f,
                parallaxY = 0f
            )
        },
        "BKG12" pairTo {
            Background(
                it.name,
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_2.source, "BKG12"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = 10,
                columns = 50,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 0),
                initPos = Vector2(
                    it.rectangle.getCenter().x + 5f * ConstVals.PPM,
                    it.rectangle.getCenter().y + 5f * ConstVals.PPM
                ),
                parallaxX = 0.1f,
                parallaxY = 0f
            )
        },
        "SunriseHills" pairTo {
            Background(
                it.name,
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, "SunriseHills"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = 1,
                columns = 50,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 2),
                initPos = Vector2(
                    it.rectangle.getCenter().x + 5f * ConstVals.PPM,
                    it.rectangle.getCenter().y
                ),
                parallaxX = 0.1f,
                parallaxY = 0f
            )
        },
        "SunsetHills" pairTo {
            Background(
                it.name,
                startX = it.rectangle.x,
                startY = it.rectangle.y,
                model = params.game.assMan.getTextureRegion(TextureAsset.BACKGROUNDS_6.source, "SunsetHills"),
                modelWidth = it.rectangle.width,
                modelHeight = it.rectangle.height,
                rows = 1,
                columns = 50,
                priority = DrawingPriority(DrawingSection.BACKGROUND, 1),
                initPos = Vector2(
                    it.rectangle.getCenter().x + 5f * ConstVals.PPM,
                    it.rectangle.getCenter().y
                ),
                parallaxX = 0.1f,
                parallaxY = 0f
            )
        },
    )

    override fun build(layer: MapLayer, returnProps: Properties) {
        val backgrounds = Array<Background>()
        val iter = layer.objects.iterator()
        while (iter.hasNext()) {
            val o = iter.next()
            if (o !is RectangleMapObject) continue

            if (o.name != null && presetBKGMap.containsKey(o.name)) {
                GameLogger.debug(TAG, "build(): building preset background ${o.name}")
                val supplier = presetBKGMap[o.name]!!
                val background = supplier.invoke(o)
                backgrounds.add(background)
                continue
            }

            GameLogger.debug(TAG, "build(): building custom background ${o.name}")

            val props = o.properties.toProps()

            val atlasKey = TEXTURE_ASSET_PREFIX + props.get(ConstKeys.ATLAS, String::class)!!
            val regionKey = props.get(ConstKeys.REGION, String::class)!!
            val backgroundRegion = params.game.assMan.getTextureRegion(atlasKey, regionKey)

            val rows = props.get(ConstKeys.ROWS, Int::class)!!
            val columns = props.get(ConstKeys.COLUMNS, Int::class)!!

            val offsetX = props.getOrDefault(ConstKeys.OFFSET_X, 0f, Float::class) * ConstVals.PPM
            val offsetY = props.getOrDefault(ConstKeys.OFFSET_Y, 0f, Float::class) * ConstVals.PPM

            val parallaxX =
                props.getOrDefault("${ConstKeys.PARALLAX}_${ConstKeys.X}", ConstVals.DEFAULT_PARALLAX_X, Float::class)
            val parallaxY =
                props.getOrDefault("${ConstKeys.PARALLAX}_${ConstKeys.Y}", ConstVals.DEFAULT_PARALLAX_Y, Float::class)

            val rotatable = props.getOrDefault(ConstKeys.ROTATION, true, Boolean::class)

            val section = DrawingSection.valueOf(
                props.getOrDefault(
                    ConstKeys.SECTION,
                    DrawingSection.BACKGROUND.name,
                    String::class
                )
            )
            val priority = props.getOrDefault(ConstKeys.PRIORITY, 0, Int::class)

            val background = if (o.name == ANIMATED_BACKGROUND) {
                val animRows = props.get("${ConstKeys.ANIMATION}_${ConstKeys.ROWS}", Int::class)!!
                val animColumns = props.get("${ConstKeys.ANIMATION}_${ConstKeys.COLUMNS}", Int::class)!!
                val duration = props.get(ConstKeys.DURATION, Float::class)!!
                AnimatedBackground(
                    o.name,
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
                    rotatable = rotatable,
                    priority = DrawingPriority(section, priority)
                )
            } else Background(
                o.name,
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
                rotatable = rotatable,
                priority = DrawingPriority(section, priority)
            )
            backgrounds.add(background)
        }
        returnProps.put(ConstKeys.BACKGROUNDS, backgrounds)

        val backgroundsToHide = ObjectSet<String>()
        layer.properties.get("${ConstKeys.HIDDEN}_${ConstKeys.BACKGROUNDS}", String::class.java)
            ?.split(",")
            ?.forEach { backgroundsToHide.add(it) }
        returnProps.put("${ConstKeys.HIDDEN}_${ConstKeys.BACKGROUNDS}", backgroundsToHide)
        GameLogger.debug(TAG, "build(): backgroundsToHide=$backgroundsToHide")
    }
}

