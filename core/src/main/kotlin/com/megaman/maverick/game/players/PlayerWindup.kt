package com.megaman.maverick.game.players

import com.maverick.game.engine.components.require
import com.megaman.maverick.game.entities.state.WeaponWindupComponent

// TODO: widen the player type when an enemy clone calls this (item 121)
val MegaPlayer.windups: WeaponWindupComponent
    get() = require(WeaponWindupComponent::class)

fun MegaPlayer.stopWindup() = windups.stop()
