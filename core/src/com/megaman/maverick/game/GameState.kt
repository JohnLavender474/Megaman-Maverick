package com.megaman.maverick.game

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.interfaces.Resettable
import com.engine.points.Points
import com.megaman.maverick.game.entities.bosses.BossType
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank

data class GameState(
    var bossesDefeated: ObjectSet<BossType> = ObjectSet(),
    var heartTanksCollected: ObjectSet<MegaHeartTank> = ObjectSet(),
    var healthTanksCollected: ObjectMap<MegaHealthTank, Int> = ObjectMap()
) : Resettable {

    companion object {
        const val DEFAULT_LIVES = 3
        const val MAX_LIVES = 9
    }

    val lives = Points(0, MAX_LIVES, DEFAULT_LIVES)

    override fun reset() {
        lives.set(DEFAULT_LIVES)
        bossesDefeated.clear()
        heartTanksCollected.clear()
        healthTanksCollected.clear()
    }
}