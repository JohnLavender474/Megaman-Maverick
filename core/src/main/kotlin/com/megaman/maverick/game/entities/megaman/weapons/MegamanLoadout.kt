package com.megaman.maverick.game.entities.megaman.weapons

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.equalsAny
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.drawables.sprites.containsRegion
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.sprites.MEGAMAN_ANIM_KEY_SUPPLIER_TAG
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.players.CharacterLoadout
import com.megaman.maverick.game.players.PlayerCharacter
import com.megaman.maverick.game.weapons.TrailWhileFiring
import com.megaman.maverick.game.weapons.WeaponDef
import com.megaman.maverick.game.weapons.WeaponLook

private const val PAUSE_REGION_PREFIX = "weapons"
private const val GET_WEAPON_REGION_PREFIX = "megaman"

private fun weaponWithLook(
    weapon: WeaponDef<Megaman>,
    atlas: TextureAsset,
    shootKey: ((Megaman, String) -> String)? = null,
    airDashKey: String = "airdash",
    jumpDownChargeKeys: Boolean = false,
    trailWhileFiring: TrailWhileFiring = TrailWhileFiring.SHOOT_KEY,
    showsTornadoWhileFiring: Boolean = false,
    keepsAnimTimeAcrossKeys: Boolean = false,
    restartsSameKeyOnPress: Boolean = false,
    hudBitRegion: String? = null
) = weapon pairTo WeaponLook(
    atlas = atlas,
    pauseRegion = "$PAUSE_REGION_PREFIX/${weapon.id}",
    getWeaponRegion = "$GET_WEAPON_REGION_PREFIX/${weapon.id}",
    shootKey = shootKey,
    airDashKey = airDashKey,
    jumpDownChargeKeys = jumpDownChargeKeys,
    trailWhileFiring = trailWhileFiring,
    showsTornadoWhileFiring = showsTornadoWhileFiring,
    keepsAnimTimeAcrossKeys = keepsAnimTimeAcrossKeys,
    restartsSameKeyOnPress = restartsSameKeyOnPress,
    hudBitRegion = hudBitRegion
)

private fun rodentClawsShootKey(megaman: Megaman, baseKey: String): String {
    val slashIndex = (megaman.weaponsHandler.stateOf(MegamanWeapons.RODENT_CLAWS) as RodentClawsState).keySlashIndex
    GameLogger.debug(MEGAMAN_ANIM_KEY_SUPPLIER_TAG, "slashIndex=$slashIndex")

    val newBaseKey = if (baseKey.equalsAny("slip", "run")) "stand" else baseKey

    val clawsAtlas = MEGAMAN_LOADOUT.lookOf(MegamanWeapons.RODENT_CLAWS).atlas
    val atlas = megaman.services.assets.getTextureAtlas(clawsAtlas.source)

    return if (atlas.containsRegion("${newBaseKey}_slash$slashIndex")) "${newBaseKey}_slash$slashIndex"
    else if (atlas.containsRegion("${newBaseKey}_slash1")) "${newBaseKey}_slash1"
    else "stand_slash1"
}

val MEGAMAN_LOADOUT: CharacterLoadout<Megaman> = CharacterLoadout(
    character = PlayerCharacter.MEGAMAN,
    looksInSelectOrder = orderedMapOf(
        weaponWithLook(MegamanWeapons.MEGA_BUSTER, TextureAsset.MEGAMAN_BUSTER, jumpDownChargeKeys = true),
        weaponWithLook(MegamanWeapons.FRIGID_SHOT, TextureAsset.MEGAMAN_ICE_CUBE, hudBitRegion = ConstKeys.ICE),
        weaponWithLook(MegamanWeapons.INFERNAL_BARRAGE, TextureAsset.MEGAMAN_MAGMA_WAVE, hudBitRegion = ConstKeys.FIRE),
        weaponWithLook(MegamanWeapons.MOON_SCYTHES, TextureAsset.MEGAMAN_MOON_SCYTHE, hudBitRegion = ConstKeys.MOON),
        weaponWithLook(MegamanWeapons.PRECIOUS_GUARD, TextureAsset.MEGAMAN_PRECIOUS_GUARD, hudBitRegion = "precious"),
        weaponWithLook(
            MegamanWeapons.AXE_SWINGER,
            TextureAsset.MEGAMAN_AXE_THROW,
            shootKey = { _, baseKey -> "${baseKey}_axe_throw" },
            keepsAnimTimeAcrossKeys = true,
            hudBitRegion = "axe"
        ),
        weaponWithLook(
            MegamanWeapons.NEEDLE_SPIN,
            TextureAsset.MEGAMAN_NEEDLE_SPIN,
            shootKey = { _, _ -> "needle_spin" },
            trailWhileFiring = TrailWhileFiring.NONE,
            showsTornadoWhileFiring = true,
            hudBitRegion = "needle"
        ),
        weaponWithLook(
            MegamanWeapons.RODENT_CLAWS,
            TextureAsset.MEGAMAN_RATTY_CLAWS,
            shootKey = ::rodentClawsShootKey,
            trailWhileFiring = TrailWhileFiring.PLAIN_KEY,
            restartsSameKeyOnPress = true,
            hudBitRegion = "rat"
        ),
        weaponWithLook(
            MegamanWeapons.REACTOR_SHOT,
            TextureAsset.MEGAMAN_REACTOR_SHOT,
            jumpDownChargeKeys = true,
            hudBitRegion = "reactor"
        ),
        weaponWithLook(
            MegamanWeapons.RUSH_JET,
            TextureAsset.MEGAMAN_RUSH_JETPACK,
            airDashKey = "jetpack_airdash",
            hudBitRegion = "rush_jet"
        )
    ),
    rewards = objectMapOf(
        LevelDefinition.MOON_MAN pairTo gdxArrayOf(MegamanWeapons.MOON_SCYTHES, MegamanWeapons.RUSH_JET),
        LevelDefinition.PRECIOUS_WOMAN pairTo gdxArrayOf(MegamanWeapons.PRECIOUS_GUARD),
        LevelDefinition.INFERNO_MAN pairTo gdxArrayOf(MegamanWeapons.INFERNAL_BARRAGE),
        LevelDefinition.GLACIER_MAN pairTo gdxArrayOf(MegamanWeapons.FRIGID_SHOT),
        LevelDefinition.TIMBER_WOMAN pairTo gdxArrayOf(MegamanWeapons.AXE_SWINGER),
        LevelDefinition.DESERT_MAN pairTo gdxArrayOf(MegamanWeapons.NEEDLE_SPIN),
        LevelDefinition.RODENT_MAN pairTo gdxArrayOf(MegamanWeapons.RODENT_CLAWS),
        LevelDefinition.REACTOR_MAN pairTo gdxArrayOf(MegamanWeapons.REACTOR_SHOT)
    )
)

internal val Megaman.currentLook: WeaponLook<Megaman>
    get() = MEGAMAN_LOADOUT.lookOf(currentWeapon)
