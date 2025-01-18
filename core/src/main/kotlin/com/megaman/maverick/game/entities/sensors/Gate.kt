package com.megaman.maverick.game.entities.sensors

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
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
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
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

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

    lateinit var state: GateState
        private set
    lateinit var direction: Direction
        private set

    private val center = Vector2()
    private val timer = Timer(DURATION)
    private var triggerable = true
    private var resettable = false
    private var transitionFinished = false
    private var showCloseEvent = true
    private var miniBossGate = false
    private lateinit var nextRoomKey: String
    private lateinit var thisBossKey: String

    override fun getType() = EntityType.SENSOR

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

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.LEFT, String::class)
        direction = Direction.valueOf(directionString.uppercase())

        miniBossGate = spawnProps.getOrDefault("${ConstKeys.MINI}_${ConstKeys.BOSS}", false, Boolean::class)
        thisBossKey = if (miniBossGate)
            spawnProps.get("${ConstKeys.BOSS}_${ConstKeys.KEY}", String::class)!! else "NO_BOSS_KEY_FOR_GATE"

        nextRoomKey = spawnProps.get(ConstKeys.ROOM, String::class)!!
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
                val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!
                if (nextRoomKey == room.name) transitionFinished = true
            }

            EventType.MINI_BOSS_DEAD -> {
                if (miniBossGate) {
                    val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!
                    GameLogger.debug(TAG, "onEvent(): MINI_BOSS_DEAD: " +
                        "this_boss_key=$thisBossKey, other_boss_key=${boss.bossKey}")
                    if (thisBossKey == boss.bossKey) triggerable = true
                }
            }
        }
    }

    override fun reset() {
        timer.reset()
        transitionFinished = false
        state = GateState.OPENABLE
        triggerable = !miniBossGate
    }

    fun trigger() {
        if (!triggerable) return
        state = GateState.OPENING
        playSoundNow(SoundAsset.BOSS_DOOR_SOUND, false)
        game.eventsMan.submitEvent(Event(EventType.GATE_INIT_OPENING))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (state == GateState.OPENING) {
            timer.update(it)
            if (timer.isFinished()) {
                GameLogger.debug(TAG, "update(): OPEN")
                timer.reset()
                state = GateState.OPEN
                game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_OPENING))
                game.eventsMan.submitEvent(Event(EventType.NEXT_ROOM_REQ, props(ConstKeys.ROOM pairTo nextRoomKey)))
            }
        }

        if (state == GateState.OPEN) {
            if (transitionFinished) {
                GameLogger.debug(TAG, "update(): CLOSING")
                transitionFinished = false
                state = GateState.CLOSING
                if (showCloseEvent) requestToPlaySound(SoundAsset.BOSS_DOOR_SOUND, false)
                game.eventsMan.submitEvent(Event(EventType.GATE_INIT_CLOSING))
            }
        }

        if (state == GateState.CLOSING) {
            timer.update(it)
            if (timer.isFinished()) {
                GameLogger.debug(TAG, "update(): CLOSED")
                timer.reset()
                state = GateState.CLOSED
                game.eventsMan.submitEvent(Event(EventType.GATE_FINISH_CLOSING))
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val gateFixture = Fixture(body, FixtureType.GATE, GameRectangle())
        gateFixture.attachedToBody = false
        body.addFixture(gateFixture)
        debugShapes.add { gateFixture}

        body.preProcess.put(ConstKeys.DEFAULT) {
            val size = GameObjectPools.fetch(Vector2::class)
            if (direction.isHorizontal()) {
                size.x = 2f
                size.y = 3f
            } else {
                size.x = 3f
                size.y = 2f
            }
            size.scl(ConstVals.PPM.toFloat())
            body.setSize(size.x, size.y)

            body.setCenter(center)

            val gateShape = gateFixture.rawShape as GameRectangle
            val gateSize = GameObjectPools.fetch(Vector2::class)
            if (direction.isHorizontal()) {
                gateSize.x = 1f
                gateSize.y = 3f
            } else {
                gateSize.x = 3f
                gateSize.y = 1f
            }
            gateSize.scl(ConstVals.PPM.toFloat())
            gateShape.setSize(gateSize)

            when (direction) {
                Direction.UP -> gateShape.setTopCenterToPoint(body.getPositionPoint(Position.TOP_CENTER))
                Direction.DOWN -> gateShape.setBottomCenterToPoint(body.getPositionPoint(Position.BOTTOM_CENTER))
                Direction.LEFT -> gateShape.setCenterLeftToPoint(body.getPositionPoint(Position.CENTER_LEFT))
                Direction.RIGHT -> gateShape.setCenterRightToPoint(body.getPositionPoint(Position.CENTER_RIGHT))
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden =
                state == GateState.OPEN || (state.equalsAny(
                    GateState.CLOSING,
                    GateState.CLOSED
                ) && !showCloseEvent)
            sprite.setOriginCenter()
            sprite.rotation = when (direction) {
                Direction.UP, Direction.DOWN -> 90f
                Direction.LEFT, Direction.RIGHT -> 0f
            }
            sprite.setCenter(body.getCenter())
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
            keySupplier, objectMapOf("closed" pairTo closed, "opening" pairTo opening, "closing" pairTo closing)
        )
        return AnimationsComponent(this, animator)
    }
}
