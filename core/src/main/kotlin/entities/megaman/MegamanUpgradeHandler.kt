package com.megaman.maverick.game.entities.megaman

import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.GameState
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank

interface IMegaUpgradable {

    val upgradeHandler: MegamanUpgradeHandler

    fun hasHealthTanks() = upgradeHandler.hasAnyHealthTank()

    fun has(heartTank: MegaHeartTank) = upgradeHandler.has(heartTank)

    fun add(heartTank: MegaHeartTank) = upgradeHandler.add(heartTank)

    fun addToHealthTank(health: Int) = upgradeHandler.add(health)

    fun put(healthTank: MegaHealthTank) = upgradeHandler.put(healthTank)

    fun put(healthTank: MegaHealthTank, health: Int) = upgradeHandler.put(healthTank, health)

    fun has(healthTank: MegaHealthTank) = upgradeHandler.has(healthTank)

    operator fun get(healthTank: MegaHealthTank) = upgradeHandler[healthTank]

    fun has(ability: MegaAbility) = upgradeHandler.has(ability)

    fun add(ability: MegaAbility) = upgradeHandler.add(ability)

    fun has(enhancement: MegaEnhancement) = upgradeHandler.has(enhancement)

    fun add(enhancement: MegaEnhancement) = upgradeHandler.add(enhancement)
}

class MegamanUpgradeHandler(private val state: GameState, private val megaman: Megaman) {

    private val heartTanks = state.heartTanksCollected
    private val healthTanks = state.healthTanksCollected
    private val abilities = state.abilitiesAttained
    private val enhancements = state.enhancementsAttained

    fun has(heartTank: MegaHeartTank) = heartTanks.contains(heartTank)

    fun add(heartTank: MegaHeartTank) {
        if (has(heartTank)) return
        state.heartTanksCollected.add(heartTank)
        megaman.getHealthPoints().max += MegaHeartTank.HEALTH_BUMP
    }

    fun add(health: Int): Boolean {
        check(health >= 0) { "Cannot add negative amount of health" }
        if (health == 0 || megaman.getHealthPoints().current == megaman.getHealthPoints().max)
            return false

        var temp = health

        for (healthTank in healthTanks.entries()) {
            val tankAmountToFill = ConstVals.MAX_HEALTH - healthTank.value
            // health tank is full so continue
            if (tankAmountToFill <= 0) continue
            // health is less than amount needed to fill the health tank
            else if (tankAmountToFill >= temp) {
                healthTank.value += temp
                return true
            }
            // health is greater than amount needed to fill the health tank
            else {
                healthTank.value += tankAmountToFill
                temp -= tankAmountToFill
            }
        }

        // if any tanks were filled, then the temp health value should not be equal to the
        // original health value
        return health != temp
    }

    fun put(healthTank: MegaHealthTank) = put(healthTank, 0)

    fun put(healthTank: MegaHealthTank, health: Int) {
        var temp = health
        if (temp > ConstVals.MAX_HEALTH) temp = ConstVals.MAX_HEALTH
        else if (health < 0) temp = 0

        healthTanks.put(healthTank, temp)
    }

    fun hasAnyHealthTank() = !healthTanks.isEmpty

    fun has(healthTank: MegaHealthTank) = healthTanks.containsKey(healthTank)

    operator fun get(healthTank: MegaHealthTank): Int = if (!has(healthTank)) 0 else healthTanks[healthTank]

    fun has(ability: MegaAbility) = abilities.contains(ability)

    fun add(ability: MegaAbility) = abilities.add(ability)

    fun has(enhancement: MegaEnhancement) = enhancements.contains(enhancement)

    fun add(enhancement: MegaEnhancement) = enhancements.add(enhancement)
}
