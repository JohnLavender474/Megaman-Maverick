package com.megaman.maverick.game.entities.megaman

import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.constants.MegamanProps

/** Returns the [SpriteComponent] of this [Megaman], or creates a new one if it doesn't have one. */
fun Megaman.spriteComponent(): SpriteComponent {
  if (hasComponent(SpriteComponent::class)) return getComponent(SpriteComponent::class)!!

  val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
  sprite.setSize(2.475f * ConstVals.PPM, 1.875f * ConstVals.PPM)
  sprite.setOriginCenter()

  val spriteComponent = SpriteComponent(this, "player" to sprite)
  spriteComponent.putUpdateFunction("player") { _, player ->
    (player as GameSprite).let {
      val flipX = getProperty(MegamanProps.FACING) == "left"
      val flipY = getProperty(MegamanProps.UPSIDE_DOWN) == true

      it.setFlip(flipX, flipY)
    }
  }

  return spriteComponent
}
