package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
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
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class PortalHopper(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ITeleporterEntity, IAudioEntity, IEventListener {

    companion object {
        const val TAG = "PortalHopper"
        private var waitRegion: TextureRegion? = null
        private var launchRegion: TextureRegion? = null
        private const val PORTAL_HOP_IMPULSE = 35f
        private const val PORTAL_HOP_DELAY = 0.5f
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.TELEPORT)

    private lateinit var thisDirection: Direction
    private lateinit var nextDirection: Direction

    private val hopQueue = Array<GamePair<IBodyEntity, Timer>>()

    private var thisKey = -1
    private var nextKey = -1
    private var launch = false
    private var rotation = 0f

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        if (waitRegion == null || launchRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            waitRegion = atlas.findRegion("PortalHopper/Wait")
            launchRegion = atlas.findRegion("PortalHopper/Launch")
        }
        addComponent(AudioComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): props = $spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP.name, String::class)
        nextDirection = Direction.valueOf(directionString.uppercase())

        thisKey = spawnProps.get(ConstKeys.KEY, Int::class)!!
        nextKey = spawnProps.get(ConstKeys.NEXT, Int::class)!!
        rotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)

        launch = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): thisKey=$thisKey")
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "thisKey=$thisKey, onEvent(): event=$event")
        if (event.key == EventType.TELEPORT && event.isProperty(ConstKeys.KEY, thisKey)) {
            val entity = event.getProperty(ConstKeys.ENTITY, IBodyEntity::class)!!
            val direction = event.getProperty(ConstKeys.DIRECTION, Direction::class)!!
            receiveEntity(entity, direction)
        }
    }

    private fun receiveEntity(entity: IBodyEntity, direction: Direction) {
        GameLogger.debug(TAG, "thisKey=$thisKey, receiveEntity(): entity=$entity")

        thisDirection = direction
        launch = true

        val hopPoint = when (thisDirection) {
            Direction.UP -> body.getPositionPoint(Position.TOP_CENTER)
            Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER)
            Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT)
            Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT)
        }
        when (thisDirection) {
            Direction.UP -> entity.body.setBottomCenterToPoint(hopPoint)
            Direction.DOWN -> entity.body.setTopCenterToPoint(hopPoint)
            Direction.LEFT -> entity.body.setCenterRightToPoint(hopPoint)
            Direction.RIGHT -> entity.body.setCenterLeftToPoint(hopPoint)
        }

        val onPortalStart = entity.getProperty(ConstKeys.ON_TELEPORT_START) as? () -> Unit
        onPortalStart?.invoke()

        hopQueue.add(entity pairTo Timer(PORTAL_HOP_DELAY))
        GameLogger.debug(TAG, "teleportEntity(): thisKey=$thisKey, entity=$entity, hopPoint=$hopPoint")
    }

    override fun teleportEntity(entity: IBodyEntity) {
        GameLogger.debug(TAG, "thisKey=$thisKey, teleportEntity(): entity=$entity")
        game.eventsMan.submitEvent(
            Event(
                EventType.TELEPORT, props(
                    ConstKeys.ENTITY pairTo entity,
                    ConstKeys.KEY pairTo nextKey,
                    ConstKeys.DIRECTION pairTo nextDirection
                )
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (hopQueue.isEmpty) {
            launch = false
            return@UpdatablesComponent
        }

        val iter = hopQueue.iterator()
        while (iter.hasNext()) {
            val (entity, timer) = iter.next()
            timer.update(delta)

            val onPortalContinue = entity.getProperty(ConstKeys.ON_TELEPORT_CONTINUE) as? () -> Unit
            onPortalContinue?.invoke()

            if (timer.isFinished()) {
                GameLogger.debug(TAG, "Timer finished: thisKey=$thisKey, entity=$entity, timer=$timer")

                requestToPlaySound(SoundAsset.FLOATING_PORTAL_SOUND, false)

                val onPortalEnd = entity.getProperty(ConstKeys.ON_TELEPORT_END) as? () -> Unit
                onPortalEnd?.invoke()

                val impulse = (when (thisDirection) {
                    Direction.UP -> Vector2(0f, PORTAL_HOP_IMPULSE)
                    Direction.DOWN -> Vector2(0f, -PORTAL_HOP_IMPULSE)
                    Direction.LEFT -> Vector2(-PORTAL_HOP_IMPULSE, 0f)
                    Direction.RIGHT -> Vector2(PORTAL_HOP_IMPULSE, 0f)
                }).scl(ConstVals.PPM.toFloat())
                entity.body.physics.velocity.set(impulse)

                iter.remove()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        val teleporterFixture = Fixture(body, FixtureType.TELEPORTER, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(teleporterFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setOriginCenter()
            _sprite.rotation = rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (launch) "launch" else "wait" }
        val animations = objectMapOf<String, IAnimation>(
            "launch" pairTo Animation(launchRegion!!, 1, 3, 0.05f, true),
            "wait" pairTo Animation(waitRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
