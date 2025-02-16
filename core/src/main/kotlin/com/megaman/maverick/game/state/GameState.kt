package com.megaman.maverick.game.state

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.points.Points
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition

class GameState : Resettable {

    companion object {
        const val TAG = "GameState"
        private const val EXPECTED_STRING_LINES = 5
    }

    private val levelsDefeated = ObjectSet<LevelDefinition>()
    private val heartTanksCollected = ObjectSet<MegaHeartTank>()
    private val healthTanksCollected = ObjectMap<MegaHealthTank, Points>()
    private val enhancementsAttained = ObjectSet<MegaEnhancement>()
    private var currency = Points(ConstVals.MIN_CURRENCY, ConstVals.MAX_CURRENCY, ConstVals.MIN_CURRENCY)

    private val listeners = OrderedSet<IGameStateListener>()

    fun addListener(listener: IGameStateListener) {
        val added = listeners.add(listener)
        GameLogger.debug(TAG, "addListener(): listener=$listener, added=$added")
    }

    fun removeListener(listener: IGameStateListener) {
        val removed = listeners.remove(listener)
        GameLogger.debug(TAG, "removeListener(): listener=$listener, removed=$removed")
    }

    fun isLevelDefeated(level: LevelDefinition) = levelsDefeated.contains(level)

    fun addLevelDefeated(level: LevelDefinition) {
        val added = levelsDefeated.add(level)
        GameLogger.debug(TAG, "addLevelDefeated(): level=$level, added=$added")
        if (added) listeners.forEach { it.onAddLevelDefeated(level) }
    }

    fun removeLevelDefeated(level: LevelDefinition) {
        val removed = levelsDefeated.remove(level)
        GameLogger.debug(TAG, "removeLevelDefeated(): level=$level, removed=$removed")
        if (removed) listeners.forEach { it.onRemoveLevelDefeated(level) }
    }

    fun containsHeartTank(heartTank: MegaHeartTank) = heartTanksCollected.contains(heartTank)

    fun addHeartTank(heartTank: MegaHeartTank) {
        val added = heartTanksCollected.add(heartTank)
        GameLogger.debug(TAG, "addHeartTank(): heartTank=$heartTank, added=$added")
        if (added) listeners.forEach { it.onAddHeartTank(heartTank) }
    }

    fun removeHeartTank(heartTank: MegaHeartTank) {
        val removed = heartTanksCollected.remove(heartTank)
        GameLogger.debug(TAG, "removeHeartTank(): heartTank=$heartTank, removed=$removed")
        if (removed) listeners.forEach { it.onRemoveHeartTank(heartTank) }
    }

    fun containsHealthTank(healthTank: MegaHealthTank) = healthTanksCollected.containsKey(healthTank)

    fun putHealthTank(healthTank: MegaHealthTank, value: Int = ConstVals.MIN_HEALTH) {
        if (healthTanksCollected.containsKey(healthTank)) {
            GameLogger.error(TAG, "addHealthTank(): already has health tank: $healthTank")
            return
        }

        GameLogger.debug(TAG, "addHealthTank(): healthTank=$healthTank")

        healthTanksCollected.put(healthTank, Points(ConstVals.MIN_HEALTH, ConstVals.MAX_HEALTH, value))
        listeners.forEach { it.onPutHealthTank(healthTank) }
    }

    fun removeHealthTank(healthTank: MegaHealthTank) {
        if (!healthTanksCollected.containsKey(healthTank)) {
            GameLogger.error(TAG, "removeHealthTank(): does not have health tank: $healthTank")
            return
        }

        GameLogger.debug(TAG, "removeHealthTank(): healthTank=$healthTank")

        healthTanksCollected.remove(healthTank)
        listeners.forEach { it.onRemoveHealthTank(healthTank) }
    }

    fun getHealthTankValue(healthTank: MegaHealthTank) = healthTanksCollected[healthTank]?.current ?: 0

    fun addHealthToHealthTank(healthTank: MegaHealthTank, value: Int) {
        if (!containsHealthTank(healthTank)) return

        val tank = healthTanksCollected[healthTank]
        if (tank.translate(value)) listeners.forEach { it.onAddHealthToHealthTank(healthTank, value) }
    }

    fun containsEnhancement(enhancement: MegaEnhancement) = enhancementsAttained.contains(enhancement)

