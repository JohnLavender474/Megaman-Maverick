package com.megaman.maverick.game.players

import com.maverick.game.engine.components.require
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.state.WeaponChargeComponent

// TODO: widen the player type when an enemy clone calls this (item 121)
fun playerChargeComponent(player: MegaPlayer, halfTime: Float, fullTime: Float) =
    WeaponChargeComponent(halfTime, fullTime) {
        player.requestToPlaySound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND, loop = false)
    }

val MegaPlayer.playerChargeStatus: MegaChargeStatus
    get() {
        val charge = require(WeaponChargeComponent::class)
        return when {
            charge.full -> MegaChargeStatus.FULLY_CHARGED
            charge.half -> MegaChargeStatus.HALF_CHARGED
            else -> MegaChargeStatus.NOT_CHARGED
        }
    }

fun MegaPlayer.stopCharging() {
    stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND)
    require(WeaponChargeComponent::class).reset()
}
