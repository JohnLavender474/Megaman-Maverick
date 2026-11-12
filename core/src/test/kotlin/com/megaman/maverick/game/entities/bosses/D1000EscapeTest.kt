package com.megaman.maverick.game.entities.bosses

import com.megaman.maverick.game.entities.megamanx.MegamanXValues
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.sqrt
import com.megaman.maverick.game.entities.bosses.d1000.D1000Escape
import com.megaman.maverick.game.entities.bosses.d1000.D1000Moves
import com.megaman.maverick.game.entities.bosses.d1000.D1000XMoves

private const val T = RIG_PPM
private const val WIDTH = 10f * T

class D1000EscapeTest : DescribeSpec({

    fun escape(build: D1000Escape.() -> Unit) = D1000Escape().apply(build)

    fun D1000Escape.from(tiles: Float, moves: D1000Moves = D1000Moves.NORMAL) = hasEscape(tiles * T, 0f, WIDTH, moves)

    describe("the floor model") {
        it("finds an escape on an open floor") {
            escape { }.from(5f) shouldBe true
        }

        it("walks X out of a pillar that comes after the reaction time") {
            escape { add(4.5f * T, 5.5f * T, 1f, 2f) }.from(5f) shouldBe true
        }

        it("finds no escape when a wall, a lane and a pillar close every place") {
            escape {
                add(2f * T, 3f * T, 0f, 4f)
                add(0.5f * T, 1.5f * T, 1f, 2f)
            }.from(1f) shouldBe false
        }

        it("keeps a stunned X still: a hazard in the stun has no escape, the same hazard after it has one") {
            escape {
                stun(0f, 1.2f)
                add(4.5f * T, 5.5f * T, 0.5f, 1f)
            }.from(5f) shouldBe false
            escape { add(4.5f * T, 5.5f * T, 0.5f, 1f) }.from(5f) shouldBe true
        }

        it("lets X jump a floor wave, but not while stunned") {
            escape { add(0f, WIDTH, 1f, 1.5f, jumpable = true) }.from(5f) shouldBe true
            escape {
                add(0f, WIDTH, 1f, 1.5f, jumpable = true)
                stun(0.9f, 1.6f)
            }.from(5f) shouldBe false
        }

        it("does not count a hazard that covers X before X can react: the boss cannot change it") {
            escape { add(4.5f * T, 5.5f * T, 0.1f, 0.6f) }.from(5f) shouldBe true
        }

        it("needs a wider gap on Normal than on Hard") {
            val gap = escape {
                add(0f, 2f * T, 0.5f, 3f)
                add(3.08f * T, WIDTH, 0.5f, 3f)
            }
            gap.from(2.54f, D1000Moves.NORMAL) shouldBe false
            gap.from(2.54f, D1000Moves.HARD) shouldBe true
        }

        it("reads a strip as free only when no hazard is in it in the time window") {
            val strip = escape { add(0f, 1f * T, 1f, 2f, fullHeight = true) }
            strip.free(0f, 1.5f * T, 0f, 0.9f) shouldBe true
            strip.free(0f, 1.5f * T, 0f, 1.5f) shouldBe false
            strip.free(2f * T, 3f * T, 0f, 4f) shouldBe true
            escape { add(0f, 1f * T, 1f, 2f) }.free(0f, 1.5f * T, 0f, 4f) shouldBe true
        }

        it("starts X at a later time when asked: hazards before that time do not count") {
            val late = escape { add(0.5f * T, 1.5f * T, 0.3f, 1f) }
            late.from(1f) shouldBe false
            late.hasEscape(1f * T, 0f, WIDTH, D1000Moves.NORMAL, 1.1f) shouldBe true
        }
    }

    describe("X's moves") {
        it("climbs as high as the jump in the jump time") {
            val jump = MegamanXValues.JUMP_SPEED
            val gravity = -MegamanXValues.JUMP_GRAVITY
            val time = (jump - sqrt(jump * jump - 2f * gravity)) / gravity
            D1000XMoves.climbTime(1f * T) shouldBe (time plusOrMinus 0.01f)
        }

        it("needs wall kicks above the jump height, so a higher climb takes longer") {
            val peak = MegamanXValues.JUMP_SPEED * MegamanXValues.JUMP_SPEED / (-2f * MegamanXValues.JUMP_GRAVITY)
            (D1000XMoves.climbTime((peak + 1f) * T) > D1000XMoves.climbTime(peak * T)) shouldBe true
            (D1000XMoves.climbTime(6f * T) > D1000XMoves.climbTime(4f * T)) shouldBe true
            (D1000XMoves.climbTime(6f * T) < 2f) shouldBe true
        }

        it("falls with X's fall gravity") {
            D1000XMoves.fallTime(3f * T) shouldBe (sqrt(2f * 3f / -MegamanXValues.FALL_GRAVITY) plusOrMinus 0.001f)
        }
    }
})
