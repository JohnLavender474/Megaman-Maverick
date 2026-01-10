package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.entities.megaman.components.headFixture
import com.megaman.maverick.game.entities.megaman.components.leftSideFixture
import com.megaman.maverick.game.entities.megaman.components.rightSideFixture
import com.megaman.maverick.game.entities.utils.DrawableShapesComponentBuilder
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class PipePortal(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ITeleporterEntity, IAudioEntity,
    IEventListener, IDirectional {

    companion object {
        const val TAG = "PipePortal"

        private const val TELEPORT_DUR = 0.5f
    }

    override lateinit var direction: Direction

    override val eventKeyMask = objectSetOf<Any>(EventType.TELEPORT)

    private val entering = ObjectMap<IBodyEntity, Timer>()
    private val exiting = ObjectMap<IBodyEntity, Timer>()

    private var thisKey = -1
    private var nextKey = -1

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        direction = Direction.valueOf(spawnProps.get(ConstKeys.DIRECTION, String::class)!!.uppercase())

        thisKey = spawnProps.get("${ConstKeys.THIS}_${ConstKeys.KEY}", Int::class)!!
        nextKey = spawnProps.getOrDefault("${ConstKeys.NEXT}_${ConstKeys.KEY}", -1, Int::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")

        if (event.key == EventType.TELEPORT && event.isProperty(ConstKeys.KEY, thisKey)) {
            val entity = event.getProperty(ConstKeys.ENTITY, IBodyEntity::class)!!

            if (event.isProperty(ConstKeys.TYPE, ConstKeys.EXIT)) pushEntityOut(entity)
            else if (event.isProperty(ConstKeys.TYPE, ConstKeys.ENTER)) pullEntityIn(entity)
        }
    }

    private fun pushEntityOut(entity: IBodyEntity) {
        GameLogger.debug(TAG, "pushEntityOut(): thisKey=$thisKey, entity=$entity")

        if (exiting.containsKey(entity)) {
            GameLogger.debug(TAG, "pushEntityOut(): entity already in 'exiting'")
            return
        }

        val position = DirectionPositionMapper.getPosition(direction)
        entity.body.positionOnPoint(body.getPositionPoint(position), position.opposite())

        exiting.put(entity, Timer(TELEPORT_DUR))

        if (entity == megaman) game.setFocusSnappedAway(false)

        requestToPlaySound(SoundAsset.SMB3_PIPE_SOUND, false)
    }

    private fun pullEntityIn(entity: IBodyEntity) {
        GameLogger.debug(TAG, "pullEntityIn(): thisKey=$thisKey, entity=$entity")

        if (entering.containsKey(entity)) {
            GameLogger.debug(TAG, "pullEntityIn(): entity already in 'entering'")
            return
        }

        val position = DirectionPositionMapper.getPosition(direction)
        entity.body.positionOnPoint(body.getPositionPoint(position.opposite()), position)

        entering.put(entity, Timer(TELEPORT_DUR))

        val onPortalStart = entity.getProperty(ConstKeys.ON_TELEPORT_START) as? (ITeleporterEntity) -> Unit
        onPortalStart?.invoke(this)

        if (entity == megaman) game.setFocusSnappedAway(true)

        requestToPlaySound(SoundAsset.SMB3_PIPE_SOUND, false)
    }

    override fun shouldTeleport(entity: IBodyEntity): Boolean {
        if (!super.shouldTeleport(entity) || entity != megaman || nextKey == -1) return false

        return when (direction) {
            Direction.UP -> megaman.feetFixture.overlaps(body.getBounds()) &&
                game.controllerPoller.isPressed(MegaControllerButton.DOWN)
            Direction.DOWN -> megaman.headFixture.overlaps(body.getBounds()) &&
                game.controllerPoller.isPressed(MegaControllerButton.UP)
            Direction.LEFT -> megaman.rightSideFixture.overlaps(body.getBounds()) &&
                game.controllerPoller.isPressed(MegaControllerButton.RIGHT)
            Direction.RIGHT -> megaman.leftSideFixture.overlaps(body.getBounds()) &&
                game.controllerPoller.isPressed(MegaControllerButton.LEFT)
        }
    }

    override fun isTeleporting(entity: IBodyEntity) =
        entering.containsKey(entity) || exiting.containsKey(entity)

    override fun teleport(entity: IBodyEntity) {
        GameLogger.debug(TAG, "teleport(): entity=$entity")
        pullEntityIn(entity)
    }

    fun isEntering(entity: IBodyEntity) = entering.containsKey(entity)

    fun isExiting(entity: IBodyEntity) = exiting.containsKey(entity)

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val enteringIter = entering.iterator()
        while (enteringIter.hasNext) {
            val entry = enteringIter.next()
            val entity = entry.key as MegaGameEntity
            val timer = entry.value

            if (entity.dead) {
                enteringIter.remove()
                continue
            }

            val onPortalContinue = entity.getProperty(ConstKeys.ON_TELEPORT_CONTINUE) as? () -> Unit
            onPortalContinue?.invoke()

            timer.update(delta)
            if (timer.isFinished()) {
                GameLogger.debug(TAG, "update(): send timer finished; thisKey=$thisKey, entity=$entity")
                game.eventsMan.submitEvent(
                    Event(
                        EventType.TELEPORT, props(
                            ConstKeys.TYPE pairTo ConstKeys.EXIT,
                            ConstKeys.ENTITY pairTo entity,
                            ConstKeys.KEY pairTo nextKey,
                        )
                    )
                )
                enteringIter.remove()
            }
        }

        val exitingIter = exiting.iterator()
        while (exitingIter.hasNext) {
            val entry = exitingIter.next()
            val entity = entry.key as MegaGameEntity
            val timer = entry.value

            if (entity.dead) {
                exitingIter.remove()
                continue
            }

            val onPortalContinue = entity.getProperty(ConstKeys.ON_TELEPORT_CONTINUE) as? () -> Unit
            onPortalContinue?.invoke()

            timer.update(delta)
            if (timer.isFinished()) {
                GameLogger.debug(TAG, "update(): receive timer finished; thisKey=$thisKey, entity=$entity")

                val onPortalEnd = entity.getProperty(ConstKeys.ON_TELEPORT_END) as? () -> Unit
                onPortalEnd?.invoke()

                exitingIter.remove()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val drawableShapesComponent = DrawableShapesComponentBuilder()
        drawableShapesComponent.addDebug { body.getBounds() }
        addComponent(drawableShapesComponent.build())

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.TELEPORTER))
    }

    override fun getType() = EntityType.SPECIAL
}
