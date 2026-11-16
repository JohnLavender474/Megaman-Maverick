package com.megaman.maverick.game.levels

import com.badlogic.gdx.utils.OrderedSet
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.weapons.WeaponDef

object LevelRewards {

    fun weaponsFor(levelDef: LevelDefinition): OrderedSet<WeaponDef> {
        val set = OrderedSet<WeaponDef>()
        when (levelDef) {
            LevelDefinition.MOON_MAN -> set.addAll(MegamanWeapons.MOON_SCYTHES, MegamanWeapons.RUSH_JET)
            LevelDefinition.PRECIOUS_WOMAN -> set.add(MegamanWeapons.PRECIOUS_GUARD)
            LevelDefinition.INFERNO_MAN -> set.add(MegamanWeapons.INFERNAL_BARRAGE)
            LevelDefinition.GLACIER_MAN -> set.add(MegamanWeapons.FRIGID_SHOT)
            LevelDefinition.TIMBER_WOMAN -> set.add(MegamanWeapons.AXE_SWINGER)
            LevelDefinition.DESERT_MAN -> set.add(MegamanWeapons.NEEDLE_SPIN)
            LevelDefinition.RODENT_MAN -> set.add(MegamanWeapons.RODENT_CLAWS)
            LevelDefinition.REACTOR_MAN -> set.add(MegamanWeapons.REACTOR_SHOT)
            else -> {}
        }
        return set
    }
}
