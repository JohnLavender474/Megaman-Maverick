package com.megaman.maverick.game.state

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.toGdxArray
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.Points
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.levels.LevelDefMap
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelRewards
import com.megaman.maverick.game.levels.LevelType
import kotlin.math.min

class GameState : Resettable {

    companion object {
        const val TAG = "GameState"
    }

    private val levelsDefeated = ObjectSet<LevelDefinition>()
    private val heartTanksCollected = ObjectSet<MegaHeartTank>()
    private val enhancementsAttained = ObjectSet<MegaEnhancement>()
    private val healthTanksCollected = ObjectMap<MegaHealthTank, Points>()
    private val weaponsOwned = ObjectSet<MegamanWeapon>()
    private var currency = Points(ConstVals.MIN_CURRENCY, ConstVals.MAX_CURRENCY, ConstVals.MIN_CURRENCY)
    private var difficultyMode = DifficultyMode.NORMAL

    // TODO: one lives count is shared by all players; per-player lives (decision H in item 029) need a count for
    //  each slot, and the lives calls already take the slot (item 047)
    private val lives = Points(ConstVals.MIN_LIVES, ConstVals.MAX_LIVES, ConstVals.START_LIVES)

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
        if (added) {
            weaponsOwned.addAll(LevelRewards.weaponsFor(level))
            listeners.forEach { it.onAddLevelDefeated(level) }
        }
    }

    fun removeLevelDefeated(level: LevelDefinition) {
        val removed = levelsDefeated.remove(level)
        GameLogger.debug(TAG, "removeLevelDefeated(): level=$level, removed=$removed")
        if (removed) {
            LevelRewards.weaponsFor(level).forEach { weaponsOwned.remove(it) }
            listeners.forEach { it.onRemoveLevelDefeated(level) }
        }
    }

    fun allRobotMasterLevelsDefeated() =
        LevelDefMap.getDefsOfLevelType(LevelType.ROBOT_MASTER_LEVEL).all { level -> isLevelDefeated(level) }

    fun getNextWilyStage(): LevelDefinition? {
        if (!allRobotMasterLevelsDefeated()) return null
        val wilyStages = LevelDefMap.getDefsOfLevelType(LevelType.WILY_LEVEL)
        return wilyStages.firstOrNull { wilyStage -> !isLevelDefeated(wilyStage) }
    }

    fun allLevelsDefeated() = allRobotMasterLevelsDefeated() && getNextWilyStage() == null

    fun containsWeapon(weapon: MegamanWeapon) = weapon == MegamanWeapon.MEGA_BUSTER || weaponsOwned.contains(weapon)

    fun addWeapon(weapon: MegamanWeapon) {
        val added = weaponsOwned.add(weapon)
        GameLogger.debug(TAG, "addWeapon(): weapon=$weapon, added=$added")
    }

    fun clearWeapons() {
        GameLogger.debug(TAG, "clearWeapons()")
        weaponsOwned.clear()
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
        if (!containsHealthTank(healthTank)) {
            GameLogger.error(
                TAG,
                "addHealthToHealthTank(): cannot add health because state does not contain health tank: $healthTank"
            )
            return
        }

        if (value <= 0) {
            GameLogger.error(TAG, "addHealthToHealthTank(): value must be greater than 0: $value")
            return
        }

        val tank = healthTanksCollected[healthTank]
        if (tank.translate(value)) {
            GameLogger.debug(TAG, "addHealthToHealthTank(): added $value to healthTank=$healthTank")
            listeners.forEach { it.onAddHealthToHealthTank(healthTank, value) }
        } else GameLogger.error(TAG, "addHealthToHealthTank(): failed to add $value to healthTank=$healthTank")

        GameLogger.debug(TAG, "addHealthToHealthTank(): all tanks: $healthTanksCollected")
    }

    fun removeHealthFromHealthTank(healthTank: MegaHealthTank, value: Int) {
        if (!containsHealthTank(healthTank)) {
            GameLogger.error(
                TAG,
                "addHealthToHealthTank(): cannot add health because state does not contain health tank: $healthTank"
            )
            return
        }

        if (value <= 0) {
            GameLogger.error(TAG, "addHealthToHealthTank(): value must be greater than 0: $value")
            return
        }

        val tank = healthTanksCollected[healthTank]
        if (tank.translate(-value)) {
            GameLogger.debug(TAG, "removeHealthFromHealthTank(): removed $value to healthTank=$healthTank")
            listeners.forEach { it.onRemoveHealthFromHealthTank(healthTank, value) }
        } else GameLogger.error(
            TAG,
            "removeHealthFromHealthTank(): failed to remove $value from healthTank=$healthTank"
        )
    }

    fun fillHealthTank(healthTank: MegaHealthTank) {
        if (!containsHealthTank(healthTank)) {
            putHealthTank(healthTank, ConstVals.MAX_HEALTH)
            return
        }
        val missing = ConstVals.MAX_HEALTH - getHealthTankValue(healthTank)
        if (missing > 0) addHealthToHealthTank(healthTank, missing)
    }

    fun hasAnyHealthTanks() =MegaHealthTank.entries.any { containsHealthTank(it) }

    fun addToHealthTanks(health: Int): Boolean {
        check(health >= 0) { "Cannot add negative amount of health" }

        // fill the tanks that are closest to being full first so that health is topped off into a partially
        // filled tank instead of being spread across tanks that are empty; ties are broken by enum order
        val tanks = MegaHealthTank.entries
            .filter { containsHealthTank(it) && getHealthTankValue(it) < ConstVals.MAX_HEALTH }
            .sortedWith(compareByDescending<MegaHealthTank> { getHealthTankValue(it) }.thenBy { it.ordinal })

        var temp = health
        for (healthTank in tanks) {
            if (temp <= 0) break

            val tankAmountToFill = ConstVals.MAX_HEALTH - getHealthTankValue(healthTank)
            val amountToAdd = min(tankAmountToFill, temp)

            GameLogger.debug(
                TAG, "addToHealthTanks(): healthTank=$healthTank: " +
                    "tankAmountToFill=$tankAmountToFill, temp=$temp: add amountToAdd=$amountToAdd"
            )

            addHealthToHealthTank(healthTank, amountToAdd)
            temp -= amountToAdd
        }

        // if any tanks were filled, then the temp health value should not be equal to the original health value
        return health != temp
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

        if (currency.translate(value)) {
            GameLogger.debug(TAG, "addCurrency(): added $value to currency")
            listeners.forEach { it.onAddCurrency(value) }
        } else GameLogger.debug(
            TAG, "addCurrency(): did not add $value to currency. " +
                "Not an error if currency is already at max: getCurrency=${getCurrency()}."
        )
    }

    fun removeCurrency(value: Int) {
        if (value <= 0) {
            GameLogger.error(TAG, "removeCurrency(): value must be greater than 0: $value.")
            return
        }

        if (currency.translate(-value)) {
            GameLogger.debug(TAG, "removeCurrency(): removed $value from currency")
            listeners.forEach { it.onRemoveCurrency(value) }
        } else GameLogger.debug(
            TAG, "removeCurrency(): did not remove $value from currency. " +
                "Not an error if currency is already at min: getCurrency=${getCurrency()}."
        )
    }

    fun getCurrency() = currency.current

    fun getMaxCurrency() = currency.max

    @Suppress("UNUSED_PARAMETER")
    fun getLives(slot: PlayerSlot) = lives.current

    @Suppress("UNUSED_PARAMETER")
    fun addLife(slot: PlayerSlot) = lives.translate(1)

    @Suppress("UNUSED_PARAMETER")
    fun removeLife(slot: PlayerSlot) = lives.translate(-1)

    // TODO: with per-player lives, game over is a level-flow rule (for example when every player has no lives left)
    //  (item 047)
    fun livesExhausted() = lives.isMin()

    fun resetLives() = lives.set(ConstVals.START_LIVES)

    fun setLives(value: Int) = lives.set(value)

    fun setDifficultyMode(difficultyMode: DifficultyMode) {
        this.difficultyMode = difficultyMode
    }

    fun getDifficultyMode() = difficultyMode

    override fun reset() {
        GameLogger.debug(TAG, "reset()")

        val levelIter = levelsDefeated.toGdxArray().iterator()
        while (levelIter.hasNext()) {
            val level = levelIter.next()
            try {
                removeLevelDefeated(level)
            } catch (e: Exception) {
                throw Exception("Failed to remove level: ${level}. Levels defeated: $levelsDefeated", e)
            }
        }

        val heartTankIter = heartTanksCollected.toGdxArray().iterator()
        while (heartTankIter.hasNext()) {
            val heartTank = heartTankIter.next()
            try {
                removeHeartTank(heartTank)
            } catch (e: Exception) {
                throw Exception("Failed to remove heart tank: ${heartTank}. Levels defeated: $heartTanksCollected", e)
            }
        }

        val healthTankIter = healthTanksCollected.keys().toGdxArray().iterator()
        while (healthTankIter.hasNext()) {
            val healthTank = healthTankIter.next()
            try {
                removeHealthTank(healthTank)
            } catch (e: Exception) {
                throw Exception(
                    "Failed to remove health tank: ${healthTank}. Levels defeated: $healthTanksCollected",
                    e
                )
            }
        }

        val enhancementIter = enhancementsAttained.toGdxArray().iterator()
        while (enhancementIter.hasNext()) {
            val enhancement = enhancementIter.next()
            try {
                removeEnhancement(enhancement)
            } catch (e: Exception) {
                throw Exception("Failed to remove enhancement: ${enhancement}. Levels defeated: $enhancement", e)
            }
        }

        val currencyToRemove = getCurrency() - currency.min
        if (currencyToRemove > 0) removeCurrency(currencyToRemove)

        difficultyMode = DifficultyMode.NORMAL
    }

    override fun toString(): String {
        val builder = StringBuilder()

        val levelDefs = levelsDefeated.sorted().joinToString(",") { it.name }
        builder.append("$levelDefs;")

        val heartTanks = heartTanksCollected.sorted().joinToString(",") { it.name }
        builder.append("$heartTanks;")

        val healthTanks = healthTanksCollected.keys().sorted().joinToString(",") { it.name }
        builder.append("$healthTanks;")

        val enhancements = enhancementsAttained.sorted().joinToString(",") { it.name }
        builder.append("$enhancements;")

        builder.append("${currency.current};")

        builder.append(difficultyMode.name)

        val s = builder.toString()

        GameLogger.debug(TAG, "toString(): s=$s")

        return s
    }

    fun fromString(s: String) {
        GameLogger.debug(TAG, "fromString(): s=$s")

        val lines = s.split(";").map { it.replace("\\s+", "").split(",") }.toGdxArray()
        for (i in 0 until lines.size) lines[i] = lines[i].filter { !it.isBlank() }

        if (lines.size >= 1 && !lines[0].isEmpty()) lines[0].forEach {
            try {
                val level = LevelDefinition.valueOf(it.uppercase())
                if (level.type != LevelType.EXTRA_LEVEL) addLevelDefeated(level)
                else GameLogger.log(TAG, "fromString(): do not record extra level as defeated: $it")
            } catch (e: Exception) {
                GameLogger.error(TAG, "fromString(): failed to add level: $it", e)
            }
        }

        if (lines.size >= 2 && !lines[1].isEmpty()) lines[1].forEach {
            try {
                val heartTank = MegaHeartTank.valueOf(it.uppercase())
                addHeartTank(heartTank)
            } catch (e: Exception) {
                GameLogger.error(TAG, "fromString(): failed to add heart tank: $it", e)
            }
        }

        if (lines.size >= 3 && !lines[2].isEmpty()) lines[2].forEach {
            try {
                val healthTank = MegaHealthTank.valueOf(it.uppercase())
                putHealthTank(healthTank)
            } catch (e: Exception) {
                GameLogger.error(TAG, "fromString(): failed to add health tank: $it", e)
            }
        }

        if (lines.size >= 4 && !lines[3].isEmpty()) lines[3].forEach {
            try {
                val enhancement = MegaEnhancement.valueOf(it.uppercase())
                addEnhancement(enhancement)
            } catch (e: Exception) {
                GameLogger.error(TAG, "fromString(): failed to add enhancement: $it", e)
            }
        }

        if (lines.size >= 5 && !lines[4].isEmpty()) {
            try {
                val currency = lines[4][0].toInt()
                this.currency.set(currency)
            } catch (e: Exception) {
                GameLogger.error(TAG, "fromString(): failed to load currency", e)
            }
        }

        if (lines.size >= 6 && !lines[5].isEmpty()) {
            try {
                val difficultyMode = DifficultyMode.valueOf(lines[5][0].uppercase())
                this.difficultyMode = difficultyMode
            } catch (e: Exception) {
                GameLogger.debug(TAG, "fromString(): failed to load difficulty mode, default to NORMAL", e)
                this.difficultyMode = DifficultyMode.NORMAL
            }
        }
    }
}
