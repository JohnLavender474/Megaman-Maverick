package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.IGame2D
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.isSensing
import kotlin.math.abs

internal fun Megaman.defineAnimationsComponent(): AnimationsComponent {
  val keySupplier = { getKey(this) }
  val animator = Animator(keySupplier, createAnimationMap(game))
  return AnimationsComponent(this, { sprites.get("player") }, animator)
}

private fun getKey(megaman: Megaman): String {
  var key: String =
      if (megaman.defineDamageableComponent().isUnderDamage()) {
        "Damaged"
      } else if (megaman.isBehaviorActive(BehaviorType.CLIMBING)) {
        if (!megaman.body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) {
          if (megaman.shooting) {
            "ClimbShoot"
          } else if (megaman.chargingFully) {
            "FinishClimbCharging"
          } else if (megaman.charging) {
            "FinishClimbHalfCharging"
          } else {
            "FinishClimb"
          }
        } else if (megaman.body.physics.velocity.y != 0f) {
          if (megaman.shooting) {
            "ClimbShoot"
          } else if (megaman.chargingFully) {
            "ClimbCharging"
          } else if (megaman.charging) {
            "ClimbHalfCharging"
          } else {
            "Climb"
          }
        } else {
          if (megaman.shooting) {
            "ClimbShoot"
          } else if (megaman.chargingFully) {
            "StillClimbCharging"
          } else if (megaman.charging) {
            "StillClimbHalfCharging"
          } else {
            "StillClimb"
          }
        }
      } else if (megaman.isBehaviorActive(BehaviorType.AIR_DASHING)) {
        if (megaman.chargingFully) {
          "AirDashCharging"
        } else if (megaman.charging) {
          "AirDashHalfCharging"
        } else {
          "AirDash"
        }
      } else if (megaman.isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
        if (megaman.shooting) {
          "GroundSlideShoot"
        } else if (megaman.chargingFully) {
          "GroundSlideCharging"
        } else if (megaman.charging) {
          "GroundSlideHalfCharging"
        } else {
          "GroundSlide"
        }
      } else if (megaman.isBehaviorActive(BehaviorType.WALL_SLIDING)) {
        if (megaman.shooting) {
          "WallSlideShoot"
        } else if (megaman.chargingFully) {
          "WallSlideCharging"
        } else if (megaman.charging) {
          "WallSlideHalfCharging"
        } else {
          "WallSlide"
        }
      } else if (megaman.isBehaviorActive(BehaviorType.SWIMMING)) {
        if (megaman.shooting) {
          "SwimShoot"
        } else if (megaman.chargingFully) {
          "SwimCharging"
        } else if (megaman.charging) {
          "SwimHalfCharging"
        } else {
          "Swim"
        }
      } else if (megaman.isBehaviorActive(BehaviorType.JUMPING) ||
          !megaman.body.isSensing(BodySense.FEET_ON_GROUND)) {
        if (megaman.shooting) {
          "JumpShoot"
        } else if (megaman.chargingFully) {
          "JumpCharging"
        } else if (megaman.charging) {
          "JumpHalfCharging"
        } else {
          "Jump"
        }
      } else if (megaman.body.isSensing(BodySense.FEET_ON_GROUND) && megaman.running) {
        if (megaman.shooting) {
          "RunShoot"
        } else if (megaman.chargingFully) {
          "RunCharging"
        } else if (megaman.charging) {
          "RunHalfCharging"
        } else {
          "Run"
        }
      } else if (megaman.body.isSensing(BodySense.FEET_ON_GROUND) &&
          abs(megaman.body.physics.velocity.x) > ConstVals.PPM / 16f) {
        if (megaman.shooting) {
          "SlipSlideShoot"
        } else if (megaman.chargingFully) {
          "SlipSlideCharging"
        } else if (megaman.charging) {
          "SlipSlideHalfCharging"
        } else {
          "SlipSlide"
        }
      } else {
        if (megaman.shooting) {
          "StandShoot"
        } else if (megaman.chargingFully) {
          "StandCharging"
        } else if (megaman.charging) {
          "StandHalfCharging"
        } else {
          "Stand"
        }
      }

  key = if (megaman.facing == Facing.LEFT) "${key}_Left" else key
  return "${megaman.currentWeapon.name}_$key"
}

