package com.megaman.maverick.game.entities.bosses

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Context
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director

fun d1000Context(
    r: Int = 0,
    tier: D1000Tier = D1000Tier.NORMAL,
    last: D1000Action? = null,
    beforeLast: D1000Action? = null,
    distance: D1000Distance = D1000Distance.FAR,
    playerOnWall: Boolean = false,
    playerAbove: Boolean = false,
    playerNearEye: Boolean = false,
    cyclesSinceCombo: Int = 0,
    hard: Boolean = false,
    playerAirborne: Boolean = false
) = D1000Context(r, tier, last, beforeLast, distance, playerOnWall, playerAbove, playerNearEye, cyclesSinceCombo, hard, playerAirborne)

private val ROLLS = 0 until D1000Director.ROLLS

private fun plan(context: D1000Context) = D1000Director.choose(context)!!

private fun actions(context: (Int) -> D1000Context) = ROLLS.map { plan(context(it)).action }.toSet()

private val CONTEXTS = D1000Tier.entries.flatMap { tier ->
    D1000Distance.entries.flatMap { distance ->
        listOf(false, true).flatMap { onWall ->
            listOf(false, true).flatMap { above ->
                listOf(false, true).map { nearEye -> { r: Int -> d1000Context(r, tier, null, null, distance, onWall, above, nearEye) } }
            }
        }
    }
}

