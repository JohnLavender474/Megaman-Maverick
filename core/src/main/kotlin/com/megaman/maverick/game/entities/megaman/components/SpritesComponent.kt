package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.*
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanKeys
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint

const val MEGAMAN_SPRITE_KEY = "Megaman"
const val MEGAMAN_SPRITE_SIZE = 3f

const val JETPACK_FLAME_SPRITE_KEY = "JetpackFlame"
const val JETPACK_FLAME_SPRITE_SIZE = 1f

const val DAMAGE_BURST_SPRITE_KEY = "DamagedBurst"
const val DAMAGE_BURST_SPRITE_SIZE = MEGAMAN_SPRITE_SIZE
const val DAMAGE_BURST_OFFSET = 0.25f

const val DESERT_TORNADO_SPRITE_KEY = "DesertTornado"
const val DESERT_TORNADO_SPRITE_SIZE = 3f
const val DESERT_TORNADO_OFFSET = -0.1f

const val GROUND_SLIDE_SPRITE_OFFSET_Y = 0.1f

fun Megaman.getSpriteDirection() = when {
    isBehaviorActive(BehaviorType.AIR_DASHING) -> getProperty(MegamanKeys.DIRECTION_ON_AIR_DASH, Direction::class)!!
    else -> direction
}

fun Megaman.shouldFlipSpriteX() = isFacing(Facing.LEFT)

fun Megaman.shouldFlipSpriteY() = false // getSpriteDirection() == Direction.DOWN

fun Megaman.getSpriteRotation() = when (getSpriteDirection()) {
    Direction.UP -> 0f
    Direction.DOWN -> 180f
    Direction.LEFT -> 90f
    Direction.RIGHT -> 270f
}

fun Megaman.getSpriteXTranslation() = when (getSpriteDirection()) {
    Direction.UP, Direction.DOWN -> 0f
    Direction.LEFT -> when {
        isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) -> 0f
        else -> 0.2f
    }
    Direction.RIGHT -> when {
        isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) -> 0f
        !feetOnGround || isBehaviorActive(BehaviorType.JUMPING) -> -0.5f
        else -> -0.2f
    }
}

fun Megaman.getSpriteYTranslation() = when (getSpriteDirection()) {
    Direction.UP -> when {
        isBehaviorActive(BehaviorType.AIR_DASHING) -> 0f
        !feetOnGround && !isBehaviorActive(BehaviorType.WALL_SLIDING) -> -0.25f
        isBehaviorActive(BehaviorType.GROUND_SLIDING) -> -GROUND_SLIDE_SPRITE_OFFSET_Y
        else -> 0f
    }
    Direction.DOWN -> when {
        isBehaviorActive(BehaviorType.GROUND_SLIDING) -> GROUND_SLIDE_SPRITE_OFFSET_Y
        else -> 0f
    }
    Direction.LEFT -> when {
        isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) -> 0f
        else -> 0f
    }
    Direction.RIGHT -> when {
        isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) -> 0f
        else -> 0f
    }
}

fun Megaman.getSpritePriority(out: DrawingPriority): DrawingPriority {
    out.section = DrawingSection.PLAYGROUND
    out.value = 1
    return out
}

fun Megaman.shouldHideSprite() = !frozen && (!ready || teleporting)

internal fun Megaman.defineSpritesComponent(): SpritesComponent {
    val component = SpritesComponent()
    defineMegamanSprite(component)
    defineJetpackFlameSprite(component)
    defineDamagedBurstSprite(component)
    defineDesertTornatoSprite(component)
    return component
}

private fun Megaman.defineMegamanSprite(component: SpritesComponent) {
    val priority = DrawingPriority()
    val sprite = GameSprite(getSpritePriority(priority))
    component.putSprite(MEGAMAN_SPRITE_KEY, sprite)
    component.putPreProcess(MEGAMAN_SPRITE_KEY) { _, player ->
        player.setSize(MEGAMAN_SPRITE_SIZE * ConstVals.PPM)
        val direction = getSpriteDirection()
        player.setFlip(shouldFlipSpriteX(), shouldFlipSpriteY())
        player.setOriginCenter()
        player.rotation = getSpriteRotation()
        val position = DirectionPositionMapper.getInvertedPosition(direction)
        player.setPosition(body.getPositionPoint(position), position)
        player.translateX(getSpriteXTranslation() * ConstVals.PPM)
        player.translateY(getSpriteYTranslation() * ConstVals.PPM)
        player.setAlpha(if (recoveryFlash) 0f else 1f)
        player.hidden = shouldHideSprite()
    }
}

