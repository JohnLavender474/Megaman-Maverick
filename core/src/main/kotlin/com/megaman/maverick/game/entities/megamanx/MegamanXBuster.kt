package com.megaman.maverick.game.entities.megamanx

import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.players.BusterOffset
import com.megaman.maverick.game.players.BusterParams
import com.megaman.maverick.game.players.BusterShot

private fun chargedShot(fully: Boolean): (Properties) -> ChargedShot = { props ->
    props.put(ConstKeys.BOOLEAN, fully)
    MegaEntityFactory.fetch(ChargedShot::class)!!
}

internal val MEGAMAN_X_BUSTER = BusterParams(
    notCharged = BusterShot(
        speed = MegamanXValues.SHOT_SPEED,
        wait = MegamanXValues.SHOT_WAIT,
        poseTime = MegamanXValues.SHOT_POSE_TIME,
        limit = MegamanXValues.SHOT_LIMIT,
        sound = SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND,
        fetch = { MegaEntityFactory.fetch(Bullet::class)!! }
    ),
    halfCharged = BusterShot(
        speed = MegamanXValues.HALF_SHOT_SPEED,
        wait = MegamanXValues.CHARGED_SHOT_WAIT,
        poseTime = MegamanXValues.HALF_SHOT_POSE_TIME,
        limit = MegamanXValues.SHOT_LIMIT,
        sound = SoundAsset.MEGA_BUSTER_CHARGED_SHOT_SOUND,
        fetch = chargedShot(false)
    ),
    fullyCharged = BusterShot(
        speed = MegamanXValues.FULL_SHOT_SPEED,
        wait = MegamanXValues.CHARGED_SHOT_WAIT,
        poseTime = MegamanXValues.FULL_SHOT_POSE_TIME,
        limit = MegamanXValues.FULL_SHOT_LIMIT,
        sound = SoundAsset.MEGA_BUSTER_CHARGED_SHOT_SOUND,
        fetch = chargedShot(true)
    ),
    offsetX = MegamanXValues.SHOT_OFFSET_X,
    offsetY = MegamanXValues.SHOT_OFFSET_Y,
    runOffsetX = 0.5f,
    airOffsetY = 0.1f,
    // TODO: a crouched shot skips the air offset, which the shot took before when the feet sensor missed the ground
    //  during a crouch (060c process log, 2026-11-14)
    crouchOffset = BusterOffset(
        MegamanXValues.SHOT_OFFSET_X + MegamanXValues.SHOT_OFFSET_X_CROUCH,
        MegamanXValues.SHOT_OFFSET_Y
    ),
    climbOffset = BusterOffset(MegamanXValues.SHOT_OFFSET_X, MegamanXValues.SHOT_OFFSET_Y_CLIMB),
    slideOffset = null,
    wallSlideOffset = null,
    airDashOffset = null,
    swimOffset = null
)
