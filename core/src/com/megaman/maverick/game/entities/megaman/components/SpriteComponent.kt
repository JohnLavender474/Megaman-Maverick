package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.enums.Facing
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.Megaman

internal fun Megaman.defineSpriteComponent(): SpriteComponent {
  val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
  sprite.setSize(2.475f * ConstVals.PPM, 1.875f * ConstVals.PPM)
  sprite.setOriginCenter()

  val spriteComponent = SpriteComponent(this, "player" to sprite)

  spriteComponent.putUpdateFunction("player") { _, player ->
    player.hidden = !ready

    val flipX = !maverick && facing == Facing.LEFT
    val flipY = upsideDown
    player.setFlip(flipX, flipY)

    player.setPosition(body.x, body.y)
  }

  return spriteComponent
}
