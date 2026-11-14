package com.megaman.maverick.game.entities.protoman

import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.projectiles.ProtoBuster
import com.megaman.maverick.game.players.BusterOffset
import com.megaman.maverick.game.players.BusterParams
import com.megaman.maverick.game.players.BusterShot

// TODO: Megaman's shot sound until Proto Man has his own (item 060g)
internal val PROTOMAN_BUSTER = BusterParams(
    notCharged = BusterShot(
        speed = ProtomanValues.SHOT_SPEED,
        wait = ProtomanValues.SHOT_WAIT,
        poseTime = ProtomanValues.SHOT_POSE_TIME,
        limit = null,
        sound = SoundAsset.MEGA_BUSTER_BULLET_SHOT_SOUND,
        fetch = { MegaEntityFactory.fetch(ProtoBuster::class)!! }
    ),
    halfCharged = null,
    fullyCharged = null,
    offsetX = ProtomanValues.SHOT_OFFSET_X,
    offsetY = ProtomanValues.SHOT_OFFSET_Y,
    runOffsetX = ProtomanValues.SHOT_RUN_OFFSET_X,
    airOffsetY = ProtomanValues.SHOT_AIR_OFFSET_Y,
    crouchOffset = BusterOffset(ProtomanValues.SHOT_CROUCH_OFFSET_X, ProtomanValues.SHOT_CROUCH_OFFSET_Y),
    climbOffset = null
)
