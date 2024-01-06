package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.enums.Direction
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

  val SpritesComponent = SpritesComponent(this, "player" to sprite)
  SpritesComponent.putUpdateFunction("player") { _, player ->
    player as GameSprite
    player.hidden = !ready

    val flipX = !maverick && facing == Facing.LEFT
    val flipY = isDirectionRotatedDown()
    player.setFlip(flipX, flipY)

    if (isDirectionRotatedVertically())
        sprite.setSize(2.475f * ConstVals.PPM, 1.875f * ConstVals.PPM)
    else sprite.setSize(1.875f * ConstVals.PPM, 2.475f * ConstVals.PPM)

    sprite.setOriginCenter()
    val rotation =
        if (isDirectionRotatedLeft()) 90f else if (isDirectionRotatedRight()) 270f else 0f
    player.setRotation(rotation)

    val position =
        when (directionRotation) {
          Direction.UP -> Position.BOTTOM_CENTER
          Direction.DOWN -> Position.TOP_CENTER
          Direction.LEFT -> Position.CENTER_RIGHT
          Direction.RIGHT -> Position.CENTER_LEFT
        }
    val bodyPosition = body.getPositionPoint(position)
    player.setPosition(bodyPosition, position)
    if (directionRotation == Direction.LEFT) player.translateX(-0.15f * ConstVals.PPM)
    else if (directionRotation == Direction.RIGHT) player.translateX(0.15f * ConstVals.PPM)

    player.setAlpha(if (damageFlash) 0f else 1f)
  }

  return SpritesComponent
}
