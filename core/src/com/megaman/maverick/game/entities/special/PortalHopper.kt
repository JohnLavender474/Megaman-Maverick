package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class PortalHopper(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpriteEntity, IAnimatedEntity,
    ITeleporterEntity, IAudioEntity, IEventListener {

    companion object {
        const val TAG = "PortalHopper"
        private var region: TextureRegion? = null
        private const val PORTAL_HOP_IMPULSE = 35f
        private const val PORTAL_HOP_DELAY = 0.25f
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.TELEPORT)

    private lateinit var impulse: Vector2

    private val hopQueue = Array<Pair<IBodyEntity, Timer>>()

    private var thisKey = -1
    private var nextKey = -1
    private var launch = false

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.SPECIALS_1.source, "PortalHopper")
        addComponent(AudioComponent(this))
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): props = $spawnProps")
        super.spawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP.name, String::class)
        val direction = Direction.valueOf(directionString.uppercase())
        impulse = (when (direction) {
            Direction.UP -> Vector2(0f, PORTAL_HOP_IMPULSE)
            Direction.DOWN -> Vector2(0f, -PORTAL_HOP_IMPULSE)
            Direction.LEFT -> Vector2(-PORTAL_HOP_IMPULSE, 0f)
            Direction.RIGHT -> Vector2(PORTAL_HOP_IMPULSE, 0f)
        }).scl(ConstVals.PPM.toFloat())

        thisKey = spawnProps.get(ConstKeys.KEY, Int::class)!!
        nextKey = spawnProps.get(ConstKeys.NEXT, Int::class)!!
        launch = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super<GameEntity>.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        if (event.key == EventType.TELEPORT && event.isProperty(ConstKeys.KEY, thisKey)) {
            val entity = event.getProperty(ConstKeys.ENTITY, IBodyEntity::class)!!
            receiveEntity(entity)
        }
    }

    override fun teleportEntity(entity: IBodyEntity) {
        GameLogger.debug(TAG, "teleportEntity(): entity=$entity")
        game.eventsMan.submitEvent(
            Event(
                EventType.TELEPORT, props(
                    ConstKeys.ENTITY to entity, ConstKeys.KEY to nextKey
                )
            )
        )
    }

    private fun receiveEntity(entity: IBodyEntity) {
        GameLogger.debug(TAG, "receiveEntity(): entity=$entity")
        launch = true

        val hopPoint = body.getTopCenterPoint()
        entity.body.setBottomCenterToPoint(hopPoint)

        val onPortalStart = entity.getProperty(ConstKeys.ON_PORTAL_HOPPER_START) as? () -> Unit
        onPortalStart?.invoke()

        hopQueue.add(entity to Timer(PORTAL_HOP_DELAY))
        GameLogger.debug(TAG, "teleportEntity(): entity=$entity, hopPoint=$hopPoint")
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
        if (hopQueue.isEmpty) {
            launch = false
            return@UpdatablesComponent
        }

        val iter = hopQueue.iterator()
        while (iter.hasNext()) {
            val (entity, timer) = iter.next()
            timer.update(delta)

            val onPortalContinue = entity.getProperty(ConstKeys.ON_PORTAL_HOPPER_CONTINUE) as? () -> Unit
            onPortalContinue?.invoke()

            if (timer.isFinished()) {
                GameLogger.debug(TAG, "Timer finished: entity=$entity, timer=$timer")
                val onPortalEnd = entity.getProperty(ConstKeys.ON_PORTAL_HOPPER_END) as? () -> Unit
                onPortalEnd?.invoke()
                entity.body.physics.velocity.set(impulse)
                requestToPlaySound(SoundAsset.TELEPORT_SOUND, false)
                iter.remove()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()

        val teleporterFixture = Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.TELEPORTER)
        body.addFixture(teleporterFixture)
        teleporterFixture.shape.color = Color.BLUE
        debugShapes.add { teleporterFixture.shape }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())

        val spritesComponent = SpritesComponent(this, "hopper" to sprite)
        spritesComponent.putUpdateFunction("hopper") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (launch) "launch" else "wait" }
        val animations = objectMapOf<String, IAnimation>(
            "launch" to Animation(region!!, 1, 3, 0.05f, true), "wait" to Animation(region!!, 1, 3, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}