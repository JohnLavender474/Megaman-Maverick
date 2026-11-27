package com.megaman.maverick.game.weapons

import com.badlogic.gdx.utils.ObjectSet
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.players.MegaPlayer

class WeaponAirDash<in O : MegaPlayer>(
    val maxTime: Float,
    val speedScalar: Float,
    val endsWhen: (O) -> Boolean,
    val onEnd: (O) -> Unit
)

class WeaponDef<in O : MegaPlayer>(
    val id: String,
    val cooldown: Float,
    val fire: (O, WeaponState, MegaChargeStatus) -> Unit,
    val newState: (O) -> WeaponState = { WeaponState(cooldown) },
    val cost: (WeaponState) -> Int = { 0 },
    val chargeable: Boolean = false,
    val alwaysOwned: Boolean = false,
    val infiniteAmmo: Boolean = false,
    val onDeselect: (O, WeaponState) -> Unit = { _, _ -> },
    movesBlockedWhileFiring: ObjectSet<BehaviorType> = ObjectSet(),
    val airDash: WeaponAirDash<O>? = null,
    val enablesJetpack: Boolean = false,
    val summonsRush: Boolean = false,
    val stopsChargeOnHold: Boolean = false,
    val stopsRunWhileFiring: Boolean = false,
    val gripsWall: Boolean = false,
    val spinFixturesWhileFiring: Boolean = false,
    val axeShield: Boolean = false
) {

    private val movesBlockedWhileFiring = ObjectSet(movesBlockedWhileFiring)

    val displayName = id.uppercase().replace("_", " ")

    fun preventsWhileFiring(move: BehaviorType) = movesBlockedWhileFiring.contains(move)

    override fun toString() = id
}
