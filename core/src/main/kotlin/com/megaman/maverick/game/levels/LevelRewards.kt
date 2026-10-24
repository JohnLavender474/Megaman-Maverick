package com.megaman.maverick.game.levels

import com.badlogic.gdx.utils.OrderedSet
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon

object LevelRewards {

    fun weaponsFor(levelDef: LevelDefinition): OrderedSet<MegamanWeapon> {
        val set = OrderedSet<MegamanWeapon>()
        when (levelDef) {
            LevelDefinition.MOON_MAN -> set.addAll(MegamanWeapon.MOON_SCYTHES, MegamanWeapon.RUSH_JET)
            LevelDefinition.PRECIOUS_WOMAN -> set.add(MegamanWeapon.PRECIOUS_GUARD)
            LevelDefinition.INFERNO_MAN -> set.add(MegamanWeapon.INFERNAL_BARRAGE)
            LevelDefinition.GLACIER_MAN -> set.add(MegamanWeapon.FRIGID_SHOT)
            LevelDefinition.TIMBER_WOMAN -> set.add(MegamanWeapon.AXE_SWINGER)
            LevelDefinition.DESERT_MAN -> set.add(MegamanWeapon.NEEDLE_SPIN)
            LevelDefinition.RODENT_MAN -> set.add(MegamanWeapon.RODENT_CLAWS)
            LevelDefinition.REACTOR_MAN -> set.add(MegamanWeapon.REACTOR_SHOT)
            else -> {}
        }
        return set
    }
}
