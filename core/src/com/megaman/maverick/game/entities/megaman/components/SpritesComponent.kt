package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.Megaman

internal fun Megaman.defineSpritesComponent(): SpritesComponent {
  val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
  sprite.setSize(2.475f * ConstVals.PPM, 1.875f * ConstVals.PPM)
  sprite.setOriginCenter()

  val SpritesComponent = SpritesComponent(this, "player" to sprite)
  SpritesComponent.putUpdateFunction("player") { _, player ->
    player as GameSprite
    player.hidden = !ready

    val flipX = !maverick && facing == Facing.LEFT
    val flipY = upsideDown
    player.setFlip(flipX, flipY)

    val position = if (upsideDown) Position.TOP_CENTER else Position.BOTTOM_CENTER
    val bodyPosition = body.getPositionPoint(position)
    player.setPosition(bodyPosition, position)

    player.setAlpha(if (damageFlash) 0f else 1f)
  }

  return SpritesComponent
}
