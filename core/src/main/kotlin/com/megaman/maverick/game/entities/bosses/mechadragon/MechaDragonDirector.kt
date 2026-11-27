package com.megaman.maverick.game.entities.bosses.mechadragon

import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.random.GameRandom
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim

enum class MechaDragonMove { HOVER, CHARGE, LAND, BREATH, CHOMP, TAKEOFF }

object MechaDragonDirector {

    const val CHARGE_CHANCE_DELTA = 35
    const val MAX_HOVERS_PER_AIR_CYCLE = 2
    const val LAND_CHANCE = 50
    const val POCKET_CHOMP_CHANCE = 70
    const val OPEN_CHOMP_CHANCE = 15
    const val SECOND_BREATH_CHANCE = 30
    const val MAX_BREATHS_PER_GROUND_CYCLE = 2
    const val AIM_LEAD_TIME = 1f

    // TODO: take the aim mix from a shared lead-aim helper when one exists (item 158)
    val AIMS = gdxArrayOf(OrbAim.CURRENT, OrbAim.LEAD, OrbAim.NEAR)

    fun afterHover(hoversThisAirCycle: Int, chargeChance: Int, random: GameRandom) = when {
        hoversThisAirCycle >= MAX_HOVERS_PER_AIR_CYCLE -> MechaDragonMove.LAND
        random.nextInt(0, 100) <= chargeChance -> MechaDragonMove.CHARGE
        random.nextInt(0, 99) < LAND_CHANCE -> MechaDragonMove.LAND
        else -> MechaDragonMove.HOVER
    }

    fun chargeChanceAfter(move: MechaDragonMove, chargeChance: Int) =
        if (move == MechaDragonMove.CHARGE) 0 else minOf(chargeChance + CHARGE_CHANCE_DELTA, 100)

    fun afterBreath(breathsThisGroundCycle: Int, playerInPocket: Boolean, panic: Boolean, random: GameRandom): MechaDragonMove {
        val chompChance = when {
            playerInPocket && panic -> 100
            playerInPocket -> POCKET_CHOMP_CHANCE
            else -> OPEN_CHOMP_CHANCE
        }
        if (random.nextInt(0, 99) < chompChance) return MechaDragonMove.CHOMP
        if (breathsThisGroundCycle < MAX_BREATHS_PER_GROUND_CYCLE && random.nextInt(0, 99) < SECOND_BREATH_CHANCE)
            return MechaDragonMove.BREATH
        return MechaDragonMove.TAKEOFF
    }
}
