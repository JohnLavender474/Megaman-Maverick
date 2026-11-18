package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.world.body.BodyLabel
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class JumpPlannerTest : DescribeSpec({

    val ppm = ConstVals.PPM.toFloat()
    val farSideX: (GameRectangle) -> Boolean = { it.getX() > 5f * ppm }

    lateinit var world: GroundTestWorld
    lateinit var plan: JumpPlan
    val landing = GameRectangle()

    fun capturing(goal: (GameRectangle) -> Boolean): (GameRectangle) -> Boolean = {
        val passed = goal(it)
        if (passed) landing.set(it)
        passed
    }

    beforeEach {
        world = GroundTestWorld()
        plan = JumpPlan()
    }

    fun planner(x: Float, y: Float): JumpPlanner {
        val owner = world.owner(x, y)
        val probe = GroundProbe(owner, world.services.levelState)
        return JumpPlanner(
            owner, probe, gravity = 37.5f, maxSpeedForward = 8f, maxSpeedUp = 18f, maxFlightTime = 2f,
            fixedStep = { 1f / 150f }
        )
    }

    fun gapLevel(farFloorX: Float = 6f) {
        world.block(0f, 0f, 3f, 1f)
        world.block(farFloorX, 0f, 6f, 1f)
        world.death(3f, -3f, farFloorX - 3f, 1f)
    }

    describe("plan with gravity down (Direction.UP)") {

        it("passes a clear arc over a 3-tile gap and lands on the far floor") {
            gapLevel()
            val passed =
                planner(2f, 1f).plan(Facing.RIGHT, Direction.UP, gdxArrayOf(Vector2(8f, 10f)), capturing(farSideX), plan)
            passed shouldBe true
            plan.worldVelocity shouldBe Vector2(8f * ppm, 10f * ppm)
            landing.getY() shouldBe 1f * ppm
            landing.getX() shouldBeGreaterThan 5f * ppm
        }

        it("chooses the first candidate that passes") {
            gapLevel()
            val candidates = gdxArrayOf(Vector2(2f, 10f), Vector2(8f, 10f))
            planner(2f, 1f).plan(Facing.RIGHT, Direction.UP, candidates, farSideX, plan) shouldBe true
            plan.worldVelocity shouldBe Vector2(8f * ppm, 10f * ppm)
        }

        it("fails an arc into a low ceiling") {
            gapLevel()
            world.block(2f, 2.5f, 5f, 1f)
            planner(2f, 1f).plan(Facing.RIGHT, Direction.UP, gdxArrayOf(Vector2(8f, 10f)), farSideX, plan) shouldBe false
        }

        it("fails a landing on DEATH") {
            gapLevel()
            world.death(6f, 1f, 6f, 0.5f)
            planner(2f, 1f).plan(Facing.RIGHT, Direction.UP, gdxArrayOf(Vector2(8f, 10f)), farSideX, plan) shouldBe false
        }

        it("fails a gap that is too wide") {
            gapLevel(farFloorX = 9f)
            planner(2f, 1f).plan(Facing.RIGHT, Direction.UP, gdxArrayOf(Vector2(8f, 10f)), farSideX, plan) shouldBe false
        }

        it("fails when the goal rejects the landing") {
            gapLevel()
            planner(2f, 1f).plan(Facing.RIGHT, Direction.UP, gdxArrayOf(Vector2(8f, 10f)), { false }, plan) shouldBe false
        }

        it("lands on a one-way platform after passing up through it") {
            world.block(0f, 0f, 10f, 1f)
            world.block(1f, 2.5f, 4f, 0.25f, label = BodyLabel.COLLIDE_DOWN_ONLY)
            val onPlatform: (GameRectangle) -> Boolean = { it.getY() == 2.75f * ppm }
            planner(2f, 1f).plan(Facing.RIGHT, Direction.UP, gdxArrayOf(Vector2(1f, 12f)), onPlatform, plan) shouldBe true
        }
    }

    describe("plan with turned gravity") {

        it("Direction.RIGHT: gives the velocity in the world frame") {
            world.block(0f, 0f, 1f, 3f)
            world.block(0f, 6f, 1f, 6f)
            world.death(-3f, 3f, 1f, 3f)
            val farSideY: (GameRectangle) -> Boolean = { it.getY() > 5f * ppm }
            val passed =
                planner(1f, 2f).plan(Facing.RIGHT, Direction.RIGHT, gdxArrayOf(Vector2(8f, 10f)), capturing(farSideY), plan)
            passed shouldBe true
            plan.worldVelocity shouldBe Vector2(10f * ppm, 8f * ppm)
            landing.getX() shouldBe 1f * ppm
            landing.getY() shouldBeGreaterThan 5f * ppm
        }
    }
})
