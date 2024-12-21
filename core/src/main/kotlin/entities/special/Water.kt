package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IWater
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import java.util.*

class Water(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IWater {

    private class WaterSpriteDef(
        val priority: DrawingPriority,
        val alpha: Float
    )

    companion object {
        const val TAG = "Water"

        private val ATLAS_KEY = TextureAsset.SPECIALS_1.source
        private const val REGION_KEY_PREFIX = "${TAG}_v2"

        private val regions = ObjectMap<String, TextureRegion>()

        private val DEFS = gdxArrayOf(
            WaterSpriteDef(DrawingPriority(DrawingSection.FOREGROUND, 10), 0.25f),
            WaterSpriteDef(DrawingPriority(DrawingSection.FOREGROUND, 11), 0.1f)
        )
    }

    private var splashSound = true

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(ATLAS_KEY)
            gdxArrayOf("surface_foreground", "surface_background", "under").forEach {
                regions.put(it, atlas.findRegion("${REGION_KEY_PREFIX}/$it"))
            }
        }
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val hasSurface = spawnProps.getOrDefault(ConstKeys.SURFACE, true, Boolean::class)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        body.forEachFixture { fixture ->
            val shape = (fixture as Fixture).rawShape
            if (shape is GameRectangle) shape.set(bounds)
        }

        val hidden = spawnProps.getOrDefault(ConstKeys.HIDDEN, false, Boolean::class)
        if (hidden) {
            removeComponent(SpritesComponent::class)
            removeComponent(AnimationsComponent::class)
        } else defineDrawables(bounds, hasSurface)

        splashSound = spawnProps.getOrDefault(ConstKeys.SPLASH, true, Boolean::class)
    }

    override fun onDestroy() {
        super.onDestroy()
        GameLogger.debug(TAG, "onDestroy()")
    }

    override fun doMakeSplashSound() = splashSound

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val waterFixture = Fixture(body, FixtureType.WATER)
        body.addFixture(waterFixture)
        debugShapes.add { waterFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullable))
    }

    private fun defineDrawables(bounds: GameRectangle, hasSurface: Boolean) {
        val sprites = OrderedMap<Any, GameSprite>()
        val animators = OrderedMap<Any, IAnimator>()

        val rows = (bounds.getHeight() / ConstVals.PPM).toInt()
        val columns = (bounds.getWidth() / ConstVals.PPM).toInt()

        val regionEntries = Array<GamePair<TextureRegion, WaterSpriteDef>>()
        for (x in 0 until columns) for (y in 0 until rows) {
            val pos = GameObjectPools.fetch(Vector2::class)
                .set(bounds.getX() + x * ConstVals.PPM, bounds.getY() + y * ConstVals.PPM)

            val animRows: Int
            val animCols: Int
            when {
                hasSurface && y == rows - 1 -> {
                    regionEntries.addAll(
                        regions["surface_background"] pairTo DEFS[0],
                        regions["surface_foreground"] pairTo DEFS[0],
                        regions["surface_foreground"] pairTo DEFS[1]
                    )
                    animRows = 2
                    animCols = 2
                }

                else -> {
                    regionEntries.addAll(
                        regions["under"] pairTo DEFS[0],
                        regions["under"] pairTo DEFS[1]
                    )
                    animRows = 1
                    animCols = 1
                }
            }

            regionEntries.forEach { (region, def) ->
                val animation = Animation(region, animRows, animCols, 0.1f, true)
                val sprite = GameSprite(def.priority.copy())
                sprite.setBounds(pos.x, pos.y, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
                sprite.setAlpha(def.alpha)

                val key = UUID.randomUUID()
                sprites.put(key, sprite)

                val animator = Animator(animation)
                animators.put(key, animator)
            }

            regionEntries.clear()
        }

        addComponent(SpritesComponent(sprites))
        addComponent(AnimationsComponent(animators, sprites))
    }

    override fun getEntityType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
