package com.megaman.maverick.game.weapons

import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.players.MegaPlayer

enum class TrailWhileFiring { SHOOT_KEY, PLAIN_KEY, NONE }

class WeaponLook<in O : MegaPlayer>(
    val atlas: TextureAsset,
    val pauseRegion: String,
    val getWeaponRegion: String,
    val shootKey: ((O, String) -> String)? = null,
    val airDashKey: String = "airdash",
    val jumpDownChargeKeys: Boolean = false,
    val trailWhileFiring: TrailWhileFiring = TrailWhileFiring.SHOOT_KEY,
    val showsTornadoWhileFiring: Boolean = false,
    val keepsAnimTimeAcrossKeys: Boolean = false,
    val restartsSameKeyOnPress: Boolean = false,
    val hudBitRegion: String? = null
)
