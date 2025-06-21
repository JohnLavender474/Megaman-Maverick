package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.*
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.*
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionPoint

class PortalHopper(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ITeleporterEntity, IAudioEntity, IEventListener {

    companion object {
        const val TAG = "PortalHopper"

        private const val PORTAL_HOP_IMPULSE = 35f
        private const val PORTAL_HOP_DELAY = 0.5f

        private const val MOON_WAIT_ROT_DELAY = 0.1f
        private const val MOON_HOP_ROT_DELAY = 0.05f

        private val portalAnimDefs = orderedMapOf(
            "wait" pairTo AnimationDef(2, 2, 0.1f, true),
            "launch" pairTo AnimationDef(2, 1, 0.1f, true)
        )
        private val moonAnimDefs = orderedMapOf(
            "moon" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.TELEPORT)

    private lateinit var nextDirection: Direction

    /** sets the entity's direction only when the entity implements [IDirectional] */
    private var nextEntityDirection: Direction? = null

    /** sets the entity's bodily direction; separate from [nextEntityDirection] */
    private var nextBodyDirection: Direction? = null

    private val hopQueueMap = OrderedMap<IBodyEntity, GamePair<Timer, Direction>>()
    private val timerPool = Pool<Timer>(supplier = { Timer() })

    private var thisKey = -1
    private var nextKey = -1

    private var rotation = 0f

    private var launch = false

    private val moonRotationTimer = Timer()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            val keys = Array<String>()
            portalAnimDefs.keys().forEach { key -> keys.add(key) }
            moonAnimDefs.keys().forEach { key -> keys.add(key) }
            keys.forEach { key -> regions.put(key, atlas.findRegion("${TAG}V2/$key")) }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class)
        nextDirection = Direction.valueOf(directionString.uppercase())

        val nextEntityDirString =
            spawnProps.get("${ConstKeys.NEXT}_${ConstKeys.ENTITY}_${ConstKeys.DIRECTION}", String::class)
        nextEntityDirection =
            if (nextEntityDirString != null) Direction.valueOf(nextEntityDirString.uppercase()) else null

        val nextBodyDirString =
            spawnProps.get("${ConstKeys.NEXT}_${ConstKeys.BODY}_${ConstKeys.DIRECTION}", String::class)
        nextBodyDirection =
            if (nextBodyDirString != null) Direction.valueOf(nextBodyDirString.uppercase()) else null

        thisKey = spawnProps.get(ConstKeys.KEY, Int::class)!!
        nextKey = spawnProps.get(ConstKeys.NEXT, Int::class)!!

        rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)

        launch = false

        moonRotationTimer.resetDuration(MOON_WAIT_ROT_DELAY)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): thisKey=$thisKey")
        super.onDestroy()
        hopQueueMap.clear()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        if (event.key == EventType.TELEPORT && event.isProperty(ConstKeys.KEY, thisKey)) {
            GameLogger.debug(TAG, "onEvent(): thisKey=$thisKey, event=$event")

            val entity = event.getProperty(ConstKeys.ENTITY, IBodyEntity::class)!!
            val direction = event.getProperty(ConstKeys.DIRECTION, Direction::class)!!
            val bodyDirection = event.getProperty("${ConstKeys.BODY}_${ConstKeys.DIRECTION}", Direction::class)
            val entityDirection = event.getProperty("${ConstKeys.ENTITY}_${ConstKeys.DIRECTION}", Direction::class)

            receiveEntity(entity, direction, bodyDirection, entityDirection)
        }
    }

    private fun receiveEntity(
        entity: IBodyEntity,
        direction: Direction,
        bodyDirection: Direction?,
        entityDirection: Direction?
    ) {
        GameLogger.debug(
            TAG,
            "receiveEntity(): " +
                "thisKey=$thisKey, " +
                "entity=$entity, " +
                "direction=$direction, " +
                "bodyDirection=$bodyDirection, " +
                "entityDirection=$entityDirection"
        )

        launch = true

        val hopPoint = when (direction) {
            Direction.UP -> body.getPositionPoint(Position.TOP_CENTER)
            Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER)
            Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT)
            Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT)
        }
        when (direction) {
            Direction.UP -> entity.body.setBottomCenterToPoint(hopPoint)
            Direction.DOWN -> entity.body.setTopCenterToPoint(hopPoint)
            Direction.LEFT -> entity.body.setCenterRightToPoint(hopPoint)
            Direction.RIGHT -> entity.body.setCenterLeftToPoint(hopPoint)
        }

        val onPortalStart = entity.getProperty(ConstKeys.ON_TELEPORT_START) as? () -> Unit
        onPortalStart?.invoke()

        if (bodyDirection != null) entity.body.direction = bodyDirection
        if (entityDirection != null && entity is IDirectional) entity.direction = entityDirection

        val timer = timerPool.fetch()
        timer.resetDuration(PORTAL_HOP_DELAY)

        hopQueueMap.put(entity, timer pairTo direction)
    }

    override fun teleportEntity(entity: IBodyEntity) {
        GameLogger.debug(TAG, "teleportEntity(): thisKey=$thisKey, nextKey=$nextKey, entity=$entity")

        game.eventsMan.submitEvent(
            Event(
                EventType.TELEPORT, props(
                    ConstKeys.KEY pairTo nextKey,
                    ConstKeys.ENTITY pairTo entity,
                    ConstKeys.DIRECTION pairTo nextDirection,
                    "${ConstKeys.BODY}_${ConstKeys.DIRECTION}" pairTo nextBodyDirection,
                    "${ConstKeys.ENTITY}_${ConstKeys.DIRECTION}" pairTo nextEntityDirection
                )
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (hopQueueMap.isEmpty) {
            launch = false
            return@UpdatablesComponent
        }

        val iter = hopQueueMap.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val entity = entry.key
            val (timer, direction) = entry.value

            timer.update(delta)

            val onPortalContinue = entity.getProperty(ConstKeys.ON_TELEPORT_CONTINUE) as? () -> Unit
            onPortalContinue?.invoke()

            if (timer.isFinished()) {
                GameLogger.debug(TAG, "update(): thisKey=$thisKey, timer finished")

                requestToPlaySound(SoundAsset.FLOATING_PORTAL_SOUND, false)

                val onPortalEnd = entity.getProperty(ConstKeys.ON_TELEPORT_END) as? () -> Unit
                onPortalEnd?.invoke()

                val velocity = entity.body.physics.velocity
                when (direction) {
                    Direction.UP -> velocity.set(0f, PORTAL_HOP_IMPULSE)
                    Direction.DOWN -> velocity.set(0f, -PORTAL_HOP_IMPULSE)
                    Direction.LEFT -> velocity.set(-PORTAL_HOP_IMPULSE, 0f)
                    Direction.RIGHT -> velocity.set(PORTAL_HOP_IMPULSE, 0f)
                }.scl(ConstVals.PPM.toFloat())

                timerPool.free(timer)

                iter.remove()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.TELEPORTER))
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite("portal", GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            sprite.rotation = if (rotation < 0f) megaman.direction.rotation else rotation
        }
        .sprite("moon", GameSprite(regions["moon"]).also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { delta, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            moonRotationTimer.update(delta)
            if (moonRotationTimer.isFinished()) {
                sprite.rotation += 90f
                moonRotationTimer.resetDuration(if (launch) MOON_HOP_ROT_DELAY else MOON_WAIT_ROT_DELAY)
            }
        }
        .build()

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (launch) "launch" else "wait" }
        val animations = ObjectMap<String, IAnimation>()
        portalAnimDefs.forEach { entry ->
            val key = entry.key
            val (rows, columns, durations, loop) = entry.value
            animations.put(key, Animation(regions[key], rows, columns, durations, loop))
        }
        val animator = Animator(keySupplier, animations)
        val entry: GamePair<() -> GameSprite, IAnimator> = { sprites["portal"] } pairTo animator
        return AnimationsComponent(gdxArrayOf(entry))
    }

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
