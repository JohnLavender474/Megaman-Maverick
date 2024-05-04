package com.megaman.maverick.game.entities.sensors

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.Resettable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Gate(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, IAudioEntity, ISpriteEntity, IEventListener,
    Resettable {

    companion object {
        const val TAG = "Gate"
        private var atlas: TextureAtlas? = null
        private const val DURATION = 0.5f
    }

    enum class GateState {
        OPENABLE, OPENING, OPEN, CLOSING, CLOSED
    }

    override val eventKeyMask =
        objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.END_ROOM_TRANS, EventType.MINI_BOSS_DEAD)

    val center = Vector2()

    lateinit var state: GateState
        private set
    lateinit var direction: Direction
        private set

    private val timer = Timer(DURATION)
    private val audioMan: MegaAudioManager
        get() = getMegamanMaverickGame().audioMan

    private lateinit var nextRoomKey: String

    private var triggerable = true
    private var miniBossGate = false
    private var resettable = false
    private var transitionFinished = false
    private var showCloseEvent = true

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.GATES.source)
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent(this))
        runnablesOnDestroy.add { game.eventsMan.removeListener(this) }
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        game.eventsMan.addListener(this)
        center.set((spawnProps.get(ConstKeys.BOUNDS) as GameRectangle).getCenter())

        nextRoomKey = spawnProps.get(ConstKeys.ROOM) as String

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, "left", String::class)
        direction = Direction.valueOf(directionString.uppercase())

        triggerable = spawnProps.getOrDefault(ConstKeys.TRIGGER, true, Boolean::class)
        resettable = spawnProps.getOrDefault(ConstKeys.RESET, false, Boolean::class)
        showCloseEvent = spawnProps.getOrDefault(ConstKeys.CLOSE, true, Boolean::class)

        miniBossGate = spawnProps.getOrDefault("${ConstKeys.MINI}_${ConstKeys.BOSS}", false, Boolean::class)
        if (miniBossGate) triggerable = false

        reset()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_SPAWN -> if (resettable) reset()
            EventType.GAME_OVER -> reset()
            EventType.END_ROOM_TRANS -> {
                val room = event.getProperty(ConstKeys.ROOM) as RectangleMapObject
                if (nextRoomKey == room.name) transitionFinished = true
            }

            EventType.MINI_BOSS_DEAD -> {
                if (miniBossGate) triggerable = true
            }
        }
    }

    override fun reset() {
        timer.reset()
        transitionFinished = false
        state = GateState.OPENABLE
    }

    fun trigger() {
        if (!triggerable) return

        state = GateState.OPENING
        audioMan.playSound(SoundAsset.BOSS_DOOR_SOUND, false)
        game.eventsMan.submitEvent(Event(EventType.GATE_INIT_OPENING))
    }

    private fun stateIsOfOpenType() =
        state == GateState.OPENABLE || state == GateState.OPENING || state == GateState.OPEN

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        if (state == GateState.OPENING) {
            timer.update(it)
            if (timer.isFinished()) {
                GameLogger.debug(TAG, "Set gate to OPENED")
                timer.reset()
                state = GateState.OPEN
                game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_OPENING))
                game.eventsMan.submitEvent(
                    Event(EventType.NEXT_ROOM_REQ, props(ConstKeys.ROOM to nextRoomKey))
                )
            }
        }

        if (state == GateState.OPEN) {
            if (transitionFinished) {
                GameLogger.debug(TAG, "Set gate to CLOSING")
                transitionFinished = false
                state = GateState.CLOSING
                if (showCloseEvent) requestToPlaySound(SoundAsset.BOSS_DOOR_SOUND, false)
                game.eventsMan.submitEvent(Event(EventType.GATE_INIT_CLOSING))
            }
        }

        if (state == GateState.CLOSING) {
            timer.update(it)
            if (timer.isFinished()) {
                GameLogger.debug(TAG, "Set gate to CLOSED")
                timer.reset()
                state = GateState.CLOSED
                game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_CLOSING))
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val gateFixture = Fixture(body, FixtureType.GATE, GameRectangle())
        body.addFixture(gateFixture)
        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            val bodySize = if (direction.isHorizontal()) Vector2(2f, 3f) else Vector2(3f, 2f)
            body.setSize(bodySize.scl(ConstVals.PPM.toFloat()))
            (gateFixture.rawShape as GameRectangle).set(body)
            body.setCenter(center)
        })

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat(), 3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden =
                state == GateState.OPEN || (state.equalsAny(GateState.CLOSING, GateState.CLOSED) && !showCloseEvent)

            _sprite.setOriginCenter()
            _sprite.rotation = when (direction) {
                Direction.UP, Direction.DOWN -> 90f
                Direction.LEFT, Direction.RIGHT -> 0f
            }

            val position = when (state) {
                GateState.CLOSING, GateState.CLOSED -> when (direction) {
                    Direction.UP -> Position.BOTTOM_CENTER
                    Direction.DOWN -> Position.TOP_CENTER
                    Direction.LEFT -> Position.BOTTOM_RIGHT
                    Direction.RIGHT -> Position.BOTTOM_LEFT
                }

                else -> when (direction) {
                    Direction.UP -> Position.TOP_CENTER
                    Direction.DOWN -> Position.BOTTOM_CENTER
                    Direction.LEFT -> Position.BOTTOM_LEFT
                    Direction.RIGHT -> Position.BOTTOM_RIGHT
                }
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)

            val translateY = when (direction) {
                Direction.UP -> if (stateIsOfOpenType()) -1f else 1f
                Direction.DOWN -> if (stateIsOfOpenType()) 1f else -1f
                else -> 0f
            }
            _sprite.translateY(translateY * ConstVals.PPM)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = {
            when (state) {
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
