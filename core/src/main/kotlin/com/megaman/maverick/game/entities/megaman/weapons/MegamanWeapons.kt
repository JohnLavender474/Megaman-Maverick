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

    val MEGA_BUSTER: WeaponDef<Megaman> = WeaponDef(
        id = "mega_buster",
        cooldown = 0.1f,
        fire = ::fireMegaBuster,
        alwaysOwned = true,
        infiniteAmmo = true
    )

    val FRIGID_SHOT: WeaponDef<Megaman> = WeaponDef(
        id = "frigid_shot",
        cooldown = 0.1f,
        fire = ::fireFrigidShot,
        cost = { 1 }
    )

    val INFERNAL_BARRAGE: WeaponDef<Megaman> = WeaponDef(
        id = "infernal_barrage",
        cooldown = 0.5f,
        fire = ::fireInfernalBarrage,
        cost = { 3 }
    )

    val MOON_SCYTHES: WeaponDef<Megaman> = WeaponDef(
        id = "moon_scythes",
        cooldown = 0.1f,
        fire = ::fireMoonScythes,
        newState = { MoonScythesState(MOON_SCYTHES.cooldown) },
        cost = { 3 }
    )

    val PRECIOUS_GUARD: WeaponDef<Megaman> = WeaponDef(
        id = "precious_guard",
        cooldown = 0.1f,
        fire = ::firePreciousGuard,
        newState = { megaman -> PreciousGuardState(megaman, PRECIOUS_GUARD.cooldown) },
        cost = { (it as PreciousGuardState).cost },
        onDeselect = { _, state -> destroyPreciousGuardGems(state) }
    )

    val AXE_SWINGER: WeaponDef<Megaman> = WeaponDef(
        id = "axe_swinger",
        cooldown = 0.5f,
        fire = ::fireAxeSwinger,
        cost = { 1 },
        axeShield = true
    )

    val NEEDLE_SPIN: WeaponDef<Megaman> = WeaponDef(
        id = "needle_spin",
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
        spinFixturesWhileFiring = true
    )

    val RODENT_CLAWS: WeaponDef<Megaman> = WeaponDef(
        id = "rodent_claws",
        cooldown = 0.1f,
        fire = ::fireRodentClaws,
        newState = { RodentClawsState(RODENT_CLAWS.cooldown) },
        cost = { 2 },
        onDeselect = { megaman, _ ->
            if (megaman.isBehaviorActive(BehaviorType.WALL_SLIDING)) megaman.body.physics.gravityOn = true
        },
        stopsRunWhileFiring = true,
        gripsWall = true
    )

    val REACTOR_SHOT: WeaponDef<Megaman> = WeaponDef(
        id = "reactor_shot",
        cooldown = 0.5f,
        fire = ::fireReactorShot,
        cost = { 2 }
    )

    val RUSH_JET: WeaponDef<Megaman> = WeaponDef(
        id = "rush_jet",
        cooldown = 0.1f,
        fire = ::fireRushJet,
        airDash = WeaponAirDash(
            maxTime = MegamanValues.RUSH_JET_AIR_DASH_MAX_TIME,
            speedScalar = MegamanValues.RUSH_JET_AIR_DASH_SCALAR,
            endsWhen = { it.weaponsHandler.isDepleted(RUSH_JET) || it.input.isPressed(MegaControllerButton.UP) },
            onEnd = { it.jetpackHandler.reset() }
        ),
        enablesJetpack = true,
        stopsChargeOnHold = true
    )
}
