package com.megaman.maverick.game.entities.megaman.components

import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanKeys
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

const val GROUND_SLIDE_SPRITE_OFFSET_Y = 0.1f

fun Megaman.getSpriteDirection() =
    if (isBehaviorActive(BehaviorType.AIR_DASHING))
        getProperty(MegamanKeys.DIRECTION_ON_AIR_DASH, Direction::class)!!
    else directionRotation

fun Megaman.shouldFlipSpriteX() =
    !maverick && if (getSpriteDirection() == Direction.RIGHT) facing == Facing.RIGHT else facing == Facing.LEFT

fun Megaman.shouldFlipSpriteY() = getSpriteDirection() == Direction.DOWN

fun Megaman.getSpriteRotation() = when (getSpriteDirection()) {
    Direction.UP, Direction.DOWN -> 0f
    Direction.LEFT -> 90f
    Direction.RIGHT -> 270f
}

fun Megaman.getSpriteXTranslation() = when (getSpriteDirection()) {
    Direction.UP, Direction.DOWN -> when {
        megamanAnimator.currentKey?.contains("JumpShoot") == true -> 0.1f * facing.value
        else -> 0f
    }

    Direction.LEFT -> {
        when {
            isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0.3f
            else -> 0.2f
        }
    }

    Direction.RIGHT -> {
        when {
            isBehaviorActive(BehaviorType.GROUND_SLIDING) -> -0.3f
            else -> -0.2f
        }
    }
}

fun Megaman.getSpriteYTranslation() = when (getSpriteDirection()) {
    Direction.UP -> when {
        !body.isSensing(BodySense.FEET_ON_GROUND) && !isBehaviorActive(BehaviorType.WALL_SLIDING) -> -0.25f
        isBehaviorActive(BehaviorType.GROUND_SLIDING) -> -GROUND_SLIDE_SPRITE_OFFSET_Y
        else -> 0f
    }

    Direction.DOWN -> when {
        isBehaviorActive(BehaviorType.GROUND_SLIDING) -> GROUND_SLIDE_SPRITE_OFFSET_Y
        else -> 0.075f
    }

    Direction.LEFT, Direction.RIGHT -> when {
        megamanAnimator.currentKey?.contains("JumpShoot") == true -> 0.1f * facing.value
        else -> 0f
    }
}

internal fun Megaman.defineSpritesComponent(): SpritesComponent {
    val spritesComponent = SpritesComponent()

    val megamanSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
    megamanSprite.setSize(2.5f * ConstVals.PPM)
    spritesComponent.sprites.put("megaman", megamanSprite)
    spritesComponent.putUpdateFunction("megaman") { _, player ->
        val direction = getSpriteDirection()
        player.setFlip(shouldFlipSpriteX(), shouldFlipSpriteY())
        player.setOriginCenter()
        player.rotation = getSpriteRotation()
        val position = DirectionPositionMapper.getInvertedPosition(direction)
        player.setPosition(body.getPositionPoint(position), position)
        player.translateX(getSpriteXTranslation() * ConstVals.PPM)
        player.translateY(getSpriteYTranslation() * ConstVals.PPM)
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
        flame.rotation = directionRotation.rotation

        val verticalOffset = -0.25f * ConstVals.PPM
        val facingOffsetScaled = -0.45f * facing.value * ConstVals.PPM
        val offset = when (directionRotation) {
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
