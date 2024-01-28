package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.interfaces.Resettable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.ISpriteEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

open class AnimatedBlock(game: MegamanMaverickGame) :
    Block(game), ISpriteEntity, IAnimatedEntity, Resettable {

  companion object {
    const val TAG = "AnimatedBlock"
  }

  lateinit var region: TextureRegion
  var hidden = false

  override fun init() {
    super<Block>.init()
    addComponent(defineSpritesComponent())
    addComponent(AnimationsComponent(this, Array()))
  }

  override fun spawn(spawnProps: Properties) {
    GameLogger.debug(TAG, "spawn(): spawnProps = $spawnProps")
    super.spawn(spawnProps)

    val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
    body.set(bounds)

    val animation = spawnProps.get(ConstKeys.ANIMATION, String::class)!!
    AnimatedBlockAnimators.createAndSetAnimations(animation, this)

    if (spawnProps.containsKey(ConstKeys.RUN_ON_SPAWN)) {
      val runOnSpawn = spawnProps.get(ConstKeys.RUN_ON_SPAWN, Runnable::class)!!
      runOnSpawn.run()
    }
  }

  protected open fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()

    val spritesComponent = SpritesComponent(this, "block" to sprite)
    spritesComponent.putUpdateFunction("block") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setSize(body.width, body.height)
      val center = body.getCenter()
      _sprite.setCenter(center.x, center.y)
      _sprite.hidden = hidden
    }

    return spritesComponent
  }

  override fun reset() {
    getAnimatorsArray().forEach { it.second.reset() }
  }
}

object AnimatedBlockAnimators {

  const val TAG = "AnimatedBlockAnimators"

  fun createAndSetAnimations(key: String, animatedBlock: AnimatedBlock) {
    val animators = animatedBlock.getAnimatorsArray()
    animators.clear()

    val assMan = animatedBlock.game.assMan

    val animation: Animation =
        when (key) {
          "Brick1" -> {
            val region = assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, "Brick1")
            Animation(region, 1, 3, 0.05f, false)
          }
          else -> throw IllegalArgumentException("$TAG: Illegal key = $key")
        }

    val animator = Animator(animation)
    animators.add({ animatedBlock.sprites.get("block") } to animator)
  }
}
