package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
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
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.*

class HealthBulb(game: MegamanMaverickGame) : MegaGameEntity(game), ItemEntity, ISpritesEntity, IAnimatedEntity,
    IBodyEntity, ICullableEntity, IDirectionRotatable {

    companion object {
        const val TAG = "HealthBulb"
        const val SMALL_HEALTH = 3
        const val LARGE_HEALTH = 6
        private var textureAtlas: TextureAtlas? = null
        private const val TIME_TO_BLINK = 2f
        private const val BLINK_DUR = 0.01f
        private const val CULL_DUR = 3.5f
        private const val GRAVITY = 0.25f
        private const val WATER_GRAVITY = 0.1f
        private const val VEL_CLAMP = 5f
        private const val WATER_VEL_CLAMP = 1.5f
    }

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private val blinkTimer = Timer(BLINK_DUR)
    private val cullTimer = Timer(CULL_DUR)
    private lateinit var itemFixture: Fixture
    private lateinit var waterListenerFixture: Fixture
    private lateinit var feetFixture: Fixture
    private var large = false
    private var timeCull = false
    private var blink = false
    private var warning = false
    private var gravity = GRAVITY
    private var velClamp = VEL_CLAMP

    override fun getEntityType() = EntityType.ITEM

    override fun init() {
        if (textureAtlas == null) textureAtlas = game.assMan.getTextureAtlas(TextureAsset.ITEMS_1.source)
        cullTimer.setRunnables(gdxArrayOf(TimeMarkedRunnable(TIME_TO_BLINK) { warning = true }))
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn =
            if (spawnProps.containsKey(ConstKeys.BOUNDS))
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
            else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        large = spawnProps.getOrDefault(ConstKeys.LARGE, true, Boolean::class)
        timeCull = spawnProps.getOrDefault(ConstKeys.TIMED, true, Boolean::class)

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullOutOfBounds) putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(this, 0.25f))
        else removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

        body.setSize((if (large) 0.5f else 0.25f) * ConstVals.PPM)
        body.setCenter(spawn)

        (itemFixture.rawShape as GameRectangle).set(body)
        (waterListenerFixture.rawShape as GameRectangle).set(body)
        feetFixture.offsetFromBodyCenter.y = (if (large) -0.25f else -0.125f) * ConstVals.PPM

        warning = false
        blink = false
        blinkTimer.setToEnd()
        cullTimer.reset()

        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, GRAVITY, Float::class)
        velClamp = spawnProps.getOrDefault(ConstKeys.CLAMP, VEL_CLAMP, Float::class)
    }

    override fun contactWithPlayer(megaman: Megaman) {
        destroy()
        game.eventsMan.submitEvent(
            Event(
                EventType.ADD_PLAYER_HEALTH, props(ConstKeys.VALUE to (if (large) LARGE_HEALTH else SMALL_HEALTH))
            )
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        itemFixture = Fixture(body, FixtureType.ITEM, GameRectangle())
        body.addFixture(itemFixture)
        itemFixture.rawShape.color = Color.PURPLE
        debugShapes.add { itemFixture.getShape() }

        feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }
        
        waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle())
        waterListenerFixture.setHitWaterByReceiver { 
            body.physics.velocity.setZero()
            gravity = WATER_GRAVITY
            velClamp = WATER_VEL_CLAMP
            Splash.generate(body, it.body)
        }
        body.addFixture(waterListenerFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            body.physics.velocityClamp.set(velClamp * ConstVals.PPM)

            if (body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_SAND)) {
                body.physics.gravityOn = false
                body.physics.velocity.setZero()
            } else {
                body.physics.gravityOn = true
                body.physics.gravity = when (directionRotation!!) {
                    Direction.LEFT -> Vector2(gravity, 0f)
                    Direction.RIGHT -> Vector2(-gravity, 0f)
                    Direction.UP -> Vector2(0f, -gravity)
                    Direction.DOWN -> Vector2(0f, gravity)
                }.scl(ConstVals.PPM.toFloat())
            }

            feetFixture.putProperty(ConstKeys.STICK_TO_BLOCK, !body.isSensing(BodySense.FEET_ON_SAND))
        })

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(0.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = blink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (large) "large" else "small" }
        val animations = objectMapOf<String, IAnimation>(
            "large" to Animation(textureAtlas!!.findRegion("HealthBulb"), 1, 2, 0.15f, true),
            "small" to Animation(textureAtlas!!.findRegion("SmallHealthBulb"))
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun defineCullablesComponent(): CullablesComponent {
        val eventsToCullOn = objectSetOf<Any>(EventType.GAME_OVER)
        val cullOnEvent = CullableOnEvent({ eventsToCullOn.contains(it.key) }, eventsToCullOn)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS to cullOnEvent))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!timeCull) return@UpdatablesComponent

        if (warning) {
            blinkTimer.update(delta)
            if (blinkTimer.isFinished()) {
                blinkTimer.reset()
                blink = !blink
            }
        }

        cullTimer.update(delta)
        if (cullTimer.isFinished()) destroy()
    })
}
