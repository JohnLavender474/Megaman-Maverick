package com.megaman.maverick.game.entities.sensors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class Gate(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IAudioEntity, ISpritesEntity, IEventListener,
    Resettable {

    companion object {
        const val TAG = "Gate"
        private var atlas: TextureAtlas? = null
        private const val DURATION = 0.5f
    }

    enum class GateState { OPENABLE, OPENING, OPEN, CLOSING, CLOSED }

    override val eventKeyMask =
        objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.END_ROOM_TRANS, EventType.MINI_BOSS_DEAD)

    val center = Vector2()

    lateinit var gateState: GateState
        private set
    lateinit var direction: Direction
        private set

    private val timer = Timer(DURATION)

    private lateinit var nextRoomKey: String

    private var triggerable = true
    private var resettable = false
    private var transitionFinished = false
    private var showCloseEvent = true
    private var miniBossGate = false
    private lateinit var thisBossKey: String

    override fun getEntityType() = EntityType.SENSOR

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.GATES.source)
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        game.eventsMan.addListener(this)

        center.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter())

        nextRoomKey = spawnProps.get(ConstKeys.ROOM, String::class)!!
        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, "left", String::class)
        direction = Direction.valueOf(directionString.uppercase())
        miniBossGate = spawnProps.getOrDefault("${ConstKeys.MINI}_${ConstKeys.BOSS}", false, Boolean::class)
        thisBossKey = if (miniBossGate)
            spawnProps.get("${ConstKeys.BOSS}_${ConstKeys.KEY}", String::class)!! else "NO_BOSS_KEY_FOR_GATE"
        triggerable = spawnProps.getOrDefault(ConstKeys.TRIGGER, !miniBossGate, Boolean::class)
        resettable = spawnProps.getOrDefault(ConstKeys.RESET, true, Boolean::class)
        showCloseEvent = spawnProps.getOrDefault(ConstKeys.CLOSE, true, Boolean::class)

        reset()
    }

    override fun onDestroy() {
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_SPAWN -> if (resettable) reset()
            EventType.END_ROOM_TRANS -> {
                val room = event.getProperty(ConstKeys.ROOM) as RectangleMapObject
                if (nextRoomKey == room.name) transitionFinished = true
            }

            EventType.MINI_BOSS_DEAD -> {
                if (miniBossGate) {
                    val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!
                    GameLogger.debug(TAG, "This boss key: $thisBossKey. Other boss key: ${boss.bossKey}")
                    if (thisBossKey == boss.bossKey) triggerable = true
                }
            }
        }
    }

    override fun reset() {
        timer.reset()
        transitionFinished = false
        gateState = GateState.OPENABLE
        triggerable = !miniBossGate
    }

    fun trigger() {
        if (!triggerable) return
        gateState = GateState.OPENING
        playSoundNow(SoundAsset.BOSS_DOOR_SOUND, false)
        game.eventsMan.submitEvent(Event(EventType.GATE_INIT_OPENING))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (gateState == GateState.OPENING) {
            timer.update(it)
            if (timer.isFinished()) {
                GameLogger.debug(TAG, "Set gate to OPENED")
                timer.reset()
                gateState = GateState.OPEN
                game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_OPENING))
                game.eventsMan.submitEvent(Event(EventType.NEXT_ROOM_REQ, props(ConstKeys.ROOM to nextRoomKey)))
            }
        }

        if (gateState == GateState.OPEN) {
            if (transitionFinished) {
                GameLogger.debug(TAG, "Set gate to CLOSING")
                transitionFinished = false
                gateState = GateState.CLOSING
                if (showCloseEvent) requestToPlaySound(SoundAsset.BOSS_DOOR_SOUND, false)
                game.eventsMan.submitEvent(Event(EventType.GATE_INIT_CLOSING))
            }
        }

        if (gateState == GateState.CLOSING) {
            timer.update(it)
            if (timer.isFinished()) {
                GameLogger.debug(TAG, "Set gate to CLOSED")
                timer.reset()
                gateState = GateState.CLOSED
                game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_CLOSING))
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val gateFixture = Fixture(body, FixtureType.GATE, GameRectangle())
        gateFixture.attachedToBody = false
        body.addFixture(gateFixture)
        gateFixture.rawShape.color = Color.GREEN
        debugShapes.add { gateFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            val bodySize = if (direction.isHorizontal()) Vector2(2f, 3f) else Vector2(3f, 2f)
            body.setSize(bodySize.scl(ConstVals.PPM.toFloat()))
            body.setCenter(center)

            val gateShape = gateFixture.rawShape as GameRectangle
            val gateSize = if (direction.isHorizontal()) Vector2(1f, 3f) else Vector2(3f, 1f)
            gateShape.setSize(gateSize.scl(ConstVals.PPM.toFloat()))

            when (direction) {
                Direction.UP -> gateShape.setTopCenterToPoint(body.getTopCenterPoint())
                Direction.DOWN -> gateShape.setBottomCenterToPoint(body.getBottomCenterPoint())
                Direction.LEFT -> gateShape.setCenterLeftToPoint(body.getCenterLeftPoint())
                Direction.RIGHT -> gateShape.setCenterRightToPoint(body.getCenterRightPoint())
            }
        })

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden =
                gateState == GateState.OPEN || (gateState.equalsAny(
                    GateState.CLOSING,
                    GateState.CLOSED
                ) && !showCloseEvent)
            _sprite.setOriginCenter()
            _sprite.rotation = when (direction) {
                Direction.UP, Direction.DOWN -> 90f
                Direction.LEFT, Direction.RIGHT -> 0f
            }
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = {
            when (gateState) {
                GateState.OPENABLE, GateState.CLOSED -> "closed"
                GateState.OPENING -> "opening"
                GateState.OPEN -> null
                GateState.CLOSING -> "closing"
            }
        }
        val closed = Animation(atlas!!.findRegion("closed"))
        val opening = Animation(atlas!!.findRegion("opening"), 1, 5, 0.125f, false)
        val closing = Animation(opening, reverse = true)
        val animator = Animator(
            keySupplier, objectMapOf("closed" to closed, "opening" to opening, "closing" to closing)
        )
        return AnimationsComponent(this, animator)
    }
}
