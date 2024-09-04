package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.Body
import com.mega.game.engine.world.BodyComponent
import com.mega.game.engine.world.BodyType
import com.mega.game.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class CapsuleTeleporter(game: MegamanMaverickGame) : MegaGameEntity(game), ITeleporterEntity, IBodyEntity,
    ISpritesEntity, IAnimatedEntity, IAudioEntity, IEventListener {

    companion object {
        const val TAG = "CapsuleTeleporter"
        private const val BLOCK_WIDTH = 2f
        private const val BLOCK_HEIGHT = 1f
        private const val SEND_DELAY = 0.25f
        private const val RECEIVE_DELAY = 0.25f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.TELEPORT)

    private val outgoingBodies = OrderedMap<IBodyEntity, Timer>()
    private val incomingBodies = OrderedMap<IBodyEntity, Timer>()
    private val ignoredBodies = OrderedSet<IBodyEntity>()

    private var teleporterBounds: GameRectangle? = null

    private var upperBlock: Block? = null
    private var lowerBlock: Block? = null

    private var thisKey = -1
    private var nextKey = -1

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            regions.put("frame", atlas.findRegion("$TAG/Frame"))
            regions.put("inactive", atlas.findRegion("$TAG/Inactive"))
            regions.put("active", atlas.findRegion("$TAG/Active"))
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): props = $spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        teleporterBounds = spawnProps.get(ConstKeys.CHILD, RectangleMapObject::class)!!.rectangle.toGameRectangle()

        thisKey = spawnProps.get(ConstKeys.KEY, Int::class)!!
        nextKey = spawnProps.get(ConstKeys.NEXT, Int::class)!!

        upperBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD) as Block
        upperBlock!!.spawn(
            props(
                ConstKeys.CULL_OUT_OF_BOUNDS to false,
                ConstKeys.BOUNDS to GameRectangle().setSize(BLOCK_WIDTH * ConstVals.PPM, BLOCK_HEIGHT * ConstVals.PPM)
                    .setTopCenterToPoint(body.getTopCenterPoint())
            )
        )

        lowerBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD) as Block
        lowerBlock!!.spawn(
            props(
                ConstKeys.CULL_OUT_OF_BOUNDS to false,
                ConstKeys.BOUNDS to GameRectangle().setSize(BLOCK_WIDTH * ConstVals.PPM, BLOCK_HEIGHT * ConstVals.PPM)
                    .setBottomCenterToPoint(body.getBottomCenterPoint())
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        game.eventsMan.removeListener(this)

        upperBlock?.let { it.destroy() }
        upperBlock = null
        lowerBlock?.let { it.destroy() }
        lowerBlock = null

        outgoingBodies.clear()
        incomingBodies.clear()
        ignoredBodies.clear()
    }

    override fun teleportEntity(entity: IBodyEntity) {
        GameLogger.debug(TAG, "thisKey=$thisKey, teleportEntity(): entity=$entity")

        if (ignoredBodies.contains(entity) || outgoingBodies.containsKey(entity)) {
            GameLogger.debug(TAG, "teleportEntity(): entity already in bodiesToSend")
            return
        }

        val onPortalStart = entity.getProperty(ConstKeys.ON_TELEPORT_START) as? () -> Unit
        onPortalStart?.invoke()

        outgoingBodies.put(entity, Timer(SEND_DELAY))
    }

    override fun onEvent(event: Event) {
        if (event.key == EventType.TELEPORT && event.isProperty(ConstKeys.KEY, thisKey)) {
            val entity = event.getProperty(ConstKeys.ENTITY, IBodyEntity::class)!!
            receiveEntity(entity)
        }
    }

    private fun receiveEntity(entity: IBodyEntity) {
        GameLogger.debug(PortalHopper.TAG, "thisKey=$thisKey, receiveEntity(): entity=$entity")

        if (incomingBodies.containsKey(entity)) {
            GameLogger.debug(PortalHopper.TAG, "receiveEntity(): entity already in bodiesToReceive")
            return
        }

        val teleportPosition = lowerBlock!!.body.getTopCenterPoint()
        entity.body.setBottomCenterToPoint(teleportPosition)

        incomingBodies.put(entity, Timer(RECEIVE_DELAY))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val sendIter = outgoingBodies.iterator()
        while (sendIter.hasNext) {
            val entry = sendIter.next()
            val entity = entry.key as MegaGameEntity
            val timer = entry.value

            if (entity.dead) {
                sendIter.remove()
                continue
            }

            val onPortalContinue = entity.getProperty(ConstKeys.ON_TELEPORT_CONTINUE) as? () -> Unit
            onPortalContinue?.invoke()

            timer.update(delta)
            if (timer.isFinished()) {
                GameLogger.debug(TAG, "Send timer finished: thisKey=$thisKey, entity=$entity")

                game.eventsMan.submitEvent(
                    Event(
                        EventType.TELEPORT, props(ConstKeys.ENTITY to entity, ConstKeys.KEY to nextKey)
                    )
                )

                sendIter.remove()
            }
        }

        val receiveIter = incomingBodies.iterator()
        while (receiveIter.hasNext) {
            val entry = receiveIter.next()
            val entity = entry.key
            val timer = entry.value

            if ((entity as MegaGameEntity).dead) {
                receiveIter.remove()
                continue
            }

            val onPortalContinue = entity.getProperty(ConstKeys.ON_TELEPORT_CONTINUE) as? () -> Unit
            onPortalContinue?.invoke()

            timer.update(delta)
            if (timer.isFinished()) {
                GameLogger.debug(PortalHopper.TAG, "Timer finished: thisKey=$thisKey, entity=$entity, timer=$timer")

                requestToPlaySound(SoundAsset.TELEPORT_SOUND, false)

                val onPortalEnd = entity.getProperty(ConstKeys.ON_TELEPORT_END) as? () -> Unit
                onPortalEnd?.invoke()

                ignoredBodies.add(entity)

                receiveIter.remove()
            }
        }

        val ignoredBodiesIter = ignoredBodies.iterator()
        while (ignoredBodiesIter.hasNext) {
            val entity = ignoredBodiesIter.next()
            if ((entity as MegaGameEntity).dead || !body.overlaps(entity.body as Rectangle)) ignoredBodiesIter.remove()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, 4f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val teleporterFixture = Fixture(body, FixtureType.TELEPORTER)
        teleporterFixture.attachedToBody = false
        body.addFixture(teleporterFixture)
        debugShapes.add { teleporterFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            teleporterFixture.rawShape = teleporterBounds!!
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val frameSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        frameSprite.setSize(2f * ConstVals.PPM, 4f * ConstVals.PPM)
        frameSprite.setRegion(regions["frame"]!!)

        val glassSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 2))
        glassSprite.setSize(2f * ConstVals.PPM, 4f * ConstVals.PPM)

        return SpritesComponent(
            orderedMapOf(
                "frame" to frameSprite, "glass" to glassSprite
            ), objectMapOf("frame" to UpdateFunction { _, _sprite ->
                _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            }, "glass" to UpdateFunction { _, _sprite ->
                _sprite.setCenter(body.getCenter())
            })
        )
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? =
            { if (!incomingBodies.isEmpty || !outgoingBodies.isEmpty) "active" else "inactive" }
        val animations = objectMapOf<String, IAnimation>(
            "active" to Animation(regions["active"]!!, 1, 2, 0.05f, true),
            "inactive" to Animation(regions["inactive"]!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(gdxArrayOf({ sprites["glass"] } to animator))
    }
}