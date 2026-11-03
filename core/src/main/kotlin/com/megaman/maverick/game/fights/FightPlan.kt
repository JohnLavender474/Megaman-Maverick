package com.megaman.maverick.game.fights

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.state.Step
import com.megaman.maverick.game.assets.MusicAsset

sealed interface FightRule {

    fun isMet(fight: Fight): Boolean

    object AllDefeated : FightRule {

        // TODO: a fight holds 1 boss, so this rule reads only that boss; a fight with more than one boss at once needs
        //  `Fight.cast` (item 118, ITEM.md, "Possible later, with no caller yet")
        override fun isMet(fight: Fight) = fight.boss.defeated
    }
}

class FightPlan(
    val intro: Array<Step<Fight>>,
    val win: FightRule,
    val outro: Array<Step<Fight>>,
    val music: MusicAsset
)
