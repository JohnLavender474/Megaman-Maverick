package com.megaman.maverick.game.entities.sensors

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class Gate(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IAudioEntity, ISpritesEntity, IEventListener,
    Resettable, IDirectional {

    companion object {
        const val TAG = "Gate"
        private const val DURATION = 0.5f
        private val animDefs = objectMapOf(
            GateState.OPENABLE pairTo AnimationDef(),
            GateState.OPENING pairTo AnimationDef(1, 5, 0.1f, false),
            GateState.OPEN pairTo AnimationDef(),
            GateState.CLOSING pairTo AnimationDef(1, 5, 0.1f, false),
            GateState.CLOSED pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class GateState { OPENABLE, OPENING, OPEN, CLOSING, CLOSED }

    // TODO: add other gate types
    enum class GateType { STANDARD }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_SPAWN, EventType.END_ROOM_TRANS, EventType.INTERMEDIATE_BOSS_DEAD
    )
    override lateinit var direction: Direction

    private lateinit var type: GateType
    private lateinit var state: GateState

    private lateinit var nextRoomKey: String
    private var thisBossKey: String? = null

    private val center = Vector2()
    private val timer = Timer(DURATION)

    private var triggerable = true
    private var resettable = false
    private var showCloseEvent = true
    private var transitionFinished = false

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.GATES.source)
            GateType.entries.forEach { type ->
                val prefix = type.name.lowercase()
                GateState.entries.forEach { state ->
                    val regionKey = state.name.lowercase()
                    val fullKey = "$prefix/$regionKey"
                    val region = atlas.findRegion(fullKey)
                    regions.put(fullKey, region)
                }
            }
        }

        super.init()

        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        center.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter())

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.LEFT, String::class)
        direction = Direction.valueOf(directionString.uppercase())

        val typeString = spawnProps.getOrDefault(ConstKeys.TYPE, ConstKeys.STANDARD, String::class)
        type = GateType.valueOf(typeString.uppercase())

        nextRoomKey = spawnProps.get(ConstKeys.ROOM, String::class)!!

        // If this gate has a boss key, then that means that it cannot be triggered until the boss with the boss key
        // has been defeated. Conversely, if there is no boss key, then this gate is always triggerable.
        thisBossKey = spawnProps.get("${ConstKeys.BOSS}_${ConstKeys.KEY}", String::class)
        triggerable = spawnProps.getOrDefault(ConstKeys.TRIGGER, thisBossKey == null, Boolean::class)

        resettable = spawnProps.getOrDefault(ConstKeys.RESET, true, Boolean::class)
        showCloseEvent = spawnProps.getOrDefault(ConstKeys.CLOSE, true, Boolean::class)

        reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")

        when (event.key) {
            EventType.PLAYER_SPAWN -> if (resettable) reset()

            EventType.END_ROOM_TRANS -> {
                val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!
                if (nextRoomKey == room.name) transitionFinished = true
            }

            EventType.INTERMEDIATE_BOSS_DEAD -> {
                thisBossKey?.let { it ->
                    val boss = event.getProperty(ConstKeys.BOSS, AbstractBoss::class)!!
                    GameLogger.debug(
                        TAG, "onEvent(): INTERMEDIATE_BOSS_DEAD: " +
                            "this_boss_key=$thisBossKey, other_boss_key=${boss.bossKey}"
                    )
                    if (it == boss.bossKey) triggerable = true
                }
            }
        }
    }

    override fun reset() {
        timer.reset()
        state = GateState.OPENABLE
        transitionFinished = false
        triggerable = thisBossKey == null
    }

    fun isTriggerable() = state == GateState.OPENABLE

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
        debugShapes.add { gateFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val size = GameObjectPools.fetch(Vector2::class)
            when {
                direction.isHorizontal() -> {
                    size.x = 2f
                    size.y = 3f
                }

                else -> {
                    size.x = 3f
                    size.y = 2f
                }
            }
            size.scl(ConstVals.PPM.toFloat())
            body.setSize(size.x, size.y)

            body.setCenter(center)

            val gateShape = gateFixture.rawShape as GameRectangle
            val gateSize = GameObjectPools.fetch(Vector2::class)
            when {
                direction.isHorizontal() -> {
                    gateSize.x = 1f
                    gateSize.y = 3f
                }

                else -> {
                    gateSize.x = 3f
                    gateSize.y = 1f
                }
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

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val hidden = state == GateState.OPEN ||
                (!showCloseEvent && state.equalsAny(GateState.CLOSING, GateState.CLOSED))
            sprite.hidden = hidden

            sprite.setOriginCenter()
            val rotation = when (direction) {
                Direction.UP, Direction.DOWN -> 90f
                Direction.LEFT, Direction.RIGHT -> 0f
            }
            sprite.rotation = rotation

            sprite.setCenter(body.getCenter())
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier supplier@{
                    val prefix = type.name.lowercase()
                    val key = state.name.lowercase()
                    return@supplier "$prefix/$key"
                }
                .applyToAnimations { animations ->
                    GateType.entries.forEach { type ->
                        val prefix = type.name.lowercase()

                        animDefs.forEach { entry ->
                            val key = entry.key.name.lowercase()
                            val def = entry.value

                            val fullKey = "$prefix/$key"

                            val animation = Animation(regions[fullKey], def.rows, def.cols, def.durations, def.loop)
                            animations.put(fullKey, animation)
                        }
                    }
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.SENSOR

    override fun getTag() = TAG
}