private val animationKeys =
    gdxArrayOf(
        "Climb",
        "Climb_Left",
        "ClimbHalfCharging",
        "ClimbHalfCharging_Left",
        "ClimbCharging",
        "ClimbCharging_Left",
        "ClimbShoot",
        "ClimbShoot_Left",
        "StillClimb",
        "StillClimb_Left",
        "StillClimbCharging",
        "StillClimbCharging_Left",
        "StillClimbHalfCharging",
        "StillClimbHalfCharging_Left",
        "FinishClimb",
        "FinishClimb_Left",
        "FinishClimbCharging",
        "FinishClimbCharging_Left",
        "FinishClimbHalfCharging",
        "FinishClimbHalfCharging_Left",
        "Stand",
        "Stand_Left",
        "StandCharging",
        "StandCharging_Left",
        "StandHalfCharging",
        "StandHalfCharging_Left",
        "StandShoot",
        "StandShoot_Left",
        "Damaged",
        "Damaged_Left",
        "Run",
        "Run_Left",
        "RunCharging",
        "RunCharging_Left",
        "RunHalfCharging",
        "RunHalfCharging_Left",
        "RunShoot",
        "RunShoot_Left",
        "Jump",
        "Jump_Left",
        "JumpCharging",
        "JumpCharging_Left",
        "JumpHalfCharging",
        "JumpHalfCharging_Left",
        "JumpShoot",
        "JumpShoot_Left",
        "Swim",
        "Swim_Left",
        "SwimAttack",
        "SwimAttack_Left",
        "SwimCharging",
        "SwimCharging_Left",
        "SwimHalfCharging",
        "SwimHalfCharging_Left",
        "SwimShoot",
        "SwimShoot_Left",
        "WallSlide",
        "WallSlide_Left",
        "WallSlideCharging",
        "WallSlideCharging_Left",
        "WallSlideHalfCharging",
        "WallSlideHalfCharging_Left",
        "WallSlideShoot",
        "WallSlideShoot_Left",
        "GroundSlide",
        "GroundSlide_Left",
        "GroundSlideShoot",
        "GroundSlideShoot_Left",
        "GroundSlideCharging",
        "GroundSlideCharging_Left",
        "GroundSlideHalfCharging",
        "GroundSlideHalfCharging_Left",
        "AirDash",
        "AirDash_Left",
        "AirDashCharging",
        "AirDashCharging_Left",
        "AirDashHalfCharging",
        "AirDashHalfCharging_Left",
        "SlipSlide",
        "SlipSlide_Left",
        "SlipSlideCharging",
        "SlipSlideCharging_Left",
        "SlipSlideHalfCharging",
        "SlipSlideHalfCharging_Left",
        "SlipSlideShoot",
        "SlipSlideShoot_Left",
    )

internal data class AnimationDef(
    internal val rows: Int,
    internal val cols: Int,
    internal val durations: Array<Float>,
) {

  internal constructor(
      rows: Int = 1,
      cols: Int = 1,
      duration: Float = 1f,
  ) : this(rows, cols, gdxArrayOf(duration))
}

