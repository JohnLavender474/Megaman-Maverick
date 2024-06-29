package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.events.Event
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing

class HealthBulb(game: MegamanMaverickGame) : GameEntity(game), ItemEntity, ISpritesEntity, IBodyEntity,
    IDirectionRotatable {

    companion object {
        const val TAG = "HealthBulb"
        const val SMALL_HEALTH = 3
        const val LARGE_HEALTH = 6
        private var textureAtlas: TextureAtlas? = null
        private const val TIME_TO_BLINK = 2f
        private const val BLINK_DUR = 0.01f
        private const val CULL_DUR = 3.5f
        private const val GRAVITY = 0.25f
    }

    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private val blinkTimer = Timer(BLINK_DUR)
    private val cullTimer = Timer(CULL_DUR)
    private lateinit var itemFixture: Fixture
    private lateinit var feetFixture: Fixture
    private var large = false
    private var timeCull = false
    private var blink = false
    private var warning = false

    override fun init() {
        if (textureAtlas == null) textureAtlas = game.assMan.getTextureAtlas(TextureAsset.ITEMS_1.source)
        cullTimer.setRunnables(
            gdxArrayOf(
                TimeMarkedRunnable(TIME_TO_BLINK) { warning = true },
            )
        )
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn =
            if (spawnProps.containsKey(ConstKeys.BOUNDS)) spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
                .getCenter()
            else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        large = spawnProps.getOrDefault(ConstKeys.LARGE, true) as Boolean
        timeCull = !spawnProps.containsKey(ConstKeys.TIMED) || spawnProps.get(ConstKeys.TIMED, Boolean::class)!!
        warning = false
        blink = false
        blinkTimer.setToEnd()
        cullTimer.reset()
        body.setSize((if (large) 0.5f else 0.25f) * ConstVals.PPM)
        body.setCenter(spawn)
        (itemFixture.rawShape as GameRectangle).set(body)
        feetFixture.offsetFromBodyCenter.y = (if (large) -0.25f else -0.125f) * ConstVals.PPM
        directionRotation = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
    }

    override fun contactWithPlayer(megaman: Megaman) {
        kill()
        game.eventsMan.submitEvent(
            Event(
                EventType.ADD_PLAYER_HEALTH, props(ConstKeys.VALUE to (if (large) LARGE_HEALTH else SMALL_HEALTH))
            )
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)
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

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            val gravity = when (directionRotation) {
                Direction.LEFT -> Vector2(GRAVITY, 0f)
                Direction.RIGHT -> Vector2(-GRAVITY, 0f)
                Direction.UP -> Vector2(0f, -GRAVITY)
                Direction.DOWN -> Vector2(0f, GRAVITY)
            }
            body.physics.gravity = gravity.scl(ConstVals.PPM.toFloat())
        })

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
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
        return CullablesComponent(this, objectMapOf(ConstKeys.CULL_EVENTS to cullOnEvent))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        if (body.isSensing(BodySense.FEET_ON_GROUND)) {
            body.physics.gravityOn = false
            body.physics.velocity.setZero()
        } else body.physics.gravityOn = true

        if (!timeCull) return@UpdatablesComponent

        if (warning) {
            blinkTimer.update(it)
            if (blinkTimer.isJustFinished()) {
                blinkTimer.reset()
                blink = !blink
            }
        }

        cullTimer.update(it)
        if (cullTimer.isFinished()) kill()
    })
}
