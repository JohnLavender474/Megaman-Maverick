package com.megaman.maverick.game.entities.bosses.desertman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Facing
import com.megaman.maverick.game.entities.bosses.DesertMan.DesertManState
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs

private const val TOL = 0.01f

class DesertManTornadoTest : DescribeSpec({

    lateinit var rig: DesertManRig

    afterEach { rig.dispose() }

    fun start(hard: Boolean = false, phase2: Boolean = false, bossX: Float? = null) {
        rig = DesertManRig(hard)
        rig.startFight(phase2 = phase2)
        bossX?.let { rig.placeBoss(it) }
        rig.place(21.5f, 0f)
    }

    fun tornado() = rig.attack(DesertManState.TORNADO, FAIL, PASS)

    fun shortPunchChecks() {
        val arm = rig.rect(rig.armDamager)
        arm.getWidth() shouldBe (0.75f * T plusOrMinus TOL)
        arm.getHeight() shouldBe (0.5f * T plusOrMinus TOL)
        val body = rig.bossBody
        val moved = abs(body.physics.velocity.x) * STEP + TOL
        arm.getCenter(Vector2()).y shouldBe (body.getMaxY() plusOrMinus TOL)
        if (rig.boss.facing == Facing.RIGHT) arm.getX() shouldBe (body.getMaxX() plusOrMinus moved)
        else arm.getMaxX() shouldBe (body.getX() plusOrMinus moved)
        rig.fistDamager.isActive() shouldBe false
    }

    describe("the spin-up") {
        listOf(
            Triple("normal", false to false, 0.5f),
            Triple("hard phase 2", true to true, 0.35f)
        ).forEach { (name, row, dur) ->
            it("$name: lasts $dur s with no move and the short-punch damager off") {
                start(hard = row.first, phase2 = row.second)
                val x = rig.bossBody.getX()
                tornado()
                var ticks = 0
                while (rig.animKey != "tornado_punch" && ticks < 120) {
                    rig.armDamager.isActive() shouldBe false
                    rig.bossBody.physics.velocity.x shouldBe 0f
                    rig.bossBody.getX() shouldBe x
                    rig.tick()
                    ticks++
                }
                ticks * STEP shouldBe (dur plusOrMinus 1.5f * STEP)
                rig.ticks(2f * STEP)
                (rig.bossBody.getX() > x) shouldBe true
            }
        }

        it("shows tornado_start once for 0.2 s, then tornado until he moves") {
            start()
            val x = rig.bossBody.getX()
            tornado()
            val keys = mutableListOf(rig.animKey)
            val indices = mutableListOf(rig.animIndex)
            while (rig.animKey != "tornado_punch" && keys.size < 120) {
                rig.bossBody.getX() shouldBe x
                rig.tick()
                keys.add(rig.animKey)
                indices.add(rig.animIndex)
            }
            val start = keys.takeWhile { it == "tornado_start" }.size
            start * STEP shouldBe (0.2f plusOrMinus 1.5f * STEP)
            keys.drop(start).dropLast(1).forEach { it shouldBe "tornado" }
            (keys.size - 1 - start) * STEP shouldBe (0.3f plusOrMinus 1.5f * STEP)
            indices.take(start).zipWithNext().forEach { (a, b) -> (b >= a) shouldBe true }
            indices.take(start).toSet() shouldBe setOf(0, 1)
        }

        it("turns to face the player during the spin-up") {
            start()
            tornado()
            rig.boss.facing shouldBe Facing.RIGHT
            rig.place(3f, 0f)
            rig.tick()
            rig.boss.facing shouldBe Facing.LEFT
        }
    }

    describe("the moving tornado") {
        it("keeps the fist forward with the short-punch damager on, and does not follow the player") {
            start()
            val x = rig.bossBody.getX()
            tornado()
            rig.ticksUntil(1f) { rig.bossBody.getX() != x } shouldBe true
            rig.place(3f, 0f)
            var moving = 0
            while (rig.state == DesertManState.TORNADO) {
                withClue("tick $moving") {
                    rig.animKey shouldBe "tornado_punch"
                    rig.boss.facing shouldBe Facing.RIGHT
                    rig.armDamager.isActive() shouldBe true
                    rig.armDamageable.isActive() shouldBe true
                    shortPunchChecks()
                }
                rig.tick()
                moving++
            }
            (moving > 10) shouldBe true
        }

        it("keeps the short-punch size of 0.75 x 0.5 tile after a long punch") {
            start()
            rig.place(18f, 0f)
            rig.attack(DesertManState.PUNCH, FAIL, PASS)
            rig.ticksUntil(4f) { rig.state == DesertManState.STAND } shouldBe true
            rig.place(21.5f, 0f)
            tornado()
            rig.ticksUntil(1f) { rig.armDamager.isActive() } shouldBe true
            rig.tick()
            shortPunchChecks()
        }
    }

    describe("the walls and the turn") {
        it("phase 1: the first wall ends the tornado, with no turn") {
            start()
            tornado()
            var flips = 0
            var facing = rig.boss.facing
            rig.ticksUntil(3f, each = {
                if (rig.boss.facing != facing) flips++
                facing = rig.boss.facing
                rig.place(rig.bossCenter().x / T + 2f, 0f)
            }) { rig.state != DesertManState.TORNADO } shouldBe true
            flips shouldBe 0
            rig.state shouldBe DesertManState.STAND
            rig.bossBody.getMaxX() shouldBe (RIGHT_WALL plusOrMinus TOL)
        }

        it("phase 2: turns at the first wall at 2 tiles/s, and the next wall ends it") {
            start(phase2 = true, bossX = 6f)
            tornado()
            val turns = mutableListOf<Float>()
            var facing = rig.boss.facing
            rig.ticksUntil(4f, each = {
                if (rig.boss.facing != facing) turns.add(rig.bossBody.physics.velocity.x)
                facing = rig.boss.facing
                rig.place(rig.bossCenter().x / T + 2f * facing.value, 0f)
            }) { rig.state != DesertManState.TORNADO } shouldBe true
            turns.size shouldBe 1
            turns.first() shouldBe (-2f * T plusOrMinus 0.5f * T)
            rig.state shouldBe DesertManState.STAND
            rig.bossBody.getX() shouldBe (LEFT_WALL plusOrMinus TOL)
        }

        it("phase 2: turns once when the target is more than 1 tile behind him, not at 1 tile or less") {
            start(phase2 = true, bossX = 6f)
            val x = rig.bossBody.getX()
            tornado()
            rig.ticksUntil(1f) { rig.bossBody.getX() != x } shouldBe true
            fun behind(gap: Float) = rig.playerBody.setX(rig.bossBody.getX() - gap * T - rig.playerBody.getWidth())
            repeat(10) {
                behind(0.5f)
                rig.tick()
                rig.boss.facing shouldBe Facing.RIGHT
            }
            behind(1.5f)
            rig.tick()
            rig.boss.facing shouldBe Facing.LEFT
            var flips = 0
            var facing = rig.boss.facing
            rig.ticksUntil(4f, each = {
                if (rig.boss.facing != facing) flips++
                facing = rig.boss.facing
                rig.playerBody.setX(rig.bossBody.getMaxX() + 3f * T)
            }) { rig.state != DesertManState.TORNADO } shouldBe true
            flips shouldBe 0
            rig.state shouldBe DesertManState.STAND
        }
    }
})
