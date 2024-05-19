package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.motion.MotionComponent
import com.engine.motion.Trajectory
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.events.EventType

class PropellerPlatform(game: MegamanMaverickGame) : Block(game), IMotionEntity, ISpritesEntity, IAnimatedEntity,
    IEventListener, IDirectionRotatable {

    companion object {
        const val TAG = "PropellerPlatform"
        private var region: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.END_ROOM_TRANS
    )

    override lateinit var directionRotation: Direction

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "PropellerPlatform")
        super<Block>.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        game.eventsMan.addListener(this)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.spawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        if (spawnProps.containsKey(ConstKeys.DIRECTION)) {
            var direction = spawnProps.get(ConstKeys.DIRECTION)
            if (direction is String) direction = Direction.valueOf(direction.uppercase())
            directionRotation = direction as Direction
        } else directionRotation = Direction.UP
        val trajectory = Trajectory(spawnProps.get(ConstKeys.TRAJECTORY) as String, ConstVals.PPM)
        val motionDefinition = MotionComponent.MotionDefinition(motion = trajectory,
            function = { value, _ -> body.physics.velocity.set(value) },
            onReset = { body.set(bounds) })
        putMotionDefinition(ConstKeys.TRAJECTORY, motionDefinition)
    }

    override fun onDestroy() {
        super<Block>.onDestroy()
        game.eventsMan.removeListener(this)
        clearMotionDefinitions()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS -> {
                firstSprite!!.hidden = true
                resetMotionComponent()
            }

            EventType.END_ROOM_TRANS -> firstSprite!!.hidden = false
            EventType.PLAYER_SPAWN -> resetMotionComponent()
        }
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation.rotation
            val position = when (directionRotation) {
                Direction.UP -> Position.TOP_CENTER
                Direction.DOWN -> Position.BOTTOM_CENTER
                Direction.LEFT -> Position.CENTER_LEFT
                Direction.RIGHT -> Position.CENTER_RIGHT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 8, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

}