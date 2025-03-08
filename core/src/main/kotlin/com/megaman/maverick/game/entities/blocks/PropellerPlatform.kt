package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.Trajectory
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionPoint
import com.megaman.maverick.game.world.body.setEntity

class PropellerPlatform(game: MegamanMaverickGame) : Block(game), IMotionEntity, ISpritesEntity, IAnimatedEntity,
    IDamager, IEventListener, IDirectional {

    companion object {
        const val TAG = "PropellerPlatform"
        private const val BODY_WIDTH = 1.25f
        private const val BODY_HEIGHT = 0.25f
        private const val DAMAGER_WIDTH = 0.5f
        private const val DAMAGER_HEIGHT = 0.25f
        private const val DAMAGER_OFFSET_Y = 0.6f
        private var region: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.END_ROOM_TRANS
    )
    override lateinit var direction: Direction

    var hidden = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)

        putProperty("${ConstKeys.FEET}_${ConstKeys.SOUND}", false)

        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        val bounds = GameRectangle().setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM).setCenter(center)
        body.set(bounds)

        when {
            spawnProps.containsKey(ConstKeys.DIRECTION) -> {
                var direction = spawnProps.get(ConstKeys.DIRECTION)
                if (direction is String) direction = Direction.valueOf(direction.uppercase())
                direction = direction as Direction
            }

            else -> direction = Direction.UP
        }

        val trajectory = Trajectory(spawnProps.get(ConstKeys.TRAJECTORY) as String, ConstVals.PPM)
        val motionDefinition = MotionComponent.MotionDefinition(
            motion = trajectory,
            function = { value, _ -> body.physics.velocity.set(value) },
            onReset = { body.set(bounds) })
        putMotionDefinition(ConstKeys.TRAJECTORY, motionDefinition)

        hidden = false

        game.eventsMan.addListener(this)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        clearMotionDefinitions()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.BEGIN_ROOM_TRANS -> {
                hidden = true
                resetMotionComponent()
            }

            EventType.END_ROOM_TRANS -> hidden = false
            EventType.PLAYER_SPAWN -> resetMotionComponent()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        val body = component.body

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(DAMAGER_WIDTH * ConstVals.PPM, DAMAGER_HEIGHT * ConstVals.PPM)
        )
        damagerFixture.offsetFromBodyAttachment.y = -DAMAGER_OFFSET_Y * ConstVals.PPM
        damagerFixture.setEntity(this)
        body.addFixture(damagerFixture)
        addDebugShapeSupplier { damagerFixture }

        return component
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            val position = when (direction) {
                Direction.UP -> Position.TOP_CENTER
                Direction.DOWN -> Position.BOTTOM_CENTER
                Direction.LEFT -> Position.CENTER_LEFT
                Direction.RIGHT -> Position.CENTER_RIGHT
            }
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
            sprite.hidden = hidden
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 8, 0.05f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