private val animationDefMap =
    objectMapOf(
        "Climb" to AnimationDef(1, 2, .125f),
        "Climb_Left" to AnimationDef(1, 2, .125f),
        "ClimbShoot" to AnimationDef(1, 1, .125f),
        "ClimbShoot_Left" to AnimationDef(),
        "ClimbHalfCharging" to AnimationDef(1, 2, .125f),
        "ClimbHalfCharging_Left" to AnimationDef(1, 2, .125f),
        "ClimbCharging" to AnimationDef(1, 2, .125f),
        "ClimbCharging_Left" to AnimationDef(1, 2, .125f),
        "FinishClimb" to AnimationDef(),
        "FinishClimb_Left" to AnimationDef(),
        "FinishClimbCharging" to AnimationDef(1, 2, .15f),
        "FinishClimbCharging_Left" to AnimationDef(1, 2, .15f),
        "FinishClimbHalfCharging" to AnimationDef(1, 2, .15f),
        "FinishClimbHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "StillClimb" to AnimationDef(),
        "StillClimb_Left" to AnimationDef(),
        "StillClimbCharging" to AnimationDef(1, 2, .15f),
        "StillClimbCharging_Left" to AnimationDef(1, 2, .15f),
        "StillClimbHalfCharging" to AnimationDef(1, 2, .15f),
        "StillClimbHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "Stand" to AnimationDef(1, 2, gdxArrayOf(1.5f, .15f)),
        "Stand_Left" to AnimationDef(1, 2, gdxArrayOf(1.5f, .15f)),
        "StandCharging" to AnimationDef(1, 2, .15f),
        "StandCharging_Left" to AnimationDef(1, 2, .15f),
        "StandHalfCharging" to AnimationDef(1, 2, .15f),
        "StandHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "StandShoot" to AnimationDef(),
        "StandShoot_Left" to AnimationDef(),
        "Damaged" to AnimationDef(1, 5, .05f),
        "Damaged_Left" to AnimationDef(1, 5, .05f),
        "Run" to AnimationDef(1, 4, .125f),
        "Run_Left" to AnimationDef(1, 4, .125f),
        "RunCharging" to AnimationDef(1, 4, .125f),
        "RunCharging_Left" to AnimationDef(1, 4, .125f),
        "RunHalfCharging" to AnimationDef(1, 4, .125f),
        "RunHalfCharging_Left" to AnimationDef(1, 4, .125f),
        "RunShoot" to AnimationDef(1, 4, .125f),
        "RunShoot_Left" to AnimationDef(1, 4, .125f),
        "Jump" to AnimationDef(),
        "Jump_Left" to AnimationDef(),
        "JumpCharging" to AnimationDef(1, 2, .15f),
        "JumpCharging_Left" to AnimationDef(1, 2, .15f),
        "JumpHalfCharging" to AnimationDef(1, 2, .15f),
        "JumpHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "JumpShoot" to AnimationDef(),
        "JumpShoot_Left" to AnimationDef(),
        "Swim" to AnimationDef(),
        "Swim_Left" to AnimationDef(),
        "SwimAttack" to AnimationDef(),
        "SwimAttack_Left" to AnimationDef(),
        "SwimCharging" to AnimationDef(1, 2, .15f),
        "SwimCharging_Left" to AnimationDef(1, 2, .15f),
        "SwimHalfCharging" to AnimationDef(1, 2, .15f),
        "SwimHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "SwimShoot" to AnimationDef(),
        "SwimShoot_Left" to AnimationDef(),
        "WallSlide" to AnimationDef(),
        "WallSlide_Left" to AnimationDef(),
        "WallSlideCharging" to AnimationDef(1, 2, .15f),
        "WallSlideCharging_Left" to AnimationDef(1, 2, .15f),
        "WallSlideHalfCharging" to AnimationDef(1, 2, .15f),
        "WallSlideHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "WallSlideShoot" to AnimationDef(),
        "WallSlideShoot_Left" to AnimationDef(),
        "GroundSlide" to AnimationDef(),
        "GroundSlide_Left" to AnimationDef(),
        "GroundSlideShoot" to AnimationDef(),
        "GroundSlideShoot_Left" to AnimationDef(),
        "GroundSlideCharging" to AnimationDef(1, 2, .15f),
        "GroundSlideCharging_Left" to AnimationDef(1, 2, .15f),
        "GroundSlideHalfCharging" to AnimationDef(1, 2, .15f),
        "GroundSlideHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "AirDash" to AnimationDef(),
        "AirDash_Left" to AnimationDef(),
        "AirDashCharging" to AnimationDef(1, 2, .15f),
        "AirDashCharging_Left" to AnimationDef(1, 2, .15f),
        "AirDashHalfCharging" to AnimationDef(1, 2, .15f),
        "AirDashHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "SlipSlide" to AnimationDef(),
        "SlipSlide_Left" to AnimationDef(),
        "SlipSlideCharging" to AnimationDef(1, 2, .15f),
        "SlipSlideCharging_Left" to AnimationDef(1, 2, .15f),
        "SlipSlideHalfCharging" to AnimationDef(1, 2, .15f),
        "SlipSlideHalfCharging_Left" to AnimationDef(1, 2, .15f),
        "SlipSlideShoot" to AnimationDef(),
        "SlipSlideShoot_Left" to AnimationDef(),
    )

private fun createAnimationMap(game: IGame2D): ObjectMap<String, IAnimation> {
  val map = objectMapOf<String, IAnimation>()

  MegamanWeapon.values().forEach { weapon ->
    val atlas = game.assMan.getTextureAtlas("Megaman_${weapon.name}")

    animationKeys.forEach { key ->
      val def = animationDefMap[key]

      map.put(
          "${weapon.name}_$key",
          Animation(atlas.findRegion("${weapon.name}_$key"), def.rows, def.cols, def.durations))
    }
  }
  return map
}
