package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.audio.AudioComponent
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.time.Timer
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset

class Disintegration(game: MegamanMaverickGame) : GameEntity(game), ISpriteEntity, IAudioEntity {

  companion object {
    private const val DURATION = .275f
    private var disintegrationRegion: TextureRegion? = null
  }

  private val durationTimer = Timer(DURATION)

  override fun init() {
    if (disintegrationRegion == null)
        disintegrationRegion =
            game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "Disintegration")

    addComponent(AudioComponent(this))
    addComponent(defineSpriteComponent())
    addComponent(defineAnimationsComponent())
    addComponent(defineUpdatablesComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    durationTimer.reset()

    val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
    (firstSprite as GameSprite).setPosition(spawn, Position.CENTER)

    requestToPlaySound(SoundAsset.THUMP_SOUND, false)
  }

  private fun defineUpdatablesComponent() =
      UpdatablesComponent(
          this,
          {
            durationTimer.update(it)
            if (durationTimer.isFinished()) {
              kill(props(CAUSE_OF_DEATH_MESSAGE to "Duration timer finished"))
            }
          })

  private fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
    sprite.setSize(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
    return SpriteComponent(this, "disintegration" to sprite)
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val animation = Animation(disintegrationRegion!!, 1, 3, 0.005f, true)
    val animator = Animator(animation)
    return AnimationsComponent(this, animator)
  }
}
