package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

abstract class AbstractEnergyItem(game: MegamanMaverickGame) : MegaGameEntity(game), ItemEntity, ISpritesEntity,
    IAnimatedEntity, IBodyEntity, ICullableEntity, IDirectional, IScalableGravityEntity {

    companion object {
        const val TAG = "AbstractItem"

        const val SMALL_AMOUNT = 3
        const val LARGE_AMOUNT = 6

        private const val SMALL_SIZE = 0.5f
        private const val LARGE_SIZE = 1f

        private const val TIME_TO_BLINK = 2f
        private const val BLINK_DUR = 0.01f
        private const val CULL_DUR = 3.5f

        private const val GRAVITY = 0.25f
        private const val WATER_GRAVITY = 0.1f

        private const val VEL_CLAMP = 5f
        private const val WATER_VEL_CLAMP = 1.5f

        private val animDefs = orderedMapOf(
            "large" pairTo AnimationDef(2, 1, 0.15f, true),
            "small" pairTo AnimationDef(2, 1, 0.15f, true)
        )
        private val regions = ObjectMap<String, ObjectMap<String, TextureRegion>>()
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var gravityScalar = 1f

    protected var large = false

    private val blinkTimer = Timer(BLINK_DUR)
    private val cullTimer = Timer(CULL_DUR)

    private var blink = false
    private var warning = false
    private var timeCull = false

    private var gravity = GRAVITY
    private var velClamp = VEL_CLAMP

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (!regions.containsKey(getTag())) {
            val map = ObjectMap<String, TextureRegion>()

            val atlas = game.assMan.getTextureAtlas(TextureAsset.ITEMS_1.source)
            animDefs.keys().forEach { key -> map.put(key, atlas.findRegion("${getTag()}/$key")) }

            regions.put(getTag(), map)
        }

        super.init()

        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())

        cullTimer.addRunnables(TimeMarkedRunnable(TIME_TO_BLINK) { warning = true })
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()

            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }

        large = spawnProps.getOrDefault(ConstKeys.LARGE, true, Boolean::class)
        timeCull = spawnProps.getOrDefault(ConstKeys.TIMED, true, Boolean::class)

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        when {
            cullOutOfBounds -> putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(this, 0.25f))
            else -> removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)
        }

        body.setSize((if (large) LARGE_SIZE else SMALL_SIZE) * ConstVals.PPM)
        body.setCenter(spawn)

        warning = false
        blink = false

        blinkTimer.setToEnd()
        cullTimer.reset()

        direction = megaman.direction
        gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, GRAVITY, Float::class)
        velClamp = spawnProps.getOrDefault(ConstKeys.CLAMP, VEL_CLAMP, Float::class)

        gravityScalar = 1f
    }

    override fun contactWithPlayer(megaman: Megaman) {
        destroy()

        game.eventsMan.submitEvent(
            Event(
                EventType.ADD_PLAYER_HEALTH, props(ConstKeys.VALUE pairTo if (large) LARGE_AMOUNT else SMALL_AMOUNT)
            )
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val itemFixture = Fixture(body, FixtureType.ITEM, GameRectangle())
        body.addFixture(itemFixture)
        itemFixture.drawingColor = Color.PURPLE
        debugShapes.add { itemFixture }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle())
        waterListenerFixture.setHitByWaterReceiver {
            body.physics.velocity.setZero()
            gravity = WATER_GRAVITY
            velClamp = WATER_VEL_CLAMP
            Splash.splashOnWaterSurface(body.getBounds(), it.body.getBounds(), true)
        }
        body.addFixture(waterListenerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocityClamp.set(velClamp * ConstVals.PPM)

            if (body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_SAND)) {
                body.physics.gravityOn = false
                body.physics.velocity.setZero()
            } else {
                body.physics.gravityOn = true

                val gravityVec = GameObjectPools.fetch(Vector2::class)
                when (direction) {
                    Direction.LEFT -> gravityVec.set(gravity, 0f)
                    Direction.RIGHT -> gravityVec.set(-gravity, 0f)
                    Direction.UP -> gravityVec.set(0f, -gravity)
                    Direction.DOWN -> gravityVec.set(0f, gravity)
                }.scl(gravityScalar * ConstVals.PPM.toFloat())
                body.physics.gravity.set(gravityVec)
            }

            (itemFixture.rawShape as GameRectangle).set(body)
            (waterListenerFixture.rawShape as GameRectangle).set(body)

            feetFixture.offsetFromBodyAttachment.y = (-body.getHeight() / 2f) + 0.1f * ConstVals.PPM
            feetFixture.putProperty(ConstKeys.STICK_TO_BLOCK, !body.isSensing(BodySense.FEET_ON_SAND))
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.rotation = direction.rotation

            val position = DirectionPositionMapper.getInvertedPosition(direction)
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.hidden = blink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { "${getTag()}/${if (large) "large" else "small"}" }

        val animations = ObjectMap<String, IAnimation>()
        animDefs.forEach { entry ->
            val key = entry.key
            val fullKey = "${getTag()}/$key"

            try {
                val region = regions[getTag()][key]
                val (rows, columns, durations, loop) = entry.value

                animations.put(fullKey, Animation(region, rows, columns, durations, loop))
            } catch (e: Exception) {
                throw Exception(
                    "Failed to create animation for key=$key, tag=${getTag()}, fullKey=$fullKey, regions=$regions",
                    e
                )
            }
        }

        val animator = Animator(keySupplier, animations)

        return AnimationsComponent(this, animator)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val eventsToCullOn = objectSetOf<Any>(EventType.GAME_OVER)
        val cullOnEvent = CullableOnEvent({ eventsToCullOn.contains(it.key) }, eventsToCullOn)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS pairTo cullOnEvent))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        direction = megaman.direction

        if (timeCull) {
            if (warning) {
                blinkTimer.update(delta)
                if (blinkTimer.isFinished()) {
                    blinkTimer.reset()
                    blink = !blink
                }
            }

            cullTimer.update(delta)
            if (cullTimer.isFinished()) destroy()
        }
    })

    override fun getType() = EntityType.ITEM
}