    fun addEnhancement(enhancement: MegaEnhancement) {
        val added = enhancementsAttained.add(enhancement)
        GameLogger.debug(TAG, "addEnhancement(): enhancement=$enhancement, added=$added")
        if (added) listeners.forEach { it.onAddEnhancement(enhancement) }
    }

    fun removeEnhancement(enhancement: MegaEnhancement) {
        val removed = enhancementsAttained.remove(enhancement)
        GameLogger.debug(TAG, "removeEnhancement(): enhancement=$enhancement, removed=$removed")
        if (removed) listeners.forEach { it.onRemoveEnhancement(enhancement) }
    }

    fun addCurrency(value: Int) {
        if (value <= 0) {
            GameLogger.error(TAG, "addCurrency(): value must be greater than 0: $value")
            return
        }

        if (currency.translate(value)) listeners.forEach { it.onAddCurrency(value) }
    }

    fun removeCurrency(value: Int) {
        if (value <= 0) {
            GameLogger.error(TAG, "removeCurrency(): value must be greater than 0: $value")
            return
        }

        if (currency.translate(-value)) listeners.forEach { it.onRemoveCurrency(value) }
    }

    fun getCurrency() = currency.current

    fun getMaxCurrency() = currency.max

    fun getMinCurrency() = currency.min

    override fun reset() {
        GameLogger.debug(TAG, "reset()")

        val levelIter = levelsDefeated.iterator()
        while (levelIter.hasNext) {
            val level = levelIter.next()
            removeLevelDefeated(level)
        }

        val heartTankIter = heartTanksCollected.iterator()
        while (heartTankIter.hasNext) {
            val heartTank = heartTankIter.next()
            removeHeartTank(heartTank)
        }

        val healthTankIter = healthTanksCollected.keys().iterator()
        while (healthTankIter.hasNext) {
            val healthTank = healthTankIter.next()
            removeHealthTank(healthTank)
        }

        val enhancementIter = enhancementsAttained.iterator()
        while (enhancementIter.hasNext) {
            val enhancement = enhancementIter.next()
            removeEnhancement(enhancement)
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()

        val levelDefs = levelsDefeated.joinToString(",") { it.name }
        builder.append("$levelDefs;")

        val heartTanks = heartTanksCollected.joinToString(",") { it.name }
        builder.append("$heartTanks;")

        val healthTanks = healthTanksCollected.keys().joinToString(",") { it.name }
        builder.append("$healthTanks;")

        val enhancements = enhancementsAttained.joinToString(",") { it.name }
        builder.append("$enhancements;")

        builder.append(currency)

        val s = builder.toString()
        GameLogger.debug(TAG, "toString(): s=$s")
        return s
    }

    fun fromString(s: String) {
        GameLogger.debug(TAG, "fromString(): s=$s")

        val lines = s.split(";").map { it.split(",") }.toGdxArray()
        if (lines.size != EXPECTED_STRING_LINES) throw IllegalArgumentException("Invalid game state string: $s")
        for (i in 0 until lines.size) lines[i] = lines[i].filter { !it.isBlank() }

        if (!lines[0].isEmpty()) lines[0].forEach {
            try {
                val level = LevelDefinition.valueOf(it.uppercase())
                addLevelDefeated(level)
            } catch (e: Exception) {
                GameLogger.error(TAG, "fromString(): failed to add level: $it", e)
            }
        }

        if (!lines[1].isEmpty()) lines[1].forEach {
            try {
                val heartTank = MegaHeartTank.valueOf(it.uppercase())
                addHeartTank(heartTank)
            } catch (e: Exception) {
                GameLogger.error(TAG, "fromString(): failed to add heart tank: $it", e)
            }
        }

        if (!lines[2].isEmpty()) lines[2].forEach {
            try {
                val healthTank = MegaHealthTank.valueOf(it.uppercase())
                putHealthTank(healthTank)
            } catch (e: Exception) {
                GameLogger.error(TAG, "fromString(): failed to add health tank: $it", e)
            }
        }

        if (!lines[3].isEmpty()) lines[3].forEach {
            try {
                val enhancement = MegaEnhancement.valueOf(it.uppercase())
                addEnhancement(enhancement)
            } catch (e: Exception) {
                GameLogger.error(TAG, "fromString(): failed to add enhancement: $it", e)
            }
        }

        try {
            val currency = lines[4][0].toInt()
            this.currency.set(currency)
        } catch (e: Exception) {
            GameLogger.error(TAG, "fromString(): failed to load currency", e)
        }
    }
}
