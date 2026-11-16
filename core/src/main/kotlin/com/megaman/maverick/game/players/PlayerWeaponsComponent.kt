package com.megaman.maverick.game.players

import com.maverick.game.engine.components.IGameComponent
import com.maverick.game.engine.entities.IGameEntity
import com.megaman.maverick.game.weapons.WeaponDef
import com.megaman.maverick.game.weapons.WeaponLook

class PlayerWeaponsComponent<O : MegaPlayer>(
    val loadout: CharacterLoadout<O>,
    private val currentWeaponSupplier: () -> WeaponDef<O>,
    private val selectWeapon: (WeaponDef<O>) -> Unit,
    private val ammoOf: (WeaponDef<O>) -> Int,
    private val translateAmmoOf: (WeaponDef<O>, Int) -> Int
) : IGameComponent {

    val currentWeapon: WeaponDef<O>
        get() = currentWeaponSupplier()

    val currentLook: WeaponLook<O>
        get() = loadout.lookOf(currentWeapon)

    fun select(weapon: WeaponDef<*>) = selectWeapon(loadout.find(weapon))

    fun selectFirstWeapon() = selectWeapon(loadout.weapons.first())

    fun ammo(weapon: WeaponDef<*>) = ammoOf(loadout.find(weapon))

    fun translateAmmo(weapon: WeaponDef<*>, delta: Int) = translateAmmoOf(loadout.find(weapon), delta)
}

val IGameEntity.weapons: PlayerWeaponsComponent<*>?
    get() = getComponent(PlayerWeaponsComponent::class)
