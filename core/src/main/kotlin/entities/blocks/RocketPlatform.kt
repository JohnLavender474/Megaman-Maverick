package com.megaman.maverick.game.entities.blocks



import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntitySuppliers
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper

com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntitySuppliers
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper

class RocketPlatform(game: MegamanMaverickGame) : Block(game), IParentEntity, ISpritesEntity, IMotionEntity,
    IEventListener, IDirectionRotatable {

    companion object {
        private var region: TextureRegion? = null
        private const val WIDTH = 0.85f
        private const val HEIGHT = 3f
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS, EventType.END_ROOM_TRANS)
    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }
    override var children = Array<IGameEntity>()

    private val canMove: Boolean
        get() = !game.isCameraRotating()
    private var hidden = false

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "JeffBezosLittleDickRocket")
        super.init()
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)
        game.eventsMan.addListener(this)

        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
        val position = DirectionPositionMapper.getPosition(directionRotation).opposite()
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
        bodyComponent.body.preProcess.put(ConstKeys.MOVE) { if (!canMove) body.physics.velocity.setZero() }
        return bodyComponent
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(4f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = hidden
            sprite.setOriginCenter()
            sprite.rotation = directionRotation.rotation
            sprite.setCenter(body.getCenter())
            val offset = 0.4f * ConstVals.PPM
            when (directionRotation) {
                Direction.UP -> sprite.translateY(-offset)
                Direction.DOWN -> sprite.translateY(offset)
                Direction.LEFT -> sprite.translateX(offset)
                Direction.RIGHT -> sprite.translateX(-offset)
            }
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 7, 0.05f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
