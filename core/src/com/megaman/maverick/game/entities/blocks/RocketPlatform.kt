package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.contracts.ISpriteEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.motion.MotionComponent
import com.engine.motion.Trajectory
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.events.EventType

/** A rocket platform is a block that moves along a trajectory. */
class RocketPlatform(game: MegamanMaverickGame) : Block(game), ISpriteEntity, IEventListener {

  companion object {
    private var region: TextureRegion? = null

    private const val WIDTH = .85f
    private const val HEIGHT = 3f
  }

  override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS, EventType.END_ROOM_TRANS)

  override fun init() {
    super<Block>.init()

    if (region == null)
        region =
            game.assMan.getTextureRegion(
                TextureAsset.PLATFORMS_1.source, "JeffBezosLittleDickRocket")

    addComponent(defineSpritesCompoent())
    addComponent(defineAnimationsComponent())
    addComponent(MotionComponent(this))

    runnablesOnDestroy.add { game.eventsMan.removeListener(this) }
  }

  override fun spawn(spawnProps: Properties) {
    spawnProps.put(ConstKeys.PERSIST, true)
    super.spawn(spawnProps)

    game.eventsMan.addListener(this)

    // define the spawn and bounds
    val spawn = (spawnProps.get(ConstKeys.BOUNDS) as GameRectangle).getCenter()
    val bounds = GameRectangle().setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)
    bounds.setBottomCenterToPoint(spawn)
    body.set(bounds)

    // define the trajectory
    val trajectory = Trajectory(spawnProps.get(ConstKeys.TRAJECTORY) as String, ConstVals.PPM)
    val motionDefinition =
        MotionComponent.MotionDefinition(trajectory) { value, delta ->
          body.physics.velocity.set(value.scl(delta))
        }
    getComponent(MotionComponent::class)!!.put(ConstKeys.TRAJECTORY, motionDefinition)
  }

  override fun onEvent(event: Event) {
    when (event.key) {
      EventType.BEGIN_ROOM_TRANS -> {
        firstSprite!!.hidden = true
        getComponent(MotionComponent::class)?.reset()
      }
      EventType.END_ROOM_TRANS -> firstSprite!!.hidden = false
    }
  }

  private fun defineSpritesCompoent(): SpritesComponent {
    val sprite = GameSprite(region!!)
    sprite.setSize(4f * ConstVals.PPM, 4f * ConstVals.PPM)

    val SpritesComponent = SpritesComponent(this, "rocket" to sprite)
    SpritesComponent.putUpdateFunction("rocket") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getTopCenterPoint(), Position.TOP_CENTER)
      _sprite.translateY(ConstVals.PPM / 16f)
    }

    return SpritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val animation = Animation(region!!, 1, 7, 0.05f, true)
    val animator = Animator(animation)
    return AnimationsComponent(this, animator)
  }
}
