package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.ShapeUtils
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.WoodCrate
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.IWater
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import java.util.*

class Water(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, ICullableEntity, IWater,
    Updatable {

    private data class WaterSpriteDef(
        val animDef: AnimationDef,
        val priority: DrawingPriority,
        val alpha: Float
    )

    companion object {
        const val TAG = "Water"

        private val ATLAS_KEY = TextureAsset.SPECIALS_1.source
        private const val REGION_KEY_PREFIX = "${TAG}_v2"

        private val regions = ObjectMap<String, TextureRegion>()

        private val SPRITE_DEFS = objectMapOf(
            "surface_waves_outline" pairTo WaterSpriteDef(
                AnimationDef(2, 2, 0.1f), DrawingPriority(DrawingSection.FOREGROUND, 20), 1f
            ),
            "surface_background" pairTo WaterSpriteDef(
                AnimationDef(2, 2, 0.1f), DrawingPriority(DrawingSection.PLAYGROUND, -10), 1f
            ),
            "surface_foreground" pairTo WaterSpriteDef(
                AnimationDef(2, 2, 0.1f), DrawingPriority(DrawingSection.FOREGROUND, 10), 0.2f
            ),
            "under" pairTo WaterSpriteDef(
                AnimationDef(), DrawingPriority(DrawingSection.FOREGROUND, 10), 0.2f
            )
        )
    }

    private val animations = OrderedMap<String, IAnimation>()
    // I f***ed up when designing the AnimationSystem. I designed it so that the
    // sprite-to-animation relationship is always 1:1, but in the case of this
    // entity, it would be nice if the system supposed a many-to-one relationship
    // (multiple sprites sharing the same animation). Anyway, this is a hacky
    // workaround to solve the issue.
    private val spriteToAnimMap = OrderedMap<GameSprite, String>()

    private lateinit var splashType: SplashType
    private var splashSound = true

    private val fullBounds = GameRectangle()
    private val tempRect = GameRectangle()

    private var hidden = false

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(ATLAS_KEY)
            SPRITE_DEFS.keys().forEach { key -> regions.put(key, atlas.findRegion("${REGION_KEY_PREFIX}/$key")) }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(UpdatablesComponent(this::update))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        fullBounds.set(bounds)
        body.set(fullBounds)
        body.forEachFixture { fixture ->
            val shape = (fixture as Fixture).rawShape
            if (shape is GameRectangle) shape.set(fullBounds)
        }

        hidden = spawnProps.getOrDefault(ConstKeys.HIDDEN, false, Boolean::class)
        if (hidden) {
            removeComponent(SpritesComponent::class)
            removeComponent(AnimationsComponent::class)
        } else {
            val hasSurface = spawnProps.getOrDefault(ConstKeys.SURFACE, true, Boolean::class)
            defineDrawables(bounds, hasSurface)
        }

        splashSound = spawnProps.getOrDefault(ConstKeys.SPLASH, true, Boolean::class)

        val splashType = spawnProps.getOrDefault("${ConstKeys.SPLASH}_${ConstKeys.TYPE}", SplashType.BLUE)
        this.splashType = when (splashType) {
            is String -> SplashType.valueOf(splashType.uppercase())
            is SplashType -> splashType
            else -> SplashType.BLUE
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        animations.clear()
        spriteToAnimMap.clear()
    }

    override fun shouldSplash(fixture: IFixture): Boolean {
        val entity = fixture.getEntity()
        return entity.getType() != EntityType.PROJECTILE || (entity as IProjectileEntity).owner != megaman
    }

    override fun doMakeSplashSound(fixture: IFixture) = splashSound && fixture.getEntity() !is WoodCrate

    override fun getSplashType(fixture: IFixture) = SplashType.BLUE

    override fun update(delta: Float) {
        if (hidden) return

        animations.values().forEach { animation -> animation.update(delta) }
        spriteToAnimMap.forEach { entry ->
            val sprite = entry.key
            val key = entry.value
            val animation = animations[key]
            val region = animation.getCurrentRegion()
            sprite.setRegion(region)
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val waterFixture = Fixture(body, FixtureType.WATER)
        body.addFixture(waterFixture)
        debugShapes.add { waterFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            val overlap = ShapeUtils.intersectRectangles(
                fullBounds,
                game.getGameCamera().getRotatedBounds(),
                tempRect
            )
            if (overlap) {
                body.set(tempRect)
                body.forEachFixture { fixture ->
                    val shape = (fixture as Fixture).rawShape
                    if (shape is GameRectangle) shape.set(tempRect)
                }
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val cullable = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullable))
    }

    private fun defineDrawables(bounds: GameRectangle, hasSurface: Boolean) {
        val sprites = OrderedMap<Any, GameSprite>()

        SPRITE_DEFS.forEach { def ->
            val key = def.key
            val (animDef, _, _) = def.value
            val animation = Animation(regions[key]!!, animDef.rows, animDef.cols, animDef.durations, animDef.loop)
            animations.put(key, animation)
        }

        val rows = (bounds.getHeight() / ConstVals.PPM).toInt()
        val columns = (bounds.getWidth() / ConstVals.PPM).toInt()

        val keys = Array<String>()
        for (x in 0 until columns) for (y in 0 until rows) {
            val pos = GameObjectPools.fetch(Vector2::class)
                .set(bounds.getX() + x * ConstVals.PPM, bounds.getY() + y * ConstVals.PPM)

            when {
                hasSurface && y == rows - 1 -> keys.addAll(
                    "surface_background",
                    "surface_foreground",
                    "surface_waves_outline"
                )

                else -> keys.addAll("under")
            }

            keys.forEach { key ->
                val (_, priority, alpha) = SPRITE_DEFS[key]

                val sprite = GameSprite(priority.copy())
                sprite.setBounds(pos.x, pos.y, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
                sprite.setAlpha(alpha)

                val id = UUID.randomUUID().toString()
                sprites.put(id, sprite)

                spriteToAnimMap.put(sprite, key)
            }

            keys.clear()
        }

        addComponent(SpritesComponent(sprites))
    }

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
