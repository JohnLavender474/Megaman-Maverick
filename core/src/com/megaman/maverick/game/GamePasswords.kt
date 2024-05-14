package com.megaman.maverick.game

import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.objects.MultiCollectionIterable
import com.megaman.maverick.game.entities.bosses.BossType
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank

object GamePasswords {

    private val indices = gdxArrayOf(8, 31, 18, 21, 15, 2, 17, 12, 33, 1, 23, 16, 6, 14, 5, 11, 28, 10, 13, 0)

    fun getGamePassword(state: GameState): IntArray {
        val password = IntArray(36)
        val multiCollectionIterable = MultiCollectionIterable(
            gdxArrayOf(
                BossType.values().toGdxArray(),
                MegaHeartTank.values().toGdxArray(),
                MegaHealthTank.values().toGdxArray()
            )
        )
        multiCollectionIterable.forEach { outerIndex, _, value ->
            val index = indices[outerIndex]
            val digit = when (value) {
                is BossType -> if (state.bossesDefeated.contains(value)) 1 else 0
                is MegaHeartTank -> if (state.heartTanksCollected.contains(value)) 1 else 0
                is MegaHealthTank -> if (state.healthTanksCollected.containsKey(value)) 1 else 0
                else -> null
            }
            if (digit != null) password[index] = digit
        }
        return password
    }

    fun loadGamePassword(state: GameState, password: IntArray) {
        state.reset()
        val (bossesDefeated, heartTanksCollected, healthTanksCollected) = state

        val passwordArray = password.map { it.toString().toInt() }.toIntArray()
        val multiCollectionIterable = MultiCollectionIterable(
            gdxArrayOf(
                BossType.values().toGdxArray(),
                MegaHeartTank.values().toGdxArray(),
                MegaHealthTank.values().toGdxArray()
            )
        )
        multiCollectionIterable.forEach { outerIndex, _, value ->
            val index = indices[outerIndex]
            when (value) {
                is BossType -> if (passwordArray[index] == 1) bossesDefeated.add(value)
                is MegaHeartTank -> if (passwordArray[index] == 1) heartTanksCollected.add(value)
                is MegaHealthTank -> if (passwordArray[index] == 1) healthTanksCollected.put(value, 0)
            }
        }
    }
}