package com.megaman.maverick.game.entities.sensors

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Position
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
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Gate(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, IAudioEntity, ISpriteEntity, IEventListener,
    Resettable {

    companion object {
        const val TAG = "Gate"
        private var atlas: TextureAtlas? = null
        private const val DURATION = .5f
    }

    enum class GateOrientation {
        UP, DOWN, HORIZONTAL
    }

    enum class GateState {
        OPENABLE, OPENING, OPEN, CLOSING, CLOSED
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.END_ROOM_TRANS)

    val center = Vector2()

    lateinit var state: GateState
        private set
    lateinit var orientation: GateOrientation
        private set

    private val timer = Timer(DURATION)

    private lateinit var nextRoomKey: String

    private var resettable = false
    private var transitionFinished = false

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
        reset()
        game.eventsMan.addListener(this)
        center.set((spawnProps.get(ConstKeys.BOUNDS) as GameRectangle).getCenter())
        nextRoomKey = spawnProps.get(ConstKeys.ROOM) as String
        orientation = GateOrientation.valueOf(
            spawnProps.getOrDefault(ConstKeys.ORIENTATION, "HORIZONTAL") as String
        )
        resettable = spawnProps.getOrDefault(ConstKeys.RESET, false, Boolean::class)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_SPAWN -> if (resettable) reset()
            EventType.GAME_OVER -> reset()
            EventType.END_ROOM_TRANS -> {
                val room = event.getProperty(ConstKeys.ROOM) as RectangleMapObject
                if (nextRoomKey == room.name) transitionFinished = true
            }
        }
    }

    override fun reset() {
        timer.reset()
        transitionFinished = false
        state = GateState.OPENABLE
    }

    fun trigger() {
        state = GateState.OPENING
        getMegamanMaverickGame().audioMan.playSound(SoundAsset.BOSS_DOOR, false)
        game.eventsMan.submitEvent(Event(EventType.GATE_INIT_OPENING))
    }

    fun stateIsOfOpenType() = state == GateState.OPENABLE || state == GateState.OPENING || state == GateState.OPEN

    fun stateIsOfCloseType() = state == GateState.CLOSING || state == GateState.CLOSED

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
                requestToPlaySound(SoundAsset.BOSS_DOOR, false)
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

        // gate fixture
        val gateFixture = Fixture(GameRectangle(), FixtureType.GATE)
        body.addFixture(gateFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            val bodySize = if (orientation == GateOrientation.HORIZONTAL) Vector2(2f, 3f) else Vector2(3f, 2f)
            body.setSize(bodySize.scl(ConstVals.PPM.toFloat()))
            (gateFixture.shape as GameRectangle).set(body)
            body.setCenter(center)
        })

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()

        sprite.setSize(4f * ConstVals.PPM, 3f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, "gate" to sprite)
        spritesComponent.putUpdateFunction("gate") { _, _sprite ->
            _sprite as GameSprite

            _sprite.hidden = state == GateState.OPEN
            _sprite.setFlip(state == GateState.CLOSING || state == GateState.CLOSED, false)

            _sprite.setOriginCenter()
            _sprite.rotation = when (orientation) {
                GateOrientation.UP, GateOrientation.DOWN -> 270f

                GateOrientation.HORIZONTAL -> 0f
            }

            val position = if (orientation == GateOrientation.HORIZONTAL) when (state) {
                GateState.CLOSING, GateState.CLOSED -> Position.BOTTOM_RIGHT

                else -> Position.BOTTOM_LEFT
            }
            else Position.CENTER
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)

            val translateY = when (orientation) {
                GateOrientation.UP -> if (stateIsOfOpenType()) -1f else 1f
                GateOrientation.DOWN -> if (stateIsOfOpenType()) 1f else -1f
                GateOrientation.HORIZONTAL -> 0f
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

        val closed = Animation(atlas!!.findRegion("closed"), 1, 1, 1f, true)
        val opening = Animation(atlas!!.findRegion("opening"), 1, 4, 0.125f, false)
        val closing = Animation(opening, reverse = true)

        val animator = Animator(
            keySupplier, objectMapOf("closed" to closed, "opening" to opening, "closing" to closing)
        )

        return AnimationsComponent(this, animator)
    }
}
