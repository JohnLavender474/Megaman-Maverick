package com.megaman.maverick.game.entities.megamanx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.animations.IAnimator
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.components.require
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.splitAndFlatten
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.special.PipePortal
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getPositionPoint
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.behaviors.BehaviorType

const val MEGAMAN_X_SPRITE_KEY = "MegamanX"

internal val HALF_CHARGE_TINT = Color(0.55f, 0.85f, 1f, 1f)
internal val FULL_CHARGE_TINT = Color(1f, 0.95f, 0.45f, 1f)

internal fun MegamanX.shouldHideSprite(): Boolean {
    if (frozen) return false
    if (!inPlay) return true
    return teleporting && teleporter !is PipePortal
}

internal fun MegamanX.animationKey(): String {
    val shoot = if (buster.shooting) "_shoot" else ""
    val rising = when (direction) {
        Direction.UP -> body.physics.velocity.y > 0f
        Direction.DOWN -> body.physics.velocity.y < 0f
        Direction.LEFT -> body.physics.velocity.x < 0f
        Direction.RIGHT -> body.physics.velocity.x > 0f
    }
    return when {
        damaged -> if (bodyOverGround) "damaged" else "air_damaged"
        sabre.active -> sabre.kind!!.bodyRegion
        isBehaviorActive(BehaviorType.AIR_DASHING) -> "dash$shoot"
        isBehaviorActive(BehaviorType.GROUND_DASHING) -> (if (groundDashBraking) "dash_end" else "dash") + shoot
        kickLocked -> "walljump$shoot"
        isBehaviorActive(BehaviorType.WALL_SLIDING) -> "wallslide$shoot"
        wallKicked && isBehaviorActive(BehaviorType.JUMPING) -> "walljump$shoot"
        !bodyOverGround -> (if (rising) "jump" else "fall") + shoot
        walkStarting -> "run_trans$shoot"
        running -> "run$shoot"
        buster.shooting -> "land_shoot"
        else -> "stand"
    }
}

