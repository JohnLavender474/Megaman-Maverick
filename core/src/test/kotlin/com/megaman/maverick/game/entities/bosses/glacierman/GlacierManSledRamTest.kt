package com.megaman.maverick.game.entities.bosses.glacierman

import com.maverick.game.engine.common.enums.Facing
import com.megaman.maverick.game.entities.projectiles.Snowhead
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

private const val TOLERANCE = 2f * STEP

private val PHASE_1_ROWS = listOf(
    TuningRow("normal", hard = false, phase2 = false),
    TuningRow("hard", hard = true, phase2 = false)
)
private val PHASE_2_ROWS = listOf(
    TuningRow("normal phase 2", hard = false, phase2 = true),
    TuningRow("hard phase 2", hard = true, phase2 = true)
)

private fun farTotal(row: GlacierManTuning) = row.iceBlastWeight * row.farIceBlastScale + row.duckWeight +
    row.sledWeight + row.jumpWeight + row.icicleRainWeight + row.sledRamWeight

private fun ramFraction(row: GlacierManTuning, ramScale: Float = 1f, total: Float = farTotal(row)) =
    (row.iceBlastWeight * row.farIceBlastScale + row.duckWeight + row.sledWeight + row.icicleRainWeight +
        row.sledRamWeight * ramScale / 2f) / total

