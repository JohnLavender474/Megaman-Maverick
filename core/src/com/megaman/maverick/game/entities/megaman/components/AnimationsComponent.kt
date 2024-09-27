package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.*
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.containsRegion
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing
import kotlin.math.abs

const val MEGAMAN_ANIMATIONS_COMPONENT_TAG = "MegamanAnimationsComponent"

lateinit var animations: ObjectMap<String, IAnimation>

val Megaman.slipSliding: Boolean
    get() = body.isSensing(BodySense.FEET_ON_GROUND) && abs(
        if (isDirectionRotatedVertically()) body.physics.velocity.x else body.physics.velocity.y
    ) > ConstVals.PPM / 16f

val Megaman.rawAnimKey: String
    get() = currentRawAnimKey

val Megaman.amendedAnimKey: String
    get() = currentAmendedAnimKey

private lateinit var currentRawAnimKey: String
private lateinit var currentAmendedAnimKey: String

internal fun Megaman.defineAnimationsComponent(): AnimationsComponent {
    val megamanAnimationKeySupplier = {
        val newAnimKey =
            if (!roomTransPauseTimer.isFinished()) ConstKeys.INVALID
            else if (game.isProperty(ConstKeys.ROOM_TRANSITION, true)) currentRawAnimKey
            else if (!ready) "Stand"
            else if (cameraRotating) {
                if (shooting) "JumpShoot"
                else if (fullyCharged) "JumpCharging" else if (halfCharged) "JumpHalfCharging" else "Jump"
            } else {
                if (isBehaviorActive(BehaviorType.JETPACKING)) {
                    if (shooting) "JetpackShoot"
                    else "Jetpack"
                } else if (isBehaviorActive(BehaviorType.RIDING_CART)) {
                    if (damaged) "Cartin_Damaged"
                    else if (isBehaviorActive(BehaviorType.JUMPING) || !body.isSensing(BodySense.FEET_ON_GROUND)) {
                        if (shooting) "Cartin_JumpShoot"
                        else if (fullyCharged) "Cartin_JumpFullyCharged"
                        else if (halfCharged) "Cartin_JumpHalfCharged"
                        else "Cartin_Jump"
                    } else {
                        if (shooting) "Cartin_Shoot"
                        else if (fullyCharged) "Cartin_FullyCharged"
                        else if (halfCharged) "Cartin_HalfCharged"
                        else "Cartin"
                    }
                } else if (damaged || stunned) "Damaged"
                else if (isBehaviorActive(BehaviorType.CLIMBING)) {
                    if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) {
                        if (shooting) "ClimbShoot"
                        else if (fullyCharged) "FinishClimbCharging"
                        else if (halfCharged) "FinishClimbHalfCharging" else "FinishClimb"
                    } else {
                        val movement =
                            if (isDirectionRotatedHorizontally()) body.physics.velocity.x else body.physics.velocity.y
                        if (movement != 0f) {
                            if (shooting) "ClimbShoot"
                            else if (fullyCharged) "ClimbCharging"
                            else if (halfCharged) "ClimbHalfCharging" else "Climb"
                        } else {
                            if (shooting) "ClimbShoot"
                            else if (fullyCharged) "StillClimbCharging"
                            else if (halfCharged) "StillClimbHalfCharging" else "StillClimb"
                        }
                    }
                } else if (isBehaviorActive(BehaviorType.AIR_DASHING)) {
                    if (fullyCharged) "AirDashCharging"
                    else if (halfCharged) "AirDashHalfCharging" else "AirDash"
                } else if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
                    if (shooting) "GroundSlideShoot"
                    else if (fullyCharged) "GroundSlideCharging"
                    else if (halfCharged) "GroundSlideHalfCharging" else "GroundSlide"
                } else if (isBehaviorActive(BehaviorType.WALL_SLIDING)) {
                    if (shooting) "WallSlideShoot"
                    else if (fullyCharged) "WallSlideCharging"
                    else if (halfCharged) "WallSlideHalfCharging" else "WallSlide"
                } else if (isBehaviorActive(BehaviorType.SWIMMING)) {
                    if (shooting) "SwimShoot"
                    else if (fullyCharged) "SwimCharging" else if (halfCharged) "SwimHalfCharging" else "Swim"
                } else if (body.isSensing(BodySense.FEET_ON_GROUND) && running) {
                    if (shooting) "RunShoot"
                    else if (fullyCharged) "RunCharging" else if (halfCharged) "RunHalfCharging" else "Run"
                } else if (isBehaviorActive(BehaviorType.JUMPING) || !body.isSensing(BodySense.FEET_ON_GROUND)
                /*
                (!body.isSensing(BodySense.FEET_ON_GROUND) && abs(
                    if (directionRotation!!.isVertical()) body.physics.velocity.y
                    else body.physics.velocity.x
                ) > 0.1f * ConstVals.PPM)
                 */
                ) {
                    if (shooting) "JumpShoot"
                    else if (fullyCharged) "JumpCharging" else if (halfCharged) "JumpHalfCharging" else "Jump"
                } else if (slipSliding) {
                    if (shooting) "SlipSlideShoot"
                    else if (fullyCharged) "SlipSlideCharging"
                    else if (halfCharged) "SlipSlideHalfCharging" else "SlipSlide"
                } else {
                    if (shooting) "StandShoot"
                    else if (fullyCharged) "StandCharging"
                    else if (halfCharged) "StandHalfCharging" else "Stand"
                }
            }
        if (newAnimKey != ConstKeys.INVALID) currentRawAnimKey = newAnimKey
        currentAmendedAnimKey = newAnimKey
        if (maverick && facing == Facing.LEFT) currentAmendedAnimKey += "_Left"
        currentAmendedAnimKey += if (maverick) "_MegamanMaverick" else "_Megaman"
        currentAmendedAnimKey += "_${currentWeapon.name}"
        currentAmendedAnimKey
    }

    animations = ObjectMap<String, IAnimation>()

    // TODO: create missing maverick animations
    gdxArrayOf("Megaman" /*, "MegamanMaverick"*/).forEach { megamanType ->
        for (weapon in MegamanWeapon.values()) {
            val assetSource = if (megamanType == "Megaman") when (weapon) {
                MegamanWeapon.BUSTER -> TextureAsset.MEGAMAN_BUSTER.source
                MegamanWeapon.RUSH_JETPACK -> TextureAsset.MEGAMAN_RUSH_JETPACK.source
                // TODO: MegamanWeapon.FLAME_TOSS -> "" // TODO: TextureAsset.MEGAMAN_FLAME_TOSS.source
            }
            else when (weapon) {
                MegamanWeapon.BUSTER -> TextureAsset.MEGAMAN_MAVERICK_BUSTER.source
                MegamanWeapon.RUSH_JETPACK -> "" // TODO: TextureAsset.MEGAMAN_MAVERICK_RUSH_JETPACK.source
                // TODO: MegamanWeapon.FLAME_TOSS -> "" // TODO: create texture atlas
            }
            if (assetSource == "") continue
            val atlas = game.assMan.getTextureAtlas(assetSource)

            for (animationKey in animationKeys) {
                if (!atlas.containsRegion(animationKey) ||
                    (megamanType == "Megaman" && animationKey.contains("Left"))
                ) continue

                val def = animationDefMap[animationKey]

                var _animationKey = animationKey
                _animationKey += "_${megamanType}"
                _animationKey += "_${weapon.name}"

                GameLogger.debug(
                    MEGAMAN_ANIMATIONS_COMPONENT_TAG,
                    "defineAnimationsComponent(): Putting animation \'${animationKey}\' with key \'${_animationKey}\'"
                )

                animations.put(
                    _animationKey, Animation(atlas.findRegion(animationKey), def.rows, def.cols, def.durations)
                )
            }
        }
    }

    val megamanAnimator = Animator(megamanAnimationKeySupplier, animations)

    val jetpackFlameRegion = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, "JetpackFlame")
    val jetpackFlameAnimation = Animation(jetpackFlameRegion, 1, 3, 0.1f, true)
    val jetpackFlameAnimator = Animator(jetpackFlameAnimation)

    val animatorSpritePairs = gdxArrayOf<GamePair<() -> GameSprite, IAnimator>>(
        { sprites.get("megaman") } pairTo megamanAnimator,
        { sprites.get("jetpackFlame") } pairTo jetpackFlameAnimator
    )

    return AnimationsComponent(animatorSpritePairs)
}

