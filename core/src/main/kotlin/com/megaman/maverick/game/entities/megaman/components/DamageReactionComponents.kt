package com.megaman.maverick.game.entities.megaman.components

import com.maverick.game.engine.common.extensions.objectSetOf
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.enemies.SpringHead
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.input
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing

internal fun defineKnockbackComponent() = KnockbackComponent(
    MegamanValues.DMG_X * ConstVals.PPM,
    MegamanValues.DMG_Y * ConstVals.PPM,
    objectSetOf(SpringHead::class)
)

internal fun Megaman.defineFreezeComponent() = FreezeComponent(
    MashThawRule(
        MegamanValues.HITS_TO_UNFREEZE,
        MegamanValues.FROZEN_PUSH_DUR,
        mashed = { MegamanValues.BUTTONS_TO_UNFREEZE.any { input.isJustReleased(it) } },
        onMash = { requestToPlaySound(SoundAsset.ICE_SHARD_1_SOUND, false) }
    ),
    onThaw = { IceShard.spawn5(body.getCenter()) }
)

internal fun Megaman.defineStunComponent() = StunComponent(
    MegamanValues.STUN_DUR,
    onSurface = { body.isSensing(BodySense.FEET_ON_GROUND) || isBehaviorActive(BehaviorType.WALL_SLIDING) },
    onStun = {
        // TODO: This assumes that Megaman's body is rotated UP.
        //    Refactor this to support directionally dynamic bouncing.
        body.physics.velocity.let {
            it.x = MegamanValues.STUN_IMPULSE_X * ConstVals.PPM * movementScalar * -facing.value
            it.y = MegamanValues.STUN_IMPULSE_Y * ConstVals.PPM
        }
    }
)

internal fun defineDamageRecoveryComponent() =
    DamageRecoveryComponent(MegamanValues.DAMAGE_RECOVERY_TIME, MegamanValues.DAMAGE_FLASH_DURATION)