private fun Megaman.defineJetpackFlameSprite(component: SpritesComponent) {
    val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0), false)
    component.putSprite(JETPACK_FLAME_SPRITE_KEY, sprite)
    component.putPreProcess(JETPACK_FLAME_SPRITE_KEY) { _, flame ->
        flame.setSize(JETPACK_FLAME_SPRITE_SIZE * ConstVals.PPM)

        val hidden = !isBehaviorActive(BehaviorType.JETPACKING)
        flame.hidden = hidden
        if (hidden) return@putPreProcess

        flame.setOriginCenter()
        flame.rotation = direction.rotation

        val verticalOffset = -0.25f * ConstVals.PPM
        val facingOffsetScaled = -0.45f * facing.value * ConstVals.PPM
        val offset = when (direction) {
            Direction.UP -> floatArrayOf(facingOffsetScaled, verticalOffset)
            Direction.DOWN -> floatArrayOf(facingOffsetScaled, -verticalOffset)
            Direction.LEFT -> floatArrayOf(verticalOffset, facingOffsetScaled)
            Direction.RIGHT -> floatArrayOf(-verticalOffset, -facingOffsetScaled)
        }
        val position = body.getPositionPoint(Position.CENTER).add(offset[0], offset[1])
        flame.setPosition(position, Position.CENTER)
    }
}

private fun Megaman.defineDamagedBurstSprite(component: SpritesComponent) {
    val region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, "MegamanDamageBurst")
    val priority = getSpritePriority(DrawingPriority())
    val sprite = GameSprite(region, DrawingPriority(priority.section, priority.value - 1))
    component.putSprite(DAMAGE_BURST_SPRITE_KEY, sprite)
    component.putPreProcess(DAMAGE_BURST_SPRITE_KEY) { _, burst ->
        burst.setSize(DAMAGE_BURST_SPRITE_SIZE * ConstVals.PPM)

        val center = body.getCenter()

        val offset = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> offset.set(0f, DAMAGE_BURST_OFFSET)
            Direction.DOWN -> offset.set(0f, -DAMAGE_BURST_OFFSET)
            Direction.LEFT -> offset.set(-DAMAGE_BURST_OFFSET, 0f)
            Direction.RIGHT -> offset.set(DAMAGE_BURST_OFFSET, 0f)
        }
        offset.scl(ConstVals.PPM.toFloat())
        center.add(offset)

        burst.setCenter(center)

        if (!damaged) {
            burst.hidden = true
            return@putPreProcess
        }

        val animator = getAnimator(MEGAMAN_SPRITE_KEY) as Animator
        val animation = animator.currentAnimation
        if (animation == null) {
            burst.hidden = true
            return@putPreProcess
        }

        val index = animation.getIndex()
        if (index == 0) {
            burst.hidden = true
            return@putPreProcess
        }

        burst.hidden = false
    }
}

private fun Megaman.defineDesertTornatoSprite(component: SpritesComponent) {
    val region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, "DesertTornado")
    val priority = getSpritePriority(DrawingPriority())
    val sprite = GameSprite(region, DrawingPriority(priority.section, priority.value + 1))
    component.putSprite(DESERT_TORNADO_SPRITE_KEY, sprite)
    component.putPreProcess(DESERT_TORNADO_SPRITE_KEY) { _, tornado ->
        tornado.setSize(DESERT_TORNADO_SPRITE_SIZE * ConstVals.PPM)

        val center = body.getCenter()

        val offset = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> offset.set(0f, DESERT_TORNADO_OFFSET)
            Direction.DOWN -> offset.set(0f, -DESERT_TORNADO_OFFSET)
            Direction.LEFT -> offset.set(-DESERT_TORNADO_OFFSET, 0f)
            Direction.RIGHT -> offset.set(DESERT_TORNADO_OFFSET, 0f)
        }
        offset.scl(ConstVals.PPM.toFloat())
        center.add(offset)

        tornado.setCenter(center)

        if (currentWeapon != MegamanWeapon.NEEDLE_SPIN || !shooting) {
            tornado.hidden = true
            return@putPreProcess
        }

        tornado.hidden = false
    }
}

