package com.megaman.maverick.game

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.points.Points
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition

data class GameState(
    var levelsDefeated: ObjectSet<LevelDefinition> = ObjectSet(),
    var heartTanksCollected: ObjectSet<MegaHeartTank> = ObjectSet(),
    var healthTanksCollected: ObjectMap<MegaHealthTank, Int> = ObjectMap(),
    var enhancementsAttained: ObjectSet<MegaEnhancement> = ObjectSet(),
    val lives: Points = Points(0, MAX_LIVES, DEFAULT_LIVES)
) : Resettable {

    companion object {
        const val TAG = "GameState"

        const val DEFAULT_LIVES = 3
        const val MAX_LIVES = 9

        private const val EXPECTED_STRING_LINES = 4
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset()")

        levelsDefeated.clear()
        heartTanksCollected.clear()
        healthTanksCollected.clear()
        enhancementsAttained.clear()

        lives.set(DEFAULT_LIVES)
    }

    override fun toString(): String {
        val builder = StringBuilder()

        val levelDefs = levelsDefeated.joinToString(",") { it.name }
        builder.append("$levelDefs;")

        val heartTanks = heartTanksCollected.joinToString(",") { it.name }
        builder.append("$heartTanks;")

        val healthTanks = healthTanksCollected.joinToString(",") { it.key.name }
        builder.append("$healthTanks;")

        val enhancements = enhancementsAttained.joinToString(",") { it.name }
        builder.append(enhancements)

        val s = builder.toString()

        GameLogger.debug(TAG, "toString(): s=$s")

        return s
    }

    fun fromString(s: String) {
        GameLogger.debug(TAG, "fromString(): s=$s")

        val lines = s.split(";")
        if (lines.size != EXPECTED_STRING_LINES) throw IllegalArgumentException("Invalid game state string")

        val levelDefs = lines[0].split(",").map { LevelDefinition.valueOf(it) }
        levelDefs.forEach { levelsDefeated.add(it) }

        val heartTanks = lines[1].split(",").map { MegaHeartTank.valueOf(it) }
        heartTanks.forEach { heartTanksCollected.add(it) }

        val healthTanks = lines[2].split(",").map { MegaHealthTank.valueOf(it) }
        healthTanks.forEach { healthTanksCollected.put(it, 0) }

        val enhancements = lines[3].split(",").map { MegaEnhancement.valueOf(it) }
        enhancements.forEach { enhancementsAttained.add(it) }
    }
}
