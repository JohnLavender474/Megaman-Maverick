package com.megaman.maverick.game.players

import com.maverick.game.engine.components.require
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.locks.LocksComponent
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.state.*
import com.megaman.maverick.game.locks.MegaLockGroup

// TODO: the receiver is AbstractHealthEntity only for `damaged`; it changes when item 048 replaces the base class
val AbstractHealthEntity.playerInvincible: Boolean
    get() {
        val recovery = require(DamageRecoveryComponent::class)
        val damageLocks = require(LocksComponent::class).getLockGroup(MegaLockGroup.DAMAGE)
        val playPhase = require(PlayPhaseComponent::class)
        return damaged || recovery.recovering || damageLocks.isLocked() || !existenceState.liveAfterUpdate ||
            playPhase.phase != PlayPhase.IN_PLAY
    }

// TODO: stun, freeze and damage can become MOVEMENT lock reasons, so that this rule reads only the lock (item 126)
val AbstractHealthEntity.playerCanMove: Boolean
    get() {
        val movementLocks = require(LocksComponent::class).getLockGroup(MegaLockGroup.MOVEMENT)
        val stun = require(StunComponent::class)
        val freeze = require(FreezeComponent::class)
        return !movementLocks.isLocked() && !stun.stunned && !damaged && !freeze.frozen
    }

// TODO: the owner chain of rootPlayer goes 2 levels deep (item 030)
fun MegaGameEntity.playerAllowsDamageFrom(damager: IDamagerEntity): Boolean {
    if (!existenceState.liveAfterUpdate) return false
    val source = damager.rootPlayer ?: return true
    return source != this && services.rules.friendlyFire
}

fun <T> T.playerAfterDamage(damager: IDamagerEntity) where T : MegaGameEntity, T : IBodyEntity {
    knockBackFrom(damager)
    freezeFrom(damager)
    players.onPlayerDamaged(asPlayer!!, damager)
}

// TODO: the difficulty and the enhancement change the damage here; item 023 moves these rules
fun MegaGameEntity.playerEditDamage(baseDamage: Int): Int {
    var damage = when {
        services.gameState.containsEnhancement(MegaEnhancement.DAMAGE_INCREASE) -> MegaEnhancement.scaleDamage(
            baseDamage, MegaEnhancement.MEGAMAN_DAMAGE_INCREASE_SCALAR
        )
        else -> baseDamage
    }
    if (services.gameState.getDifficultyMode() == DifficultyMode.HARD) damage += 1
    return damage
}
