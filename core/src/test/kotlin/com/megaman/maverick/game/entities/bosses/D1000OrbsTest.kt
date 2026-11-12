package com.megaman.maverick.game.entities.bosses

import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.entities.bosses.d1000.D1000BombPattern
import com.megaman.maverick.game.entities.bosses.d1000.D1000Orbs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Role
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import com.badlogic.gdx.utils.Array
import kotlin.math.abs

private const val PPM = 32f

class D1000OrbsTest : DescribeSpec({

    val arena = GameRectangle(0f, 0f, 21.5f * PPM, 12f * PPM)

    describe("the role scripts") {
        it("the Fence sweeps every lane and hunts X between two lanes") {
            D1000Orbs.roles(D1000BombPattern.FENCE, 0, 4, Array()).toList() shouldBe listOf(
                D1000Role.SWEEP, D1000Role.HUNT, D1000Role.SWEEP, D1000Role.HUNT, D1000Role.SWEEP, D1000Role.HUNT, D1000Role.SWEEP
            )
        }

        it("every Bracket script hunts X at least once, and a longer count adds hunts") {
            for (script in 0 until D1000Orbs.scriptCount(D1000BombPattern.BRACKET)) {
                val roles = D1000Orbs.roles(D1000BombPattern.BRACKET, script, 4, Array())
                roles.size shouldBe 4
                roles.contains(D1000Role.HUNT, true) shouldBe true
                roles[3] shouldBe D1000Role.HUNT
            }
        }
    }

    describe("the lanes") {
        val noBody = -1f to -1f

        it("a cut goes 3 tiles ahead of a running X, or to the side with more room when X stands") {
            D1000Orbs.cutSide(arena, 5f * PPM, 4f * PPM) shouldBe 1
            D1000Orbs.cutSide(arena, 5f * PPM, -4f * PPM) shouldBe -1
            D1000Orbs.cutSide(arena, 5f * PPM, 0f) shouldBe 1
            D1000Orbs.cutSide(arena, 18f * PPM, 0f) shouldBe -1
            D1000Orbs.sideLane(arena, 5f * PPM, 1, noBody.first, noBody.second) shouldBe (8f * PPM plusOrMinus 0.01f)
        }

        it("a feint finds no lane when every lane near X is under the body or beside an earlier mark") {
            D1000Orbs.feintLane(arena, 8f * PPM, floatArrayOf(5f * PPM), 1, 9f * PPM, 14f * PPM).isNaN() shouldBe true
        }

        it("a cut that leaves the arena or lands under the body goes to the other side") {
            D1000Orbs.sideLane(arena, 2f * PPM, -1, noBody.first, noBody.second) shouldBe (5f * PPM plusOrMinus 0.01f)
            D1000Orbs.sideLane(arena, 8f * PPM, 1, 9f * PPM, 14f * PPM) shouldBe (5f * PPM plusOrMinus 0.01f)
        }

        it("a feint lands 2 to 3.5 tiles from X, never under the body, and 3 tiles from the earlier marks") {
            val used = floatArrayOf(5.5f * PPM)
            val low = 12f * PPM
            val high = 16f * PPM
            repeat(100) {
                val x = D1000Orbs.feintLane(arena, 8f * PPM, used, 1, low, high)
                val off = abs(x - 8f * PPM)
                withClue("feint at ${x / PPM} tiles") {
                    (off >= D1000Orbs.FEINT_MIN - 0.01f && off <= D1000Orbs.FEINT_MAX + 0.01f) shouldBe true
                    D1000Orbs.underBody(x, low, high) shouldBe false
                    (abs(x - 5.5f * PPM) >= D1000Orbs.FEINT_SPREAD) shouldBe true
                }
            }
        }

        it("a wall height hunts X's lead height and a cut goes 2.5 tiles above or below, inside the band") {
            val tuning = com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning.NORMAL
            val low = 3f * PPM
            val high = 8f * PPM
            D1000Orbs.aimHeight(5f * PPM, 2f * PPM, tuning, low, high) shouldBe (5f * PPM + 2f * PPM * tuning.leadTime plusOrMinus 0.01f)
            D1000Orbs.aimHeight(1f * PPM, 0f, tuning, low, high) shouldBe (low plusOrMinus 0.01f)
            D1000Orbs.cutHeight(5f * PPM, 4f * PPM, low, high) shouldBe (7.5f * PPM plusOrMinus 0.01f)
            D1000Orbs.cutHeight(7f * PPM, 4f * PPM, low, high) shouldBe (4.5f * PPM plusOrMinus 0.01f)
            D1000Orbs.wallX(arena, 5f * PPM) shouldBe arena.getMaxX()
            D1000Orbs.wallX(arena, 15f * PPM) shouldBe arena.getX()
        }

        it("the Fence lanes start at X's lane, sweep to the far wall, then come back for the near side") {
            val out = FloatArray(D1000Orbs.MAX_LANES)
            val count = D1000Orbs.fenceLanes(arena, 5f * PPM, out)
            out.take(count).map { it / PPM }.zip(listOf(5f, 9.1f, 13.2f, 17.3f, 0.9f)).forEach { (lane, want) ->
                lane shouldBe (want plusOrMinus 0.05f)
            }
            count shouldBe 5
        }
    }
})
