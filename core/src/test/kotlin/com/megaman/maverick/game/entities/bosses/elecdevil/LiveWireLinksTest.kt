package com.megaman.maverick.game.entities.bosses.elecdevil

import com.maverick.game.engine.common.random.SeededRandom
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_BODY_WIDTH
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.state.Performance
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.util.Random

private const val SEEDS = 200
private const val SPEED_STEPS = 20
private const val CROUCH_HEIGHT = MEGAMAN_BODY_WIDTH
private const val TIME_TOLERANCE = 0.0001f

private val TUNING_ROWS = listOf(
    ElecDevilTuning.NORMAL, ElecDevilTuning.NORMAL_PHASE_2, ElecDevilTuning.HARD, ElecDevilTuning.HARD_PHASE_2
)

private fun circleBottom(row: Int) = (row + 0.5f) * ElecDevilConstants.PIECE_HEIGHT - LiveWireLinks.PIECE_RADIUS

private fun linkedIndices(links: BooleanArray, pieces: Int) = (1 until pieces).filter { links[it] }

class LiveWireLinksTest : DescribeSpec({

    val pieces = ElecDevilConstants.PIECE_COUNT
    val candidates = IntArray(pieces)
    val links = BooleanArray(pieces)
    val startRows = IntArray(pieces)

    describe("the link choice") {
        it("links only consecutive pieces of one band, at most the links of the row, with the gap of the row") {
            TUNING_ROWS.forEach { row ->
                repeat(SEEDS) { seed ->
                    val random = SeededRandom(Random(seed.toLong()))
                    ElecDevilConstants.getPieceQueue(random)
                        .forEachIndexed { index, (startRow, _) -> startRows[index] = startRow }
                    LiveWireLinks.choose(
                        pieces, row.liveWireLinks, row.liveWireMinGap, startRows, random, candidates, links
                    )
                    links[0] shouldBe false
                    val linked = linkedIndices(links, pieces)
                    withClue("row with ${row.liveWireLinks} links, seed $seed: $linked, rows ${startRows.toList()}") {
                        linked.size shouldBeLessThanOrEqual row.liveWireLinks
                        linked.zipWithNext().forEach { (a, b) -> (b - a) shouldBeGreaterThan row.liveWireMinGap }
                        linked.forEach { index ->
                            LiveWireLinks.bandOf(startRows[index - 1]) shouldBe LiveWireLinks.bandOf(startRows[index])
                        }
                    }
                }
            }
        }

        it("gives every link of the row when all pieces are in one band") {
            listOf(0, ElecDevilConstants.PIECE_ROWS - 1).forEach { bandRow ->
                startRows.fill(bandRow)
                TUNING_ROWS.forEach { row ->
                    repeat(SEEDS) { seed ->
                        LiveWireLinks.choose(
                            pieces, row.liveWireLinks, row.liveWireMinGap, startRows,
                            SeededRandom(Random(seed.toLong())), candidates, links
                        )
                        withClue("rows all $bandRow, row with ${row.liveWireLinks} links, seed $seed") {
                            linkedIndices(links, pieces).size shouldBe row.liveWireLinks
                        }
                    }
                }
            }
        }

        it("gives fewer links than the row when no pair or too few pairs have both pieces in one band") {
            for (index in 0 until pieces) startRows[index] = if (index % 2 == 0) 0 else ElecDevilConstants.PIECE_ROWS - 1
            TUNING_ROWS.forEach { row ->
                LiveWireLinks.choose(
                    pieces, row.liveWireLinks, row.liveWireMinGap, startRows, SeededRandom(Random(1L)), candidates,
                    links
                )
                linkedIndices(links, pieces).size shouldBe 0
            }
            startRows[0] = ElecDevilConstants.PIECE_ROWS - 1
            startRows[1] = 0
            TUNING_ROWS.forEach { row ->
                LiveWireLinks.choose(
                    pieces, row.liveWireLinks, row.liveWireMinGap, startRows, SeededRandom(Random(1L)), candidates,
                    links
                )
                withClue("row with ${row.liveWireLinks} links") {
                    linkedIndices(links, pieces) shouldBe listOf(2)
                    1 shouldBeLessThan row.liveWireLinks
                }
            }
        }
    }

    describe("the bands") {
        it("puts CROUCH_UNDER_MIN_ROW at the lowest row whose piece circle clears a crouching Megaman") {
            withClue("bottom of a row-${LiveWireLinks.CROUCH_UNDER_MIN_ROW} piece") {
                circleBottom(LiveWireLinks.CROUCH_UNDER_MIN_ROW) shouldBeGreaterThan CROUCH_HEIGHT
            }
            withClue("bottom of a row-${LiveWireLinks.CROUCH_UNDER_MIN_ROW - 1} piece") {
                circleBottom(LiveWireLinks.CROUCH_UNDER_MIN_ROW - 1) shouldBeLessThanOrEqual CROUCH_HEIGHT
            }
            (0 until ElecDevilConstants.PIECE_ROWS).forEach { row ->
                val expected = if (row < LiveWireLinks.CROUCH_UNDER_MIN_ROW) LiveWireBand.JUMP_OVER
                else LiveWireBand.CROUCH_UNDER
                LiveWireLinks.bandOf(row) shouldBe expected
            }
        }

        it("keeps JUMP_CLEAR_TIME within the time a full jump keeps the feet above a row-1 piece") {
            val top = (LiveWireLinks.CROUCH_UNDER_MIN_ROW - 0.5f) * ElecDevilConstants.PIECE_HEIGHT +
                LiveWireLinks.PIECE_RADIUS
            val ppm = ConstVals.PPM.toFloat()
            val step = Performance.MEDIUM.fixedStep
            val body = Body(BodyType.DYNAMIC)
            body.physics.defaultFrictionOnSelf.set(ConstVals.STANDARD_RESISTANCE_X, ConstVals.STANDARD_RESISTANCE_Y)
            body.physics.frictionOnSelf.set(body.physics.defaultFrictionOnSelf)
            body.physics.velocityClamp.set(MegamanValues.CLAMP_X * ppm, MegamanValues.CLAMP_Y * ppm)
            body.physics.velocity.set(0f, MegamanValues.JUMP_VEL * ppm)
            var rising = true
            var time = 0f
            var aboveFrom = -1f
            var aboveUntil = -1f
            while (time < 3f) {
                if (body.physics.velocity.y <= 0f) rising = false
                val gravity = if (rising) MegamanValues.JUMP_GRAVITY else MegamanValues.FALL_GRAVITY
                body.physics.gravity.set(0f, gravity * ppm)
                body.process(step)
                time += step
                val feet = body.getY() / ppm
                if (feet > top) {
                    if (aboveFrom < 0f) aboveFrom = time
                    aboveUntil = time
                }
                if (feet <= 0f) break
            }
            val above = aboveUntil - aboveFrom
            withClue("feet above $top tiles for $above s") {
                aboveFrom shouldBeGreaterThan 0f
                above shouldBeGreaterThanOrEqual LiveWireLinks.JUMP_CLEAR_TIME
            }
        }

        it("lets a JUMP_OVER pair pass a player in at most JUMP_CLEAR_TIME - JUMP_TIMING_SLACK at every speed") {
            val limit = LiveWireLinks.JUMP_CLEAR_TIME - LiveWireLinks.JUMP_TIMING_SLACK
            val reach = 2f * LiveWireLinks.PIECE_RADIUS + MEGAMAN_BODY_WIDTH
            TUNING_ROWS.forEach { row ->
                (0..SPEED_STEPS).forEach { speedStep ->
                    val speed = row.pieceMinSpeed + (row.pieceMaxSpeed - row.pieceMinSpeed) * speedStep / SPEED_STEPS
                    listOf(0f, ElecDevilConstants.PIECE_WIDTH).forEach { trailBehind ->
                        val delay = LiveWireLinks.jumpOverPassGap(speed) - trailBehind / speed
                        val pass = maxOf(0f, delay) + (trailBehind + reach) / speed
                        withClue("speed $speed, trail $trailBehind tiles behind: delay $delay, pass $pass s") {
                            pass shouldBeLessThanOrEqual limit + TIME_TOLERANCE
                        }
                    }
                }
            }
        }
    }
})