private val animationKeys = gdxArrayOf(
    "Cartin",
    "Cartin_Shoot",
    "Cartin_FullyCharged",
    "Cartin_HalfCharged",
    "Cartin_Damaged",
    "Cartin_Jump",
    "Cartin_JumpShoot",
    "Cartin_JumpHalfCharged",
    "Cartin_JumpFullyCharged",
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
    "Jetpack",
    "JetpackShoot"
)

private val animationDefMap = objectMapOf(
    "Jetpack" pairTo AnimationDef(1, 2, 0.1f),
    "JetpackShoot" pairTo AnimationDef(1, 2, 0.1f),
    "Cartin" pairTo AnimationDef(1, 2, gdxArrayOf(1.5f, 0.15f)),
    "Cartin_Damaged" pairTo AnimationDef(1, 3, 0.05f),
    "Cartin_FullyCharged" pairTo AnimationDef(1, 2, .125f),
    "Cartin_HalfCharged" pairTo AnimationDef(1, 2, .125f),
    "Cartin_Shoot" pairTo AnimationDef(),
    "Cartin_Jump" pairTo AnimationDef(),
    "Cartin_JumpHalfCharged" pairTo AnimationDef(1, 2, .125f),
    "Cartin_JumpFullyCharged" pairTo AnimationDef(1, 2, .125f),
    "Cartin_JumpShoot" pairTo AnimationDef(),
    "Climb" pairTo AnimationDef(1, 2, .125f),
    "Climb_Left" pairTo AnimationDef(1, 2, .125f),
    "ClimbShoot" pairTo AnimationDef(1, 1, .125f),
    "ClimbShoot_Left" pairTo AnimationDef(),
    "ClimbHalfCharging" pairTo AnimationDef(1, 2, .125f),
    "ClimbHalfCharging_Left" pairTo AnimationDef(1, 2, .125f),
    "ClimbCharging" pairTo AnimationDef(1, 2, .125f),
    "ClimbCharging_Left" pairTo AnimationDef(1, 2, .125f),
    "FinishClimb" pairTo AnimationDef(),
    "FinishClimb_Left" pairTo AnimationDef(),
    "FinishClimbCharging" pairTo AnimationDef(1, 2, .15f),
    "FinishClimbCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "FinishClimbHalfCharging" pairTo AnimationDef(1, 2, .15f),
    "FinishClimbHalfCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "StillClimb" pairTo AnimationDef(),
    "StillClimb_Left" pairTo AnimationDef(),
    "StillClimbCharging" pairTo AnimationDef(1, 2, .15f),
    "StillClimbCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "StillClimbHalfCharging" pairTo AnimationDef(1, 2, .15f),
    "StillClimbHalfCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "Stand" pairTo AnimationDef(1, 2, gdxArrayOf(1.5f, .15f)),
    "Stand_Left" pairTo AnimationDef(1, 2, gdxArrayOf(1.5f, .15f)),
    "StandCharging" pairTo AnimationDef(1, 2, .15f),
    "StandCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "StandHalfCharging" pairTo AnimationDef(1, 2, .15f),
    "StandHalfCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "StandShoot" pairTo AnimationDef(),
    "StandShoot_Left" pairTo AnimationDef(),
    "Damaged" pairTo AnimationDef(1, 5, .05f),
    "Damaged_Left" pairTo AnimationDef(1, 5, .05f),
    "Run" pairTo AnimationDef(1, 4, .125f),
    "Run_Left" pairTo AnimationDef(1, 4, .125f),
    "RunCharging" pairTo AnimationDef(1, 4, .125f),
    "RunCharging_Left" pairTo AnimationDef(1, 4, .125f),
    "RunHalfCharging" pairTo AnimationDef(1, 4, .125f),
    "RunHalfCharging_Left" pairTo AnimationDef(1, 4, .125f),
    "RunShoot" pairTo AnimationDef(1, 4, .125f),
    "RunShoot_Left" pairTo AnimationDef(1, 4, .125f),
    "Jump" pairTo AnimationDef(),
    "Jump_Left" pairTo AnimationDef(),
    "JumpCharging" pairTo AnimationDef(1, 2, .15f),
    "JumpCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "JumpHalfCharging" pairTo AnimationDef(1, 2, .15f),
    "JumpHalfCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "JumpShoot" pairTo AnimationDef(),
    "JumpShoot_Left" pairTo AnimationDef(),
    "Swim" pairTo AnimationDef(),
    "Swim_Left" pairTo AnimationDef(),
    "SwimAttack" pairTo AnimationDef(),
    "SwimAttack_Left" pairTo AnimationDef(),
    "SwimCharging" pairTo AnimationDef(1, 2, .15f),
    "SwimCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "SwimHalfCharging" pairTo AnimationDef(1, 2, .15f),
    "SwimHalfCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "SwimShoot" pairTo AnimationDef(),
    "SwimShoot_Left" pairTo AnimationDef(),
    "WallSlide" pairTo AnimationDef(),
    "WallSlide_Left" pairTo AnimationDef(),
    "WallSlideCharging" pairTo AnimationDef(1, 2, .15f),
    "WallSlideCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "WallSlideHalfCharging" pairTo AnimationDef(1, 2, .15f),
    "WallSlideHalfCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "WallSlideShoot" pairTo AnimationDef(),
    "WallSlideShoot_Left" pairTo AnimationDef(),
    "GroundSlide" pairTo AnimationDef(),
    "GroundSlide_Left" pairTo AnimationDef(),
    "GroundSlideShoot" pairTo AnimationDef(),
    "GroundSlideShoot_Left" pairTo AnimationDef(),
    "GroundSlideCharging" pairTo AnimationDef(1, 2, .15f),
    "GroundSlideCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "GroundSlideHalfCharging" pairTo AnimationDef(1, 2, .15f),
    "GroundSlideHalfCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "AirDash" pairTo AnimationDef(),
    "AirDash_Left" pairTo AnimationDef(),
    "AirDashCharging" pairTo AnimationDef(1, 2, .15f),
    "AirDashCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "AirDashHalfCharging" pairTo AnimationDef(1, 2, .15f),
    "AirDashHalfCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "SlipSlide" pairTo AnimationDef(),
    "SlipSlide_Left" pairTo AnimationDef(),
    "SlipSlideCharging" pairTo AnimationDef(1, 2, .15f),
    "SlipSlideCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "SlipSlideHalfCharging" pairTo AnimationDef(1, 2, .15f),
    "SlipSlideHalfCharging_Left" pairTo AnimationDef(1, 2, .15f),
    "SlipSlideShoot" pairTo AnimationDef(),
    "SlipSlideShoot_Left" pairTo AnimationDef(),
)
