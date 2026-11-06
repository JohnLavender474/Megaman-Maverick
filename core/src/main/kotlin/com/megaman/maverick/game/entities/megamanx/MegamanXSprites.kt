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

private val HALF_CHARGE_TINT = Color(0.55f, 0.85f, 1f, 1f)
private val FULL_CHARGE_TINT = Color(1f, 0.95f, 0.45f, 1f)

internal fun MegamanX.shouldHideSprite(): Boolean {
    if (frozen) return false
    if (!inPlay) return true
    return teleporting && teleporter !is PipePortal
}

internal fun MegamanX.defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
    val component = SpritesComponent(MEGAMAN_X_SPRITE_KEY pairTo sprite)
    var flashTime = 0f
    component.putPreProcess(MEGAMAN_X_SPRITE_KEY) { delta, player ->
        player.setSize(MegamanXValues.SPRITE_SIZE * ConstVals.PPM, MegamanXValues.SPRITE_SIZE * ConstVals.PPM)
        val stripFacesLeft = isBehaviorActive(BehaviorType.WALL_SLIDING)
        player.setFlip(if (stripFacesLeft) facing == Facing.RIGHT else facing == Facing.LEFT, false)
        player.setOriginCenter()
        player.rotation = direction.rotation
        val position = DirectionPositionMapper.getInvertedPosition(direction)
        player.setPosition(body.getPositionPoint(position), position)

        // TODO: the charge shows as a tint flash in code, because the 1LMX sheet has no charge frames (item 081)
        val tint = when (chargeStatus) {
            MegaChargeStatus.FULLY_CHARGED -> FULL_CHARGE_TINT
            MegaChargeStatus.HALF_CHARGED -> HALF_CHARGE_TINT
            MegaChargeStatus.NOT_CHARGED -> null
        }
        flashTime = if (tint == null) 0f else flashTime + delta
        val flashOn = tint != null && (flashTime / MegamanXValues.CHARGE_FLASH_TIME).toInt() % 2 == 0
        player.setColor(if (flashOn) tint else Color.WHITE)

        player.setAlpha(if (damageRecovery.flash) 0f else 1f)
        player.hidden = shouldHideSprite()
    }
    return component
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
        isBehaviorActive(BehaviorType.AIR_DASHING) -> "dash$shoot"
        isBehaviorActive(BehaviorType.WALL_SLIDING) -> "wallslide$shoot"
        kickLocked -> "walljump$shoot"
        !bodyOverGround -> (if (rising) "jump" else "fall") + shoot
        walkStarting -> "run_trans$shoot"
        running -> "run$shoot"
        buster.shooting -> "land_shoot"
        else -> "stand"
    }
}

internal fun MegamanX.defineAnimationsComponent(): AnimationsComponent {
    val atlas = services.assets.getTextureAtlas(TextureAsset.MEGAMAN_X.source)
    fun frames(name: String, count: Int): Array<TextureRegion> =
        atlas.findRegion(name)!!.splitAndFlatten(count, 1, Array())

    fun part(name: String, count: Int, from: Int, until: Int, duration: Float, loop: Boolean): Animation {
        val all = frames(name, count)
        val out = Array<TextureRegion>()
        for (i in from until until) out.add(all[i])
        return Animation(out, duration, loop)
    }

    val animations = ObjectMap<String, IAnimation>()
    // TODO: the stand uses the simplified 2-frame strip (eyes open, blink) with Megaman's timing; the 5-frame
    //  "stand" strip from the 1LMX sheet needs more work before X uses it (item 081)
    animations.put("stand", Animation(frames("stand_simplified", 2), gdxArrayOf(1.5f, 0.15f)))
    animations.put("land_shoot", Animation(frames("land_shoot", 1), 1f))
    animations.put("run_trans", Animation(frames("run_trans", 1), 1f))
    animations.put("run_trans_shoot", Animation(frames("run_trans_shoot", 1), 1f))
    animations.put("run", Animation(frames("run", 8), 0.08f))
    animations.put("run_shoot", Animation(frames("run_shoot", 8), 0.08f))
    animations.put("jump", part("jump_v2", 5, 0, 4, 0.08f, false))
    animations.put("fall", part("jump_v2", 5, 4, 5, 0.1f, true))
    animations.put("jump_shoot", part("jump_shoot_v2", 5, 0, 4, 0.08f, false))
    animations.put("fall_shoot", part("jump_shoot_v2", 5, 4, 5, 0.1f, true))
    animations.put("damaged", Animation(frames("damaged", 3), 0.1f, false))
    val holdThenStay = gdxArrayOf(MegamanXValues.AIR_DASH_HOVER_TIME, 1f)
    animations.put("dash", Animation(frames("dash", 2), holdThenStay, false))
    animations.put("dash_shoot", Animation(frames("dash_shoot", 2), holdThenStay, false))
    val grabThenSlide = gdxArrayOf(MegamanXValues.WALL_GRAB_TIME, 1f)
    animations.put("wallslide", Animation(frames("wallslide", 2), grabThenSlide, false))
    animations.put("wallslide_shoot", Animation(frames("wallslide_shoot", 2), grabThenSlide, false))
    animations.put("walljump", Animation(frames("walljump", 3), MegamanXValues.WALL_KICK_LOCK_TIME / 3f, false))
    animations.put("walljump_shoot", Animation(frames("walljump_shoot", 3), MegamanXValues.WALL_KICK_LOCK_TIME / 3f, false))
    animations.put("air_damaged", Animation(frames("air_damaged", 3), 0.1f, false))

    val animator = Animator(
        keySupplier = { animationKey() },
        animations = animations,
        onChangeKey = { _, currentKey, nextKey ->
            if (currentKey != null && nextKey != null &&
                currentKey.removeSuffix("_shoot") == nextKey.removeSuffix("_shoot")
            ) animations[currentKey]?.getCurrentTime()?.let { animations[nextKey]?.setCurrentTime(it) }
        }
    )
    return AnimationsComponent(orderedMapOf<Any, IAnimator>(MEGAMAN_X_SPRITE_KEY pairTo animator), sprites)
}
