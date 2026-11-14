package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.motion.RotatingLine
import com.megaman.maverick.game.ConstVals
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 60f

class BigAssMaverickRobotMotionTest : DescribeSpec({

    val origin = Vector2(10f * PPM, 20f * PPM)

    describe("the ORBIT slot") {
        it("follows the path of the old RotatingLine for both hands") {
            HandSide.entries.forEach { side ->
                val speed = side.turn * BigAssMaverickRobotMotion.ORBIT_MIN_SPEED
                val line = RotatingLine(origin.cpy(), BigAssMaverickRobotMotion.ORBIT_RADIUS * PPM, speed, side.startDegrees)
                var traveled = 0f
                repeat(120) {
                    line.update(STEP)
                    traveled += BigAssMaverickRobotMotion.ORBIT_MIN_SPEED * STEP
                    val old = line.getMotionValue(Vector2())!!
                    val new = BigAssMaverickRobotMotion.orbitSlot(origin, side, traveled, Vector2())
                    withClue("$side at step $it") {
                        new.x shouldBe (old.x plusOrMinus 0.05f)
                        new.y shouldBe (old.y plusOrMinus 0.05f)
                    }
                }
            }
        }

        it("is at the start angle at t = 0 and again at the cycle end") {
            val left = BigAssMaverickRobotMotion.orbitSlot(origin, HandSide.LEFT, 0f, Vector2())
            left.x shouldBe (origin.x - 2f * PPM plusOrMinus 0.01f)
            left.y shouldBe (origin.y plusOrMinus 0.01f)
            val end = BigAssMaverickRobotMotion.orbitSlot(origin, HandSide.LEFT, BigAssMaverickRobotMotion.ORBIT_CYCLE, Vector2())
            end.x shouldBe (left.x plusOrMinus 0.01f)
            end.y shouldBe (left.y plusOrMinus 0.01f)
            val right = BigAssMaverickRobotMotion.orbitSlot(origin, HandSide.RIGHT, 0f, Vector2())
            right.x shouldBe (origin.x + 2f * PPM plusOrMinus 0.01f)
        }
    }

    describe("the HOVER slot") {
        it("is at the origin for the left hand and a quarter cycle on for the right hand at t = 0") {
            val left = BigAssMaverickRobotMotion.hoverSlot(origin, HandSide.LEFT, 0f, 1f, Vector2())
            left.x shouldBe (origin.x plusOrMinus 0.01f)
            left.y shouldBe (origin.y plusOrMinus 0.01f)
            val right = BigAssMaverickRobotMotion.hoverSlot(origin, HandSide.RIGHT, 0f, 1f, Vector2())
            right.x shouldBe (origin.x + 1f * PPM plusOrMinus 0.01f)
            right.y shouldBe (origin.y + 0.75f * PPM plusOrMinus 0.01f)
        }

        it("comes back to its start at the cycle end and stays inside its amplitude") {
            HandSide.entries.forEach { side ->
                val start = BigAssMaverickRobotMotion.hoverSlot(origin, side, 0f, 1.3f, Vector2())
                val end = BigAssMaverickRobotMotion.hoverSlot(origin, side, BigAssMaverickRobotMotion.HOVER_CYCLE, 1.3f, Vector2())
                end.x shouldBe (start.x plusOrMinus 0.05f)
                end.y shouldBe (start.y plusOrMinus 0.05f)
                var t = 0f
                while (t < BigAssMaverickRobotMotion.HOVER_CYCLE) {
                    val slot = BigAssMaverickRobotMotion.hoverSlot(origin, side, t, 1.3f, Vector2())
                    kotlin.math.abs(slot.x - origin.x) shouldBeLessThanOrEqual 1.3f * PPM + 0.01f
                    kotlin.math.abs(slot.y - origin.y) shouldBeLessThanOrEqual 0.75f * 1.3f * PPM + 0.01f
                    t += STEP
                }
            }
        }
    }

    describe("the mode schedule") {
        val schedule = gdxArrayOf(MotionMode.ORBIT, MotionMode.ORBIT, MotionMode.HOVER)

        it("switches the mode only at a cycle end, through BLEND") {
            val motion = BigAssMaverickRobotMotion()
            val speed = 180f
            var time = 0f
            while (time < 1.99f) {
                motion.update(STEP, speed, 1f, schedule, handsArrived = false)
                time += STEP
                motion.mode shouldBe MotionMode.ORBIT
            }
            while (time < 4.05f) {
                motion.update(STEP, speed, 1f, schedule, handsArrived = false)
                time += STEP
            }
            motion.mode shouldBe MotionMode.BLEND
            motion.nextMode shouldBe MotionMode.HOVER
        }

        it("leaves BLEND when the hands arrive, and after 1 s at most") {
            val motion = BigAssMaverickRobotMotion()
            repeat(1000) {
                if (motion.mode == MotionMode.BLEND) return@repeat
                motion.update(STEP, 360f, 1f, schedule, handsArrived = false)
            }
            motion.mode shouldBe MotionMode.BLEND
            motion.update(STEP, 360f, 1f, schedule, handsArrived = true)
            motion.mode shouldBe MotionMode.HOVER
            motion.hoverTime shouldBe 0f

            var time = 0f
            while (motion.mode == MotionMode.HOVER) {
                motion.update(STEP, 360f, 1f, schedule, handsArrived = false)
                time += STEP
            }
            time shouldBe (BigAssMaverickRobotMotion.HOVER_CYCLE plusOrMinus 2 * STEP)
            motion.nextMode shouldBe MotionMode.ORBIT
            var blend = 0f
            while (motion.mode == MotionMode.BLEND) {
                motion.update(STEP, 360f, 1f, schedule, handsArrived = false)
                blend += STEP
            }
            blend shouldBe (BigAssMaverickRobotMotion.BLEND_MAX_DUR plusOrMinus 2 * STEP)
            motion.mode shouldBe MotionMode.ORBIT
        }

        it("takes a new schedule from its first entry at the next cycle end") {
            val motion = BigAssMaverickRobotMotion()
            val phase2 = gdxArrayOf(MotionMode.HOVER, MotionMode.ORBIT)
            motion.update(STEP, 180f, 1f, schedule, handsArrived = false)
            repeat(30) { motion.update(STEP, 180f, 1f, phase2, handsArrived = false) }
            motion.mode shouldBe MotionMode.ORBIT
            repeat(100) { motion.update(STEP, 180f, 1f, phase2, handsArrived = false) }
            motion.mode shouldBe MotionMode.BLEND
            motion.nextMode shouldBe MotionMode.HOVER
        }
    }

    describe("the spring") {
        it("reaches a still target and keeps to its top speed") {
            val position = Vector2()
            val velocity = Vector2()
            val target = Vector2(5f * PPM, 0f)
            repeat(180) {
                BigAssMaverickRobotMotion.spring(position, velocity, target, 8f, STEP, velocity)
                velocity.len() shouldBeLessThanOrEqual 8f * PPM + 0.01f
                position.mulAdd(velocity, STEP)
            }
            position.x shouldBe (target.x plusOrMinus 0.25f * PPM)
        }
    }
})
