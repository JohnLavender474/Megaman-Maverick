package com.megaman.maverick.game.weapons

import com.badlogic.gdx.utils.ObjectSet
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus

enum class TrailWhileFiring { SHOOT_KEY, PLAIN_KEY, NONE }

// TODO: widen Megaman to the loadout owner when a second character uses a WeaponDef (item 024b)
class WeaponAirDash(
    val maxTime: Float,
    val speedScalar: Float,
    val endsWhen: (Megaman) -> Boolean,
    val onEnd: (Megaman) -> Unit
)

// TODO: widen Megaman to the loadout owner when a second character uses a WeaponDef (item 024b)
class WeaponDef(
    val id: String,
    val atlas: TextureAsset,
    val cooldown: Float,
    val fire: (Megaman, WeaponState, MegaChargeStatus) -> Unit,
    val newState: (Megaman) -> WeaponState = { WeaponState(cooldown) },
    val cost: (WeaponState) -> Int = { 0 },
    val chargeable: Boolean = false,
    val alwaysOwned: Boolean = false,
    val infiniteAmmo: Boolean = false,
    val onDeselect: (Megaman, WeaponState) -> Unit = { _, _ -> },
    val movesBlockedWhileFiring: ObjectSet<BehaviorType> = ObjectSet(),
    val airDash: WeaponAirDash? = null,
    val enablesJetpack: Boolean = false,
    val stopsChargeOnHold: Boolean = false,
    val stopsRunWhileFiring: Boolean = false,
    val gripsWall: Boolean = false,
    val spinFixturesWhileFiring: Boolean = false,
    val axeShield: Boolean = false,
    val shootKey: ((Megaman, String) -> String)? = null,
    val airDashKey: String = "airdash",
    val jumpDownChargeKeys: Boolean = false,
    val trailWhileFiring: TrailWhileFiring = TrailWhileFiring.SHOOT_KEY,
    val showsTornadoWhileFiring: Boolean = false,
    val keepsAnimTimeAcrossKeys: Boolean = false,
    val restartsSameKeyOnPress: Boolean = false,
    val hudBitRegion: String? = null
) {

    val displayName = id.uppercase().replace("_", " ")

    override fun toString() = id
}
