package com.megaman.maverick.game

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.points.Points
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition

data class GameState(
    var levelsDefeated: ObjectSet<LevelDefinition> = ObjectSet(),
    var heartTanksCollected: ObjectSet<MegaHeartTank> = ObjectSet(),
    var healthTanksCollected: ObjectMap<MegaHealthTank, Int> = ObjectMap(),
    var abilitiesAttained: ObjectSet<MegaAbility> = ObjectSet(),
    var enhancementsAttained: ObjectSet<MegaEnhancement> = ObjectSet()
) : Resettable {

    companion object {
        const val DEFAULT_LIVES = 3
        const val MAX_LIVES = 9
    }

    val lives = Points(0, MAX_LIVES, DEFAULT_LIVES)

    override fun reset() {
        lives.set(DEFAULT_LIVES)
        levelsDefeated.clear()
        heartTanksCollected.clear()
        healthTanksCollected.clear()
        // TODO: enhancementsAttained.clear()
        // TODO: abilitiesAttained.clear()
    }


}
