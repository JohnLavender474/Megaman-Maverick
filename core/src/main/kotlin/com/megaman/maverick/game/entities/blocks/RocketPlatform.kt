package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IChildEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.Trajectory
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntitySuppliers
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint
import com.megaman.maverick.game.world.body.setEntity

class RocketPlatform(game: MegamanMaverickGame) : Block(game), IParentEntity, ISpritesEntity, IMotionEntity,
    IEventListener, IDamager, IDirectional {

    companion object {
        const val TAG = "RocketPlatform"
        private const val WIDTH = 1f
        private const val HEIGHT = 3.5f
        private const val REGION_SUFFIX = "_v2"
        private var region: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS, EventType.END_ROOM_TRANS)
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var children = Array<IGameEntity>()

    private val canMove: Boolean
        get() = !game.isCameraRotating()
    private var hidden = false

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "${TAG}${REGION_SUFFIX}")
        super.init()
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)
        game.eventsMan.addListener(this)

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        val position = DirectionPositionMapper.getPosition(direction).opposite()
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)
            .positionOnPoint(bounds.getPositionPoint(position), position)

        val trajectory = Trajectory(spawnProps.get(ConstKeys.TRAJECTORY, String::class)!!, ConstVals.PPM)
        val motionDefinition = MotionDefinition(
            motion = trajectory,
            doUpdate = { canMove },
            function = { value, _ -> body.physics.velocity.set(value) },
            onReset = {
                body.setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)
                    .positionOnPoint(bounds.getPositionPoint(position), position)
            }
        )
        putMotionDefinition(ConstKeys.TRAJECTORY, motionDefinition)

        val subsequentEntitySuppliers = convertObjectPropsToEntitySuppliers(spawnProps)
        subsequentEntitySuppliers.forEach { entry ->
            val (subsequentEntitySupplier, subsequentEntityProps) = entry
            val entity = subsequentEntitySupplier.invoke()
            if (entity is IChildEntity) {
                entity.parent = this
                children.add(entity)
            }
            entity.spawn(subsequentEntityProps)
        }

        hidden = false
    }

    override fun onDestroy() {
        super.onDestroy()
        game.eventsMan.removeListener(this)
        children.forEach { (it as GameEntity).destroy() }
        children.clear()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS -> {
                hidden = true
                children.forEach { child ->
                    if (child is ISpritesEntity) child.sprites.values().forEach { it.hidden = true }
                }
                resetMotionComponent()
            }

            EventType.END_ROOM_TRANS -> {
                hidden = false
                children.forEach { child ->
                    if (child is ISpritesEntity) child.sprites.values().forEach { it.hidden = false }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val bodyComponent = super.defineBodyComponent()
        val body = bodyComponent.body

        val damagerBounds = GameRectangle().setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        val damagerFixture = Fixture(body, FixtureType.DAMAGER, damagerBounds)
        damagerFixture.attachedToBody = false
        damagerFixture.setEntity(this)
        body.addFixture(damagerFixture)
        debugShapeSuppliers.add { damagerFixture }

        body.preProcess.put(ConstKeys.DAMAGER) {
            val position = DirectionPositionMapper.getPosition(direction)
            damagerBounds.positionOnPoint(body.getPositionPoint(position.opposite()), position)
        }
        body.preProcess.put(ConstKeys.MOVE) { if (!canMove) body.physics.velocity.setZero() }

        return bodyComponent
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(1.6875f * ConstVals.PPM, 4.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = hidden

            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            sprite.setCenter(body.getCenter())

            val offset = 0.4f * ConstVals.PPM
            when (direction) {
                Direction.UP -> sprite.translateY(-offset)
                Direction.DOWN -> sprite.translateY(offset)
                Direction.LEFT -> sprite.translateX(offset)
                Direction.RIGHT -> sprite.translateX(-offset)
            }
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 7, 1, 0.05f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
