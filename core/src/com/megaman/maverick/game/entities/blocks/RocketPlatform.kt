package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IChildEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.motion.MotionComponent
import com.engine.motion.Trajectory
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntities
import com.megaman.maverick.game.events.EventType

class RocketPlatform(game: MegamanMaverickGame) :
    Block(game), IParentEntity, ISpritesEntity, IEventListener {

    companion object {
        private var region: TextureRegion? = null

        private const val WIDTH = .85f
        private const val HEIGHT = 3f
    }

    override var children = Array<IGameEntity>()
    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS, EventType.END_ROOM_TRANS)

    override fun init() {
        super<Block>.init()

        if (region == null)
            region =
                game.assMan.getTextureRegion(
                    TextureAsset.PLATFORMS_1.source, "JeffBezosLittleDickRocket"
                )

        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.spawn(spawnProps)

        game.eventsMan.addListener(this)

        // define the spawn and bounds
        val spawn = (spawnProps.get(ConstKeys.BOUNDS) as GameRectangle).getBottomCenterPoint()
        val bounds =
            GameRectangle()
                .setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)
                .setBottomCenterToPoint(spawn)
        body.set(bounds)

        // define the trajectory
        val trajectory = Trajectory(spawnProps.get(ConstKeys.TRAJECTORY) as String, ConstVals.PPM)
        val motionDefinition =
            MotionComponent.MotionDefinition(
                motion = trajectory,
                function = { value, _ -> body.physics.velocity.set(value) },
                onReset = { body.set(bounds) })
        getComponent(MotionComponent::class)!!.put(ConstKeys.TRAJECTORY, motionDefinition)

        // spawn the entities triggered by this entity's spawning, collecting any subsequent entities
        // that implement IChildEntity into the children collection
        val subsequentEntities = convertObjectPropsToEntities(spawnProps)
        subsequentEntities.forEach { entry ->
            val (subsequentEntity, subsequentEntityProps) = entry

            if (subsequentEntity is IChildEntity) {
                subsequentEntity.parent = this
                children.add(subsequentEntity)
            }

            game.engine.spawn(subsequentEntity, subsequentEntityProps)
        }
    }

    override fun onDestroy() {
        super<Block>.onDestroy()
        game.eventsMan.removeListener(this)
        children.forEach { it.kill() }
        children.clear()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS -> {
                firstSprite!!.hidden = true
                children.forEach { child ->
                    if (child is ISpritesEntity) child.sprites.values().forEach { it.hidden = true }
                }
                getComponent(MotionComponent::class)?.reset()
            }

            EventType.END_ROOM_TRANS -> {
                firstSprite!!.hidden = false
                children.forEach { child ->
                    if (child is ISpritesEntity) child.sprites.values().forEach { it.hidden = false }
                }
            }
        }
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(4f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getTopCenterPoint(), Position.TOP_CENTER)
            _sprite.translateY(ConstVals.PPM / 16f)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 7, 0.05f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
