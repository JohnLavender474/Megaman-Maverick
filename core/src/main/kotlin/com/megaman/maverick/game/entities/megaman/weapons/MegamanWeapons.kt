package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.equalsAny
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.drawables.sprites.containsRegion
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.sprites.MEGAMAN_ANIM_KEY_SUPPLIER_TAG
import com.megaman.maverick.game.weapons.TrailWhileFiring
import com.megaman.maverick.game.weapons.WeaponAirDash
import com.megaman.maverick.game.weapons.WeaponDef

object MegamanWeapons {

    val MEGA_BUSTER: WeaponDef = WeaponDef(
        id = "mega_buster",
        atlas = TextureAsset.MEGAMAN_BUSTER,
        cooldown = 0.1f,
        fire = ::fireMegaBuster,
        chargeable = true,
        alwaysOwned = true,
        infiniteAmmo = true,
        jumpDownChargeKeys = true
    )

    val FRIGID_SHOT: WeaponDef = WeaponDef(
        id = "frigid_shot",
        atlas = TextureAsset.MEGAMAN_ICE_CUBE,
        cooldown = 0.1f,
        fire = ::fireFrigidShot,
        cost = { 1 },
        hudBitRegion = ConstKeys.ICE
    )

    val INFERNAL_BARRAGE: WeaponDef = WeaponDef(
        id = "infernal_barrage",
        atlas = TextureAsset.MEGAMAN_MAGMA_WAVE,
        cooldown = 0.5f,
        fire = ::fireInfernalBarrage,
        cost = { 3 },
        hudBitRegion = ConstKeys.FIRE
    )

    val MOON_SCYTHES: WeaponDef = WeaponDef(
        id = "moon_scythes",
        atlas = TextureAsset.MEGAMAN_MOON_SCYTHE,
        cooldown = 0.1f,
        fire = ::fireMoonScythes,
        newState = { MoonScythesState(MOON_SCYTHES.cooldown) },
        cost = { 3 },
        hudBitRegion = ConstKeys.MOON
    )

    val PRECIOUS_GUARD: WeaponDef = WeaponDef(
        id = "precious_guard",
        atlas = TextureAsset.MEGAMAN_PRECIOUS_GUARD,
        cooldown = 0.1f,
        fire = ::firePreciousGuard,
        newState = { megaman -> PreciousGuardState(megaman, PRECIOUS_GUARD.cooldown) },
        cost = { (it as PreciousGuardState).cost },
        onDeselect = { _, state -> destroyPreciousGuardGems(state) },
        hudBitRegion = "precious"
    )

    val AXE_SWINGER: WeaponDef = WeaponDef(
        id = "axe_swinger",
        atlas = TextureAsset.MEGAMAN_AXE_THROW,
        cooldown = 0.5f,
        fire = ::fireAxeSwinger,
        cost = { 1 },
        axeShield = true,
        shootKey = { _, baseKey -> "${baseKey}_axe_throw" },
        keepsAnimTimeAcrossKeys = true,
        hudBitRegion = "axe"
    )

    val NEEDLE_SPIN: WeaponDef = WeaponDef(
        id = "needle_spin",
        atlas = TextureAsset.MEGAMAN_NEEDLE_SPIN,
        cooldown = 0.25f,
        fire = ::fireNeedleSpin,
        cost = { 2 },
        movesBlockedWhileFiring = objectSetOf(
            BehaviorType.AIR_DASHING,
            BehaviorType.CLIMBING,
            BehaviorType.CROUCHING,
            BehaviorType.GROUND_SLIDING,
            BehaviorType.JUMPING,
            BehaviorType.SWIMMING,
            BehaviorType.WALL_SLIDING
        ),
        spinFixturesWhileFiring = true,
        shootKey = { _, _ -> "needle_spin" },
        trailWhileFiring = TrailWhileFiring.NONE,
        showsTornadoWhileFiring = true,
        hudBitRegion = "needle"
    )

    val RODENT_CLAWS: WeaponDef = WeaponDef(
        id = "rodent_claws",
        atlas = TextureAsset.MEGAMAN_RATTY_CLAWS,
        cooldown = 0.1f,
        fire = ::fireRodentClaws,
        newState = { RodentClawsState(RODENT_CLAWS.cooldown) },
        cost = { 2 },
        onDeselect = { megaman, _ ->
            if (megaman.isBehaviorActive(BehaviorType.WALL_SLIDING)) megaman.body.physics.gravityOn = true
        },
        stopsRunWhileFiring = true,
        gripsWall = true,
        shootKey = ::rodentClawsShootKey,
        trailWhileFiring = TrailWhileFiring.PLAIN_KEY,
        restartsSameKeyOnPress = true,
        hudBitRegion = "rat"
    )

    val REACTOR_SHOT: WeaponDef = WeaponDef(
        id = "reactor_shot",
        atlas = TextureAsset.MEGAMAN_REACTOR_SHOT,
        cooldown = 0.5f,
        fire = ::fireReactorShot,
        cost = { 2 },
        jumpDownChargeKeys = true,
        hudBitRegion = "reactor"
    )

    val RUSH_JET: WeaponDef = WeaponDef(
        id = "rush_jet",
        atlas = TextureAsset.MEGAMAN_RUSH_JETPACK,
        cooldown = 0.1f,
        fire = ::fireRushJet,
        airDash = WeaponAirDash(
            maxTime = MegamanValues.RUSH_JET_AIR_DASH_MAX_TIME,
            speedScalar = MegamanValues.RUSH_JET_AIR_DASH_SCALAR,
            endsWhen = { it.weaponsHandler.isDepleted(RUSH_JET) || it.input.isPressed(MegaControllerButton.UP) },
            onEnd = { it.jetpackHandler.reset() }
        ),
        enablesJetpack = true,
        stopsChargeOnHold = true,
        airDashKey = "jetpack_airdash",
        hudBitRegion = "rush_jet"
    )

    val all: Array<WeaponDef> = gdxArrayOf(
        MEGA_BUSTER,
        FRIGID_SHOT,
        INFERNAL_BARRAGE,
        MOON_SCYTHES,
        PRECIOUS_GUARD,
        AXE_SWINGER,
        NEEDLE_SPIN,
        RODENT_CLAWS,
        REACTOR_SHOT,
        RUSH_JET
    )

    fun byId(id: String): WeaponDef? = all.firstOrNull { it.id.equals(id, ignoreCase = true) }

    private fun rodentClawsShootKey(megaman: Megaman, baseKey: String): String {
        val slashIndex = (megaman.weaponsHandler.stateOf(RODENT_CLAWS) as RodentClawsState).keySlashIndex
        GameLogger.debug(MEGAMAN_ANIM_KEY_SUPPLIER_TAG, "slashIndex=$slashIndex")

        val newBaseKey = if (baseKey.equalsAny("slip", "run")) "stand" else baseKey

        val atlas = megaman.services.assets.getTextureAtlas(RODENT_CLAWS.atlas.source)

        return if (atlas.containsRegion("${newBaseKey}_slash$slashIndex")) "${newBaseKey}_slash$slashIndex"
        else if (atlas.containsRegion("${newBaseKey}_slash1")) "${newBaseKey}_slash1"
        else "stand_slash1"
    }
}
