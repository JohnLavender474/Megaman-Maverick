package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.math.abs

private const val PPM = ConstVals.PPM.toFloat()

class BigAssMaverickRobotPartsTest : DescribeSpec({

    val arena = GameRectangle(318f * PPM, 55f * PPM, 14f * PPM, 11.75f * PPM)
    val low: (Float, Float) -> Float = { min, _ -> min }
    val high: (Float, Float) -> Float = { _, max -> max }

    fun aim(aim: OrbAim, center: Vector2, velocity: Vector2, random: (Float, Float) -> Float) =
        OrbAim.point(aim, center, velocity, arena, random, Vector2())

    fun inArena(point: Vector2) {
        point.x shouldBeGreaterThanOrEqual arena.getX()
        point.x shouldBeLessThanOrEqual arena.getMaxX()
        point.y shouldBeGreaterThanOrEqual arena.getY()
        point.y shouldBeLessThanOrEqual arena.getMaxY()
    }

    describe("the orb aim points") {
        val center = Vector2(325f * PPM, 56f * PPM)

        it("CURRENT is the player center") {
            aim(OrbAim.CURRENT, center, Vector2(5f * PPM, 0f), low) shouldBe center
        }

        it("LEAD is 1.25 s ahead on x only, with y kept near the floor") {
            val point = aim(OrbAim.LEAD, Vector2(325f * PPM, 62f * PPM), Vector2(2f * PPM, 9f * PPM), low)
            point.x shouldBe (327.5f * PPM plusOrMinus 0.01f)
            point.y shouldBe (58f * PPM plusOrMinus 0.01f)
        }

        it("NEAR stays within 1.5 tiles of the player on x") {
            listOf(low, high).forEach { random ->
                val point = aim(OrbAim.NEAR, center, Vector2(), random)
                abs(point.x - center.x) shouldBeLessThanOrEqual 1.5f * PPM + 0.01f
                point.y shouldBe center.y
            }
        }

        it("WIDE lands 3 to 5 tiles to a side, 1 tile over the floor") {
            listOf(low, high).forEach { random ->
                val point = aim(OrbAim.WIDE, center, Vector2(), random)
                val offset = abs(point.x - center.x)
                offset shouldBeGreaterThanOrEqual 3f * PPM - 0.01f
                offset shouldBeLessThanOrEqual 5f * PPM + 0.01f
                point.y shouldBe (56f * PPM plusOrMinus 0.01f)
            }
        }

        it("every kind stays inside the room") {
            val edge = Vector2(319f * PPM, 70f * PPM)
            OrbAim.entries.forEach { kind ->
                listOf(low, high).forEach { random ->
                    withClue(kind) { inArena(aim(kind, edge, Vector2(-20f * PPM, 0f), random)) }
                }
            }
        }
    }

    describe("the attack choice") {
        it("gives no attack with no free hand") {
            BigAssMaverickRobotTuning.NORMAL_PHASE_2.attackFor(0, freeHands = 0) shouldBe null
        }

        it("gives a pincer clap only with 2 free hands") {
            val tuning = BigAssMaverickRobotTuning.NORMAL_PHASE_2
            val two = (0 until tuning.totalWeight(2)).map { tuning.attackFor(it, 2) }
            two.count { it == HandAttack.PINCER_CLAP } shouldBe tuning.clapWeight
            val one = (0 until tuning.totalWeight(1)).map { tuning.attackFor(it, 1) }
            one.contains(HandAttack.PINCER_CLAP) shouldBe false
            one.count { it == HandAttack.PUNCH } shouldBe tuning.punchWeight
        }

        it("has no clap in phase 1") {
            val tuning = BigAssMaverickRobotTuning.NORMAL
            (0 until tuning.totalWeight(2)).map { tuning.attackFor(it, 2) }.contains(HandAttack.PINCER_CLAP) shouldBe false
        }

        it("picks the row by difficulty and phase") {
            BigAssMaverickRobotTuning.of(hard = false, phase = 1) shouldBe BigAssMaverickRobotTuning.NORMAL
            BigAssMaverickRobotTuning.of(hard = false, phase = 2) shouldBe BigAssMaverickRobotTuning.NORMAL_PHASE_2
            BigAssMaverickRobotTuning.of(hard = true, phase = 1) shouldBe BigAssMaverickRobotTuning.HARD
            BigAssMaverickRobotTuning.of(hard = true, phase = 2) shouldBe BigAssMaverickRobotTuning.HARD_PHASE_2
        }
    }

    describe("the partner choreography") {
        val punch = Vector2(1f, 0f)

        fun offset(
            state: HandState,
            time: Float,
            dur: Float = 0.75f,
            ridden: Boolean = false,
            scale: Float = 1f,
            direction: Vector2 = punch
        ) = HandChoreography.partnerOffset(state, time, dur, direction, towardHead = 1f, ridden, scale, Vector2())

        it("braces down and toward the head over the wind-up") {
            offset(HandState.WIND_UP, 0f).len() shouldBe (0f plusOrMinus 0.001f)
            val end = offset(HandState.WIND_UP, 0.75f)
            end.x shouldBe (0.5f * PPM plusOrMinus 0.01f)
            end.y shouldBe (-1f * PPM plusOrMinus 0.01f)
        }

        it("counters opposite to the punch, with the peak at 0.15 s") {
            val peak = offset(HandState.LAUNCH, 0.15f)
            peak.x shouldBe (-0.75f * PPM plusOrMinus 0.01f)
            offset(HandState.LAUNCH, 0.05f).x shouldBeGreaterThanOrEqual peak.x
        }

        it("settles back to the slot over the recoil") {
            offset(HandState.RECOIL, 0f, dur = 0.3f).x shouldBe (-0.75f * PPM plusOrMinus 0.01f)
            offset(HandState.RECOIL, 0.3f, dur = 0.3f).len() shouldBe (0f plusOrMinus 0.01f)
        }

        it("lifts, dips and jolts with the slam") {
            offset(HandState.RISE, 0.3f).y shouldBe (0.75f * PPM plusOrMinus 0.01f)
            offset(HandState.HANG, 0.2f).y shouldBe (0.75f * PPM plusOrMinus 0.01f)
            abs(offset(HandState.HANG, 0.02f).x) shouldBe (2f plusOrMinus 0.01f)
            offset(HandState.FALL, 0.1f, direction = Vector2(0f, -1f)).y shouldBe (-1f * PPM plusOrMinus 0.01f)
            offset(HandState.REST, 0.1f).y shouldBe (0.25f * PPM plusOrMinus 0.01f)
            offset(HandState.REST, 0.5f).y shouldBe (0f plusOrMinus 0.01f)
        }

        it("dips by the vertical part of the dive direction only") {
            offset(HandState.FALL, 0.1f, direction = Vector2(-1f, 0f)).y shouldBe (0f plusOrMinus 0.01f)
            offset(HandState.FALL, 0.1f, direction = Vector2(1f, 0f)).y shouldBe (0f plusOrMinus 0.01f)
            offset(HandState.FALL, 0.1f, direction = Vector2(1f, -1f).nor()).y shouldBe
                (-0.7071f * PPM plusOrMinus 0.01f)
        }

        it("gives no offset while the attacker returns or idles") {
            offset(HandState.RETURN, 0.1f).len() shouldBe 0f
            offset(HandState.IDLE, 0.1f).len() shouldBe 0f
        }

        it("scales to 0.25 for a ridden partner and by the phase scale") {
            offset(HandState.WIND_UP, 0.75f, ridden = true).y shouldBe (-0.25f * PPM plusOrMinus 0.01f)
            offset(HandState.WIND_UP, 0.75f, scale = 1.25f).y shouldBe (-1.25f * PPM plusOrMinus 0.01f)
        }
    }

    describe("the hand state flags") {
        it("match the hand state table: damager and platform per state") {
            val damager = HandState.entries.filter { it.damager }.toSet()
            val platform = HandState.entries.filter { it.platform }.toSet()
            damager shouldBe setOf(
                HandState.FOLLOW, HandState.MOVE_TO_ORIGIN, HandState.REVEAL_ORBS, HandState.WIND_UP, HandState.LAUNCH,
                HandState.RECOIL, HandState.RISE, HandState.HANG, HandState.FALL, HandState.CLAP_ALIGN,
                HandState.CLAP_LOCK, HandState.CLAP_CLOSE, HandState.CLAP_IMPACT
            )
            platform shouldBe setOf(HandState.IDLE, HandState.REST, HandState.RETURN)
        }
    }

    describe("the shock wave row") {
        it("spawns a pillar each tile from 1 tile off the slam point to the wall, one each 0.08 s") {
            val row = ShockWaveRow()
            val spawns = Array<Float>()
            row.start(325f * PPM, 1f, life = 10f)
            row.start(325f * PPM, -1f, life = 10f)
            row.update(0.01f, 319f * PPM, 331f * PPM) { spawns.add(it / PPM) }
            spawns.toList() shouldBe listOf(326f, 324f)
            var time = 0.01f
            while (time < 1f) {
                row.update(0.01f, 319f * PPM, 331f * PPM) { spawns.add(it / PPM) }
                time += 0.01f
            }
            spawns.toList().sorted() shouldBe listOf(319f, 320f, 321f, 322f, 323f, 324f, 326f, 327f, 328f, 329f, 330f, 331f)
        }

        it("keeps 0.08 s between pillars of one wave") {
            val row = ShockWaveRow()
            var count = 0
            row.start(320f * PPM, 1f, life = 10f)
            repeat(20) { row.update(0.01f, 0f, 1000f * PPM) { count++ } }
            count shouldBe 3
        }

        it("makes a pillar each 0.08 s up to the wave life of each tuning row, and none after it, on a path with no end in reach") {
            listOf(
                BigAssMaverickRobotTuning.NORMAL to 7,
                BigAssMaverickRobotTuning.NORMAL_PHASE_2 to 8,
                BigAssMaverickRobotTuning.HARD to 8,
                BigAssMaverickRobotTuning.HARD_PHASE_2 to 11
            ).forEach { (tuning, pillars) ->
                val row = ShockWaveRow()
                val times = Array<Float>()
                val delta = 1f / 64f
                var time = 0f
                row.start(500f * PPM, 1f, tuning.waveLife)
                repeat(200) {
                    time += delta
                    row.update(delta, 0f, 1000f * PPM) { times.add(time) }
                }
                withClue("wave life ${tuning.waveLife}: pillar times ${times.toList()}") {
                    times.size shouldBe pillars
                    times.last() shouldBeLessThanOrEqual tuning.waveLife + delta
                }
            }
        }

        it("stops at the path end before the wave life ends") {
            val row = ShockWaveRow()
            var count = 0
            row.start(320f * PPM, 1f, life = 10f)
            repeat(300) { row.update(0.01f, 0f, 324f * PPM) { count++ } }
            count shouldBe 4
        }
    }

    describe("the tuning rows") {
        fun BigAssMaverickRobotTuning.timings() = listOf(
            attackWait, windUpDur, lockDur, restDur, waveLife, clapSpeed, orbGap, volleyDur, volleyDelay,
            launchMinSpeed, launchMaxSpeed
        )

        it("hold the values of 2026-11-15 for normal 1, normal 2, hard 1 and hard 2") {
            BigAssMaverickRobotTuning.NORMAL.timings() shouldBe
                listOf(3f, 0.75f, 0.5f, 1.2f, 0.5f, 12f, 1f, 4f, 3f, 6f, 9f)
            BigAssMaverickRobotTuning.NORMAL_PHASE_2.timings() shouldBe
                listOf(2.25f, 0.6f, 0.45f, 1f, 0.6f, 12f, 0.9f, 3f, 2f, 6f, 9f)
            BigAssMaverickRobotTuning.HARD.timings() shouldBe
                listOf(1.75f, 0.55f, 0.4f, 0.9f, 0.6f, 14f, 0.6f, 2f, 2f, 8f, 12f)
            BigAssMaverickRobotTuning.HARD_PHASE_2.timings() shouldBe
                listOf(1.25f, 0.45f, 0.35f, 0.7f, 0.8f, 14f, 0.4f, 2f, 1.5f, 8f, 12f)
        }

        it("hold the attack weights and the wave pairs of 2026-11-15") {
            fun BigAssMaverickRobotTuning.counts() = listOf(punchWeight, slamWeight, clapWeight, shockWavePairs)
            BigAssMaverickRobotTuning.NORMAL.counts() shouldBe listOf(2, 1, 0, 1)
            BigAssMaverickRobotTuning.NORMAL_PHASE_2.counts() shouldBe listOf(2, 2, 1, 1)
            BigAssMaverickRobotTuning.HARD.counts() shouldBe listOf(2, 1, 0, 1)
            BigAssMaverickRobotTuning.HARD_PHASE_2.counts() shouldBe listOf(2, 2, 2, 2)
        }
    }
})
