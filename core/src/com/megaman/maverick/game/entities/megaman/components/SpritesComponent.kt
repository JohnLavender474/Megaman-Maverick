package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanKeys

internal fun Megaman.defineSpritesComponent(): SpritesComponent {
    val spritesComponent = SpritesComponent()

    val megamanSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
    megamanSprite.setSize(2.475f * ConstVals.PPM, 1.875f * ConstVals.PPM)

    spritesComponent.sprites.put("megaman", megamanSprite)

    spritesComponent.putUpdateFunction("megaman") { _, player ->
        val direction = if (isBehaviorActive(BehaviorType.AIR_DASHING)) getProperty(
            MegamanKeys.DIRECTION_ON_AIR_DASH, Direction::class
        )!!
        else directionRotation

        val flipX = !maverick && facing == Facing.LEFT
        val flipY = direction == Direction.DOWN
        player.setFlip(flipX, flipY)

        val rotation = when (direction) {
            Direction.UP, Direction.DOWN, null -> 0f
            Direction.LEFT -> 90f
            Direction.RIGHT -> 270f
        }
        player.setOriginCenter()
        player.setRotation(rotation)

        val position = when (direction) {
            Direction.UP, null -> Position.BOTTOM_CENTER
            Direction.DOWN -> Position.TOP_CENTER
            Direction.LEFT -> Position.CENTER_RIGHT
            Direction.RIGHT -> Position.CENTER_LEFT
        }
        val bodyPosition = body.getPositionPoint(position)
        player.setPosition(bodyPosition, position)

        val xTranslation = if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) when (direction) {
            Direction.UP, Direction.DOWN, null -> 0f
            Direction.LEFT -> 0.15f
            Direction.RIGHT -> -0.15f
        } else when (direction) {
            Direction.UP, Direction.DOWN, null -> 0f
            Direction.LEFT -> 0.425f
            Direction.RIGHT -> -0.425f
        }
        player.translateX(xTranslation * ConstVals.PPM)
        player.setAlpha(if (damageFlash) 0f else 1f)
    }

    val jetpackFlameSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0), false)
    jetpackFlameSprite.setSize(ConstVals.PPM.toFloat())

    spritesComponent.sprites.put("jetpackFlame", jetpackFlameSprite)

    spritesComponent.putUpdateFunction("jetpackFlame") { _, flame ->
        val hidden = !isBehaviorActive(BehaviorType.JETPACKING)
        flame.hidden = hidden
        if (hidden) {
            return@putUpdateFunction
        }

        flame.setOriginCenter()
        flame.setRotation(directionRotation?.rotation ?: 0f)

        val verticalOffset = -0.25f * ConstVals.PPM
        val facingOffsetScaled = -0.45f * facing.value * ConstVals.PPM
        val offset = when (directionRotation!!) {
            Direction.UP -> floatArrayOf(facingOffsetScaled, verticalOffset)
            Direction.DOWN -> floatArrayOf(facingOffsetScaled, -verticalOffset)
            Direction.LEFT -> floatArrayOf(verticalOffset, facingOffsetScaled)
            Direction.RIGHT -> floatArrayOf(-verticalOffset, -facingOffsetScaled)
        }
        val position = body.getPositionPoint(Position.CENTER).add(offset[0], offset[1])
        flame.setPosition(position, Position.CENTER)
    }

    return spritesComponent
}
