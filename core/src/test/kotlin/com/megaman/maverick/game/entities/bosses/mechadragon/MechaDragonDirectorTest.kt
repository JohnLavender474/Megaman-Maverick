package com.megaman.maverick.game.entities.bosses.mechadragon

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.random.GameRandom
import com.maverick.game.engine.common.random.SeededRandom
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.bosses.bigassmaverickrobot.OrbAim
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlin.math.abs

private const val PPM = ConstVals.PPM.toFloat()

class MechaDragonDirectorTest : DescribeSpec({

    fun rolls(charge: Int = 100, vararg percent: Int): GameRandom {
        val random = mockk<GameRandom>()
        every { random.nextInt(0, 100) } returns charge
        if (percent.isNotEmpty()) every { random.nextInt(0, 99) } returnsMany percent.toList()
        return random
    }

    describe("after a hover") {
        it("lands at the latest after the second hover of an air cycle, whatever the rolls say") {
            MechaDragonDirector.afterHover(2, chargeChance = 100, random = rolls(charge = 0, 99)) shouldBe
                MechaDragonMove.LAND
            MechaDragonDirector.afterHover(3, chargeChance = 0, random = rolls(charge = 100, 99)) shouldBe
                MechaDragonMove.LAND
        }

        it("charges when the charge roll is at or below the pity chance (the old rule)") {
            MechaDragonDirector.afterHover(1, chargeChance = 35, random = rolls(charge = 35, 99)) shouldBe
                MechaDragonMove.CHARGE
            MechaDragonDirector.afterHover(1, chargeChance = 35, random = rolls(charge = 36, 99)) shouldBe
                MechaDragonMove.HOVER
        }

        it("after one hover with no charge, lands on half of the rolls and hovers again on the rest") {
            MechaDragonDirector.afterHover(1, chargeChance = 0, random = rolls(100, 49)) shouldBe MechaDragonMove.LAND
            MechaDragonDirector.afterHover(1, chargeChance = 0, random = rolls(100, 50)) shouldBe MechaDragonMove.HOVER
        }

        it("never picks the same air move twice in a row past the cap: a seeded run lands at least every 2 hovers") {
            val random = SeededRandom(7L)
            var hovers = 0
            var chance = 0
            repeat(500) {
                hovers++
                val move = MechaDragonDirector.afterHover(hovers, chance, random)
                chance = MechaDragonDirector.chargeChanceAfter(move, chance)
                hovers.shouldBeLessThanOrEqualInt(MechaDragonDirector.MAX_HOVERS_PER_AIR_CYCLE)
                if (move == MechaDragonMove.LAND) hovers = 0
            }
        }
    }

    describe("the charge pity counter") {
        it("grows by 35 after each hover that does not charge, up to 100, and resets on a charge") {
            MechaDragonDirector.chargeChanceAfter(MechaDragonMove.HOVER, 0) shouldBe 35
            MechaDragonDirector.chargeChanceAfter(MechaDragonMove.LAND, 35) shouldBe 70
            MechaDragonDirector.chargeChanceAfter(MechaDragonMove.HOVER, 70) shouldBe 100
            MechaDragonDirector.chargeChanceAfter(MechaDragonMove.HOVER, 100) shouldBe 100
            MechaDragonDirector.chargeChanceAfter(MechaDragonMove.CHARGE, 70) shouldBe 0
        }
    }

    describe("after a breath") {
        it("chomps a player in the pocket on 70% of the rolls") {
            MechaDragonDirector.afterBreath(1, playerInPocket = true, panic = false, random = rolls(100, 69)) shouldBe
                MechaDragonMove.CHOMP
            MechaDragonDirector.afterBreath(1, playerInPocket = true, panic = false, random = rolls(100, 70, 99)) shouldBe
                MechaDragonMove.TAKEOFF
        }

        it("in panic, always chomps a player in the pocket") {
            MechaDragonDirector.afterBreath(1, playerInPocket = true, panic = true, random = rolls(100, 99, 99)) shouldBe
                MechaDragonMove.CHOMP
        }

        it("chomps only rarely when no player is in the pocket") {
            MechaDragonDirector.afterBreath(1, playerInPocket = false, panic = true, random = rolls(100, 14)) shouldBe
                MechaDragonMove.CHOMP
            MechaDragonDirector.afterBreath(1, playerInPocket = false, panic = true, random = rolls(100, 15, 99)) shouldBe
                MechaDragonMove.TAKEOFF
        }

        it("breathes a second time on 30% of the rolls, never a third time") {
            MechaDragonDirector.afterBreath(1, playerInPocket = false, panic = false, random = rolls(100, 99, 29)) shouldBe
                MechaDragonMove.BREATH
            MechaDragonDirector.afterBreath(1, playerInPocket = false, panic = false, random = rolls(100, 99, 30)) shouldBe
                MechaDragonMove.TAKEOFF
            MechaDragonDirector.afterBreath(2, playerInPocket = false, panic = false, random = rolls(100, 99, 0)) shouldBe
                MechaDragonMove.TAKEOFF
        }
    }

    describe("the aim mix of the volley and the drips") {
        val arena = GameRectangle(0f, 0f, 16f * PPM, 14f * PPM)
        val player = Vector2(8f * PPM, 3f * PPM)
        val velocity = Vector2(2f * PPM, 0f)
        val near: (Float, Float) -> Float = { _, max -> max }

        fun point(i: Int) = OrbAim.point(
            MechaDragonDirector.AIMS[i], player, velocity, MechaDragonDirector.AIM_LEAD_TIME, arena, near, Vector2()
        )

        it("aims the first ball at the player") {
            point(0) shouldBe player
        }

        it("leads the player by about 1 s with the second ball") {
            point(1).x shouldBe (10f * PPM plusOrMinus 0.01f)
        }

        it("puts the third ball near the player, not on the player") {
            val third = point(2)
            withClue(third) {
                (abs(third.x - player.x) > 0f) shouldBe true
                abs(third.x - player.x) shouldBeLessThanOrEqual OrbAim.NEAR_SPREAD * PPM + 0.01f
            }
        }
    }
})

private fun Int.shouldBeLessThanOrEqualInt(max: Int) = withClue("$this <= $max") { (this <= max) shouldBe true }