class GlacierManSledRamTest : DescribeSpec({

    lateinit var rig: GlacierManRig

    afterEach { rig.dispose() }

    fun start(row: TuningRow = PHASE_2_ROWS[0]) {
        rig = GlacierManRig(hard = row.hard)
        rig.startFight(phase2 = row.phase2)
    }

    fun ram(row: GlacierManTuning = GlacierManTuning.NORMAL_PHASE_2, ramScale: Float = 1f,
            total: Float = farTotal(row)) {
        rig.leaveStand(ramFraction(row, ramScale, total)) shouldBe "brake_flash"
    }

    fun clampX() = rig.bossBody.physics.velocityClamp.x

    describe("the pick") {
        PHASE_1_ROWS.forEach { row ->
            it("${row.name}: sledRamWeight is 0, so the ram is not in the pick") {
                row.tuning.sledRamWeight shouldBe 0f
                start(row)
                rig.leaveStand(0.999f) shouldBe "fall_flash"
                rig.lastPickTotal() shouldBe (farTotal(row.tuning) plusOrMinus 0.001f)
            }
        }

        PHASE_2_ROWS.forEach { row ->
            it("${row.name}: a roll in the sledRamWeight share starts Sled Ram with brake_flash") {
                (row.tuning.sledRamWeight > 0f) shouldBe true
                start(row)
                ram(row.tuning)
                rig.lastPickTotal() shouldBe (farTotal(row.tuning) plusOrMinus 0.001f)
                rig.untilKey("sled", 2f)
            }
        }
    }

    describe("the tell and the speed") {
        it("the ram speeds: 12 tiles/s normal, 14 hard") {
            GlacierManTuning.NORMAL_PHASE_2.sledRamSpeed shouldBe 12f
            GlacierManTuning.HARD_PHASE_2.sledRamSpeed shouldBe 14f
            rig = GlacierManRig()
        }

        PHASE_2_ROWS.forEach { row ->
            val tuning = row.tuning
            it("${row.name}: brake_flash for sledRamTellDur with no move, then sled at sledRamSpeed toward " +
                "the player") {
                start(row)
                ram(tuning)
                val x = rig.bossBody.getX()
                var moved = false
                val tell = rig.secondsWhile(2f, each = {
                    if (rig.bossBody.physics.velocity.x != 0f || rig.bossBody.getX() != x) moved = true
                }) { rig.animKey == "brake_flash" }
                tell shouldBe (tuning.sledRamTellDur plusOrMinus TOLERANCE)
                moved shouldBe false
                rig.animKey shouldBe "sled"
                rig.tick()
                rig.boss.facing shouldBe Facing.LEFT
                rig.bossBody.physics.velocity.x shouldBe (-tuning.sledRamSpeed * T plusOrMinus 0.01f)
                clampX() shouldBe (tuning.sledRamSpeed * T plusOrMinus 0.01f)
                val before = rig.bossBody.getX()
                rig.tick()
                (before - rig.bossBody.getX()) shouldBe (tuning.sledRamSpeed * T * STEP plusOrMinus 0.05f)
            }
        }
    }

    describe("the walls") {
        it("it reverses once at the first wall, fires no snowball, and at the second wall goes to brake, " +
            "stop and stand") {
            start()
            rig.placeBoss(8f)
            rig.place(4f)
            ram()
            rig.untilKey("sled", 2f)
            val shots = rig.spawnsOf<Snowhead>().size
            val keys = mutableListOf("sled")
            val facings = mutableListOf(rig.boss.facing)
            var touchedLeft = false
            var maxXAtBrake = 0f
            withClue("back to STAND") {
                rig.ticksUntil(6f, each = {}) {
                    val key = rig.animKey!!
                    if (key != keys.last()) {
                        keys.add(key)
                        if (key == "brake") maxXAtBrake = rig.bossBody.getMaxX()
                    }
                    if (rig.boss.facing != facings.last()) facings.add(rig.boss.facing)
                    if (rig.bossBody.getX() <= LEFT_INNER + 0.01f) touchedLeft = true
                    key == "stand"
                } shouldBe true
            }
            keys shouldBe listOf("sled", "brake", "stop", "stand")
            touchedLeft shouldBe true
            facings shouldBe listOf(Facing.LEFT, Facing.RIGHT)
            maxXAtBrake shouldBe (RIGHT_INNER plusOrMinus 0.01f)
            rig.spawnsOf<Snowhead>().size shouldBe shots
        }

        it("after 3 s with no second wall, it goes to brake") {
            start()
            rig.placeBoss(24f)
            rig.place(3f)
            ram()
            rig.untilKey("sled", 2f)
            val shots = rig.spawnsOf<Snowhead>().size
            val begin = rig.time
            val facings = mutableListOf(rig.boss.facing)
            withClue("the brake") {
                rig.ticksUntil(5f, each = {
                    if (rig.boss.facing != facings.last()) facings.add(rig.boss.facing)
                }) { rig.baseKey == "brake" } shouldBe true
            }
            (rig.time - begin) shouldBe (3f plusOrMinus TOLERANCE)
            facings shouldBe listOf(Facing.LEFT, Facing.RIGHT)
            (rig.bossBody.getMaxX() < RIGHT_INNER - T) shouldBe true
            rig.spawnsOf<Snowhead>().size shouldBe shots
        }
    }

    describe("the velocity clamp") {
        it("the ram speed only in Sled Ram; 10 tiles/s in the tell, the brake, the stop and the stand after it") {
            start(PHASE_2_ROWS[1])
            clampX() shouldBe (10f * T plusOrMinus 0.01f)
            ram(GlacierManTuning.HARD_PHASE_2)
            clampX() shouldBe (10f * T plusOrMinus 0.01f)
            rig.untilKey("sled", 2f)
            clampX() shouldBe (14f * T plusOrMinus 0.01f)
            rig.untilKey("brake", 4f)
            clampX() shouldBe (10f * T plusOrMinus 0.01f)
            rig.untilStand()
            clampX() shouldBe (10f * T plusOrMinus 0.01f)
        }

        it("10 tiles/s in the normal sled and in the jump") {
            start()
            val row = GlacierManTuning.NORMAL_PHASE_2
            val sledFraction = (row.iceBlastWeight * row.farIceBlastScale + row.duckWeight + row.sledWeight / 2f) /
                farTotal(row)
            rig.leaveStand(sledFraction) shouldBe "brake_flash"
            rig.untilKey("sled", 2f)
            clampX() shouldBe (10f * T plusOrMinus 0.01f)
            rig.untilStand()
            rig.placeBoss(BOSS_START_X)
            rig.leaveStand(0.999f) shouldBe "fall_flash"
            rig.untilKey("jump", 2f)
            clampX() shouldBe (10f * T plusOrMinus 0.01f)
        }
    }

    describe("the repeat rule") {
        it("Sled Ram gets x lastMoveScale after one ram and weight 0 after two") {
            start()
            val row = GlacierManTuning.NORMAL_PHASE_2
            ram()
            rig.untilStand()
            rig.placeBoss(BOSS_START_X)
            val second = farTotal(row) - row.sledRamWeight + row.sledRamWeight * row.lastMoveScale
            ram(row, ramScale = row.lastMoveScale, total = second)
            rig.lastPickTotal() shouldBe (second plusOrMinus 0.001f)
            rig.untilStand()
            rig.placeBoss(BOSS_START_X)
            rig.leaveStand(0.5f)
            rig.lastPickTotal() shouldBe (farTotal(row) - row.sledRamWeight plusOrMinus 0.001f)
        }
    }
})
