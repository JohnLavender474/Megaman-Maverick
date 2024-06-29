package com.megaman.maverick.game.entities.megaman

import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.GameState
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank

interface IMegaUpgradable {

    val upgradeHandler: MegamanUpgradeHandler

    fun hasHealthTanks() = upgradeHandler.hasHealthTanks()

    fun has(heartTank: MegaHeartTank) = upgradeHandler.has(heartTank)

    fun add(heartTank: MegaHeartTank) = upgradeHandler.add(heartTank)

    fun addToHealthTank(health: Int) = upgradeHandler.add(health)

    fun put(healthTank: MegaHealthTank) = upgradeHandler.put(healthTank)

    fun put(healthTank: MegaHealthTank, health: Int) = upgradeHandler.put(healthTank, health)

    fun has(healthTank: MegaHealthTank) = upgradeHandler.has(healthTank)

    operator fun get(healthTank: MegaHealthTank) = upgradeHandler[healthTank]

    fun has(ability: MegaAbility) = upgradeHandler.has(ability)

    fun add(ability: MegaAbility) = upgradeHandler.add(ability)
}

class MegamanUpgradeHandler(private val gameState: GameState, private val megaman: Megaman) {

    private val heartTanks = gameState.heartTanksCollected
    private val healthTanks = gameState.healthTanksCollected
    private val abilities = gameState.abilitiesAttained

    fun has(heartTank: MegaHeartTank) = heartTanks.contains(heartTank)

    fun add(heartTank: MegaHeartTank) {
        if (has(heartTank)) return
        gameState.heartTanksCollected.add(heartTank)
        megaman.getHealthPoints().max += MegaHeartTank.HEALTH_BUMP
    }

    fun add(health: Int): Boolean {
        check(health >= 0) { "Cannot add negative amount of health" }
        if (health == 0 || megaman.getHealthPoints().current == megaman.getHealthPoints().max)
            return false

        var _health = health

        for (healthTank in healthTanks.entries()) {
            val tankAmountToFill = ConstVals.MAX_HEALTH - healthTank.value
            // health tank is full so continue
            if (tankAmountToFill <= 0) continue
            // health is less than amount needed to fill the health tank
            else if (tankAmountToFill >= _health) {
                healthTank.value += _health
                return true
            }
            // health is greater than amount needed to fill the health tank
            else {
                healthTank.value += tankAmountToFill
                _health -= tankAmountToFill
            }
        }

        // if any tanks were filled, then the temp health value should not be equal to the
        // original health value
        return health != _health
    }

    fun put(healthTank: MegaHealthTank) = put(healthTank, 0)

    fun put(healthTank: MegaHealthTank, health: Int) {
        var _health = health
        if (_health > ConstVals.MAX_HEALTH) _health = ConstVals.MAX_HEALTH
        else if (health < 0) _health = 0

        healthTanks.put(healthTank, _health)
    }

    fun hasHealthTanks() = !healthTanks.isEmpty

    fun has(healthTank: MegaHealthTank) = healthTanks.containsKey(healthTank)

    operator fun get(healthTank: MegaHealthTank): Int = if (!has(healthTank)) 0 else healthTanks[healthTank]

    fun has(ability: MegaAbility) = abilities.contains(ability)

    fun add(ability: MegaAbility) = abilities.add(ability)
}
