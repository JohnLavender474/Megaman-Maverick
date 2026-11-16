package com.megaman.maverick.game.players

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.megaman.maverick.game.entities.megaman.weapons.MEGAMAN_LOADOUT
import com.megaman.maverick.game.entities.megamanx.MEGAMAN_X_LOADOUT
import com.megaman.maverick.game.entities.protoman.PROTOMAN_LOADOUT
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.weapons.WeaponDef
import com.megaman.maverick.game.weapons.WeaponLook

class CharacterLoadout<O : MegaPlayer>(
    val character: PlayerCharacter,
    looksInSelectOrder: OrderedMap<WeaponDef<O>, WeaponLook<O>>,
    private val rewards: ObjectMap<LevelDefinition, Array<WeaponDef<O>>>
) {

    val weapons = Array<WeaponDef<O>>()

    private val looks = ObjectMap<WeaponDef<*>, WeaponLook<O>>()
    private val noRewards = Array<WeaponDef<O>>()
    private val rewardLevels = Array<LevelDefinition>()

    init {
        looksInSelectOrder.forEach { entry ->
            weapons.add(entry.key)
            looks.put(entry.key, entry.value)
        }
        rewards.forEach { entry ->
            rewardLevels.add(entry.key)
            entry.value.forEach { reward ->
                if (!looks.containsKey(reward)) throw IllegalArgumentException(
                    "The reward $reward is not a weapon of the $character loadout"
                )
            }
        }
    }

    fun contains(weapon: WeaponDef<*>) = looks.containsKey(weapon)

    fun find(weapon: WeaponDef<*>): WeaponDef<O> {
        for (i in 0 until weapons.size) if (weapons[i] === weapon) return weapons[i]
        throw IllegalArgumentException("The $character loadout has no weapon $weapon")
    }

    fun lookOf(weapon: WeaponDef<*>): WeaponLook<O> =
        looks[weapon] ?: throw IllegalArgumentException("The $character loadout has no look for the weapon $weapon")

    fun isRewardOfAny(levels: ObjectSet<LevelDefinition>, weapon: WeaponDef<*>): Boolean {
        for (i in 0 until rewardLevels.size) {
            val level = rewardLevels[i]
            if (levels.contains(level) && holds(rewardsFor(level), weapon)) return true
        }
        return false
    }

    fun rewardsFor(level: LevelDefinition): Array<WeaponDef<O>> = rewards[level] ?: noRewards

    override fun toString() = "CharacterLoadout(character=$character, weapons=$weapons)"

    private fun holds(defs: Array<WeaponDef<O>>, weapon: WeaponDef<*>): Boolean {
        for (i in 0 until defs.size) if (defs[i] === weapon) return true
        return false
    }
}

object CharacterLoadouts {

    val all: Array<CharacterLoadout<*>> by lazy {
        Array<CharacterLoadout<*>>().apply { PlayerCharacter.entries.forEach { add(of(it)) } }
    }

    fun of(character: PlayerCharacter): CharacterLoadout<*> = when (character) {
        PlayerCharacter.MEGAMAN -> MEGAMAN_LOADOUT
        PlayerCharacter.X -> MEGAMAN_X_LOADOUT
        PlayerCharacter.PROTOMAN -> PROTOMAN_LOADOUT
    }

    fun weaponById(id: String): WeaponDef<*>? {
        all.forEach { loadout ->
            loadout.weapons.forEach { weapon -> if (weapon.id.equals(id, ignoreCase = true)) return weapon }
        }
        return null
    }
}
