package com.megaman.maverick.game.players

import com.maverick.game.engine.components.require
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.IPlayer
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.state.SessionRules

// TODO: stun, freeze and damage can become MOVEMENT lock reasons, so that this rule reads only the lock (item 126)
val MegaPlayer.playerCanMove: Boolean
    get() {
        val movementLocks = require(LocksComponent::class).getLockGroup(MegaLockGroup.MOVEMENT)
        val stun = require(StunComponent::class)
        val freeze = require(FreezeComponent::class)
        return !movementLocks.isLocked() && !stun.stunned && !damaged && !freeze.frozen
    }

// TODO: the owner chain of rootPlayer goes 2 levels deep (item 030)
fun IPlayer.playerAllowsDamageFrom(damager: IDamagerEntity, rules: SessionRules): Boolean {
    if (!existenceState.liveAfterUpdate) return false
    val source = damager.rootPlayer ?: return true
    return source != this && rules.friendlyFire
}