class D1000Test : DescribeSpec({

    describe("choose") {

        it("crouches at once when X is near, except just after a crouch") {
            D1000Tier.entries.forEach { tier ->
                ROLLS.forEach { r ->
                    plan(d1000Context(r, tier, distance = D1000Distance.NEAR)).action shouldBe
                        D1000Action.CROUCH
                    (plan(
                        d1000Context(r, tier, last = D1000Action.CROUCH, distance = D1000Distance.NEAR)
                    ).action == D1000Action.CROUCH) shouldBe false
                }
            }
        }

        it("never picks an action 3 times in a row") {
            D1000Action.entries.forEach { action ->
                CONTEXTS.forEach { context ->
                    val picked = actions { context(it).copy(last = action, beforeLast = action, distance = D1000Distance.FAR) }
                    picked.contains(action) shouldBe false
                }
            }
        }

        it("picks the same action twice in a row") {
            actions { d1000Context(it, last = D1000Action.RAM) }.contains(D1000Action.RAM) shouldBe true
        }

        it("never bombs before Overdrive, and bombs in Overdrive") {
            CONTEXTS.forEach { context ->
                val picked = actions(context)
                if (context(0).tier == D1000Tier.NORMAL) {
                    picked.contains(D1000Action.BOMB) shouldBe false
                    picked.contains(D1000Action.SWEEP) shouldBe false
                }
            }
            val overdrive = actions { d1000Context(it, D1000Tier.OVERDRIVE) }
            overdrive.contains(D1000Action.BOMB) shouldBe true
            overdrive.contains(D1000Action.SWEEP) shouldBe true
        }

        it("never walks while X is on a wall, and rams more") {
            val far = actions { d1000Context(it) }
            far.contains(D1000Action.WALK) shouldBe true
            D1000Tier.entries.forEach { tier ->
                val onWall = actions { d1000Context(it, tier, playerOnWall = true) }
                listOf(D1000Action.WALK, D1000Action.FENCE, D1000Action.SWEEP, D1000Action.CEILING).forEach { onWall.contains(it) shouldBe false }
            }
            val rams = ROLLS.count { plan(d1000Context(it)).action == D1000Action.RAM }
            val wallRams = ROLLS.count { plan(d1000Context(it, playerOnWall = true)).action == D1000Action.RAM }
            (wallRams > rams) shouldBe true
        }

        it("gives a follow-up only when the combo gap has passed") {
            listOf(false, true).forEach { hard ->
                D1000Tier.entries.forEach { tier ->
                    val gap = D1000Director.comboGap(tier, hard)
                    ROLLS.forEach { r ->
                        val ready = plan(d1000Context(r, tier, cyclesSinceCombo = gap, hard = hard))
                        ready.followUp shouldBe D1000Director.comboOf(ready.action)
                        plan(d1000Context(r, tier, cyclesSinceCombo = gap - 1, hard = hard))
                            .followUp shouldBe null
                    }
                }
            }
        }

        it("waits 4 plans between combos on Normal (Overdrive 3), and 3 on Hard (Overdrive 2)") {
            D1000Director.comboGap(D1000Tier.NORMAL, false) shouldBe 4
            D1000Director.comboGap(D1000Tier.OVERDRIVE, false) shouldBe 3
            D1000Director.comboGap(D1000Tier.NORMAL, true) shouldBe 3
            D1000Director.comboGap(D1000Tier.OVERDRIVE, true) shouldBe 2
        }

        it("never sweeps just after a slam on Normal, and can on Hard") {
            listOf(D1000Action.CROUCH, D1000Action.CEILING).forEach { slam ->
                actions { d1000Context(it, D1000Tier.OVERDRIVE, last = slam) }.contains(D1000Action.SWEEP) shouldBe false
                actions { d1000Context(it, D1000Tier.OVERDRIVE, last = slam, hard = true) }.contains(D1000Action.SWEEP) shouldBe
                    true
            }
        }

        it("never fences or crashes the ceiling twice in a row") {
            listOf(D1000Action.FENCE, D1000Action.CEILING).forEach { big ->
                actions { d1000Context(it) }.contains(big) shouldBe true
                actions { d1000Context(it, last = big) }.contains(big) shouldBe false
            }
        }

        it("weighs the Ceiling Crash 15 more when X is above the body") {
            D1000Director.weight(D1000Action.CEILING, d1000Context(playerAbove = true)) shouldBe
                D1000Director.weight(D1000Action.CEILING, d1000Context()) + 15
        }

        it("weighs a Bracket 10 more when X is near just after a slam") {
            val mid = d1000Context(distance = D1000Distance.MID, last = D1000Action.CROUCH)
            D1000Director.weight(D1000Action.BRACKET, mid.copy(distance = D1000Distance.NEAR)) shouldBe
                D1000Director.weight(D1000Action.BRACKET, mid) + 10
        }

        it("dives only when X is near the eye, and then most of the time") {
            actions { d1000Context(it) }.contains(D1000Action.DIVE) shouldBe false
            val dives = ROLLS.count { plan(d1000Context(it, playerNearEye = true)).action == D1000Action.DIVE }
            (dives > ROLLS.count() / 4) shouldBe true
        }

        it("plays the wall volley only while X spends much of his time in the air, and then with weight ${D1000Director.WALL_WEIGHT}") {
            listOf(D1000Tier.NORMAL, D1000Tier.OVERDRIVE).forEach { tier ->
                actions { d1000Context(it, tier) }.contains(D1000Action.WALL) shouldBe false
                actions { d1000Context(it, tier, playerAirborne = true) }.contains(D1000Action.WALL) shouldBe true
                D1000Director.weight(D1000Action.WALL, d1000Context(tier = tier, playerAirborne = true)) shouldBe D1000Director.WALL_WEIGHT
            }
        }

        it("brackets more while X is on a wall, and spreads more while X is above the body") {
            D1000Director.weight(D1000Action.BRACKET, d1000Context(playerOnWall = true)) shouldBe
                D1000Director.weight(D1000Action.BRACKET, d1000Context()) + 10
            D1000Director.weight(D1000Action.SPREAD, d1000Context(playerAbove = true)) shouldBe
                D1000Director.weight(D1000Action.SPREAD, d1000Context()) + 10
        }

        it("links a fence or a sweep to a ram, and a slam to a bracket") {
            D1000Director.comboOf(D1000Action.FENCE) shouldBe D1000Action.RAM
            D1000Director.comboOf(D1000Action.SWEEP) shouldBe D1000Action.RAM
            D1000Director.comboOf(D1000Action.CROUCH) shouldBe D1000Action.BRACKET
        }

        it("links a ram to a slam and a summon to a ram") {
            D1000Director.comboOf(D1000Action.RAM) shouldBe D1000Action.CROUCH
            D1000Director.comboOf(D1000Action.SUMMON) shouldBe D1000Action.RAM
        }

        it("has an action for every roll in every context") {
            CONTEXTS.forEach { context -> ROLLS.forEach { plan(context(it)) } }
        }
    }
})
