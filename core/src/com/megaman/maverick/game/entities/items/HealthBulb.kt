package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.enums.Position
import com.engine.common.extensions.*
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
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
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
import com.megaman.maverick.game.entities.contracts.IUpsideDownable
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing

class HealthBulb(game: MegamanMaverickGame) :
    GameEntity(game), ItemEntity, ISpriteEntity, IBodyEntity, IUpsideDownable {

    companion object {
        const val TAG = "HealthBulb"

        const val SMALL_HEALTH = 3
        const val LARGE_HEALTH = 6

        private var textureAtlas: TextureAtlas? = null

        private const val TIME_TO_BLINK = 2f
        private const val BLINK_DUR = .01f
        private const val CULL_DUR = 3.5f

        private const val GRAVITY = .25f
    }

    override var upsideDown = false

    private val blinkTimer = Timer(BLINK_DUR)
    private val cullTimer = Timer(CULL_DUR)

    private lateinit var itemFixture: Fixture
    private lateinit var feetFixture: Fixture

    private var large = false
    private var timeCull = false
    private var blink = false
    private var warning = false
    private var landed = false

    override fun init() {
        if (textureAtlas == null)
            textureAtlas = game.assMan.getTextureAtlas(TextureAsset.ITEMS_1.source)

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
            if (spawnProps.containsKey(ConstKeys.BOUNDS))
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
            else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!

        large = spawnProps.getOrDefault(ConstKeys.LARGE, true) as Boolean

        timeCull = !spawnProps.containsKey(ConstKeys.TIMED) || spawnProps.get(ConstKeys.TIMED, Boolean::class)!!

        warning = false
        blink = false
        landed = false

        blinkTimer.setToEnd()
        cullTimer.reset()

        body.setSize((if (large) 0.5f else 0.25f) * ConstVals.PPM)
        body.setCenter(spawn)
        (itemFixture.rawShape as GameRectangle).set(body)
        feetFixture.offsetFromBodyCenter.y = (if (large) -0.25f else -0.125f) * ConstVals.PPM
    }

    override fun contactWithPlayer(megaman: Megaman) {
        GameLogger.debug(TAG, "contactWithPlayer")
        kill(props(CAUSE_OF_DEATH_MESSAGE to "Megaman touched the health bulb!"))
        game.eventsMan.submitEvent(
            Event(
                EventType.ADD_PLAYER_HEALTH,
                props(ConstKeys.VALUE to (if (large) LARGE_HEALTH else SMALL_HEALTH))
            )
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val shapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        itemFixture = Fixture(body, FixtureType.ITEM, GameRectangle())
        body.addFixture(itemFixture)

        itemFixture.rawShape.color = Color.PURPLE
        shapes.add { itemFixture.getShape() }

        feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)

        feetFixture.rawShape.color = Color.GREEN
        shapes.add { feetFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            if (!landed) {
                landed = body.isSensing(BodySense.FEET_ON_GROUND)
                GameLogger.debug(TAG, "preProcess(): landed = $landed")
            }

            val gameCamera = getMegamanMaverickGame().getGameCamera()
            body.physics.gravityOn = gameCamera.overlaps(body) && !landed

            if (!body.physics.gravityOn) body.physics.velocity.y = 0f

            body.physics.gravity.y = (if (upsideDown) GRAVITY else -GRAVITY) * ConstVals.PPM
        })

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)

        val SpritesComponent = SpritesComponent(this, "healthBulb" to sprite)
        SpritesComponent.putUpdateFunction("healthBulb") { _, _sprite ->
            _sprite as GameSprite

            val position = if (upsideDown) Position.TOP_CENTER else Position.BOTTOM_CENTER
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)

            _sprite.hidden = blink
        }

        return SpritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (large) "large" else "small" }
        val animations =
            objectMapOf<String, IAnimation>(
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

    private fun defineUpdatablesComponent() =
        UpdatablesComponent(
            this,
            {
                if (!timeCull) return@UpdatablesComponent

                if (warning) {
                    blinkTimer.update(it)
                    if (blinkTimer.isJustFinished()) {
                        blinkTimer.reset()
                        blink = !blink
                    }
                }

                cullTimer.update(it)
                if (cullTimer.isFinished()) kill(props(CAUSE_OF_DEATH_MESSAGE to "Time's up!"))
            })
}
