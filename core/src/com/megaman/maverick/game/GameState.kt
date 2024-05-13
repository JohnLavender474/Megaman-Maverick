package com.megaman.maverick.game

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.interfaces.IClearable
import com.megaman.maverick.game.entities.bosses.BossType
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank

data class GameState(
    var bossesDefeated: ObjectSet<BossType> = ObjectSet(),
    var heartTanksCollected: ObjectSet<MegaHeartTank> = ObjectSet(),
    var healthTanksCollected: ObjectMap<MegaHealthTank, Int> = ObjectMap()
) : IClearable {

    override fun clear() {
        bossesDefeated.clear()
        heartTanksCollected.clear()
        healthTanksCollected.clear()
    }
}