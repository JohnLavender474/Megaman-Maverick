package com.megaman.maverick.game.entities.bosses.desertman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Facing
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.bosses.DesertMan.DesertManState
import com.megaman.maverick.game.entities.decorations.DustPuff
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val TOL = 0.01f
private const val GRAVITY = 22.5f * T
private const val HALF_BODY = 0.75f * T
private const val BODY_HEIGHT = 1.75f * T
private const val CEILING = FLOOR + 10f * T
private const val BURROW_STATES = "BURROW BURROW_TRAVEL BURROW_TELL"

class DesertManBurrowTest : DescribeSpec({

    lateinit var rig: DesertManRig

    afterEach { rig.dispose() }

    fun start(hard: Boolean = false, dark: Boolean = false, playerX: Float = 18f) {
        rig = DesertManRig(hard)
        rig.startFight(phase2 = true, dark = dark)
        rig.place(playerX, 0f)
    }

    fun burrow() = rig.attack(DesertManState.BURROW, PASS)

    fun underSand() = rig.state.name in BURROW_STATES.split(" ")

    fun runTo(state: DesertManState, each: () -> Unit = {}) =
        rig.ticksUntil(5f, each = each) { rig.state == state } shouldBe true

    describe("the guard") {
        it("happens only in phase 2") {
            rig = DesertManRig()
            rig.startFight()
            rig.place(18f, 0f)
            rig.attack(DesertManState.PUNCH, PASS, PASS)
            rig.dispose()
            start()
            burrow()
        }
    }

    describe("the sink") {
        it("spawns a dust puff at the sink point and shows the burrow frames once while the body sinks 1 tile") {
            start()
            val x = rig.bossCenter().x
            burrow()
            rig.spawnPositionsOf<DustPuff>() shouldBe listOf(Vector2(x, FLOOR))
            val indices = mutableListOf(rig.animIndex)
            var k = 0
            while (rig.state == DesertManState.BURROW) {
                withClue("tick $k") {
                    rig.animKey shouldBe "burrow"
                    rig.bossBody.getY() shouldBe (FLOOR - (k * STEP / 0.4f).coerceAtMost(1f) * T plusOrMinus TOL)
                    val main = rig.sprite(ConstKeys.MAIN)
                    main.hidden shouldBe false
                    (main.x + main.width / 2f) shouldBe (x plusOrMinus TOL)
                    main.y shouldBe (FLOOR - 0.5f * T plusOrMinus TOL)
                }
                rig.tick()
                k++
                indices.add(rig.animIndex)
            }
            k * STEP shouldBe (0.4f plusOrMinus 1.5f * STEP)
            indices.zipWithNext().forEach { (a, b) -> b shouldBe if (b == a) a else a + 1 }
            indices.toSet() shouldBe setOf(0, 1, 2, 3)
            listOf(0, 1, 2).forEach { frame ->
                indices.count { it == frame } * STEP shouldBe (0.1f plusOrMinus 2.5f * STEP)
            }
        }
    }

    describe("under the sand") {
        it("does no damage, takes no damage, is not drawn after the sink, and has no terrain, collision or gravity") {
            start()
            rig.boss.canBeDamagedBy(rig.bullet()) shouldBe true
            rig.boss.canDamage(rig.player) shouldBe true
            burrow()
            rig.tick()
            var ticks = 0
            while (underSand()) {
                withClue("${rig.state} tick $ticks") {
                    rig.boss.canBeDamagedBy(rig.bullet()) shouldBe false
                    rig.boss.canDamage(rig.player) shouldBe false
                    listOf(FixtureType.FEET, FixtureType.HEAD, FixtureType.SIDE, FixtureType.BODY, FixtureType.DAMAGER,
                        FixtureType.DAMAGEABLE).forEach { type ->
                        rig.fixtures(type).forEach { it.isActive() shouldBe false }
                    }
                    rig.bossBody.physics.collisionOn shouldBe false
                    rig.bossBody.physics.gravityOn shouldBe false
                    if (rig.state != DesertManState.BURROW) rig.sprite(ConstKeys.MAIN).hidden shouldBe true
                }
                rig.tick()
                ticks++
            }
            rig.state shouldBe DesertManState.ERUPT
        }
    }

    describe("the trail") {
        listOf(false to 8f, true to 10f).forEach { (hard, speed) ->
            it("${if (hard) "hard" else "normal"}: runs at $speed tiles/s to the target x, then the tell lasts 0.6 s") {
                start(hard = hard, dark = true)
                val x = rig.bossCenter().x
                burrow()
                runTo(DesertManState.BURROW_TRAVEL)
                rig.place(5f, 0f)
                runTo(DesertManState.ERUPT)
                val travel = rig.lights.filter { it.first == DesertManState.BURROW_TRAVEL }.map { it.second }
                val tell = rig.lights.filter { it.first == DesertManState.BURROW_TELL }.map { it.second }
                (listOf(Vector2(x, FLOOR + 0.5f * T)) + travel).zipWithNext().dropLast(1).forEach { (a, b) ->
                    abs(b.x - a.x) shouldBe (speed * T * STEP plusOrMinus TOL)
                }
                tell.first().x shouldBe (18f * T plusOrMinus TOL)
                travel.forEach { it.x shouldBeLessThanOrEqual 18f * T + TOL }
                (travel + tell).forEach { it.y shouldBe (FLOOR + 0.5f * T plusOrMinus TOL) }
                tell.forEach { it.x shouldBe (18f * T plusOrMinus TOL) }
                tell.size * STEP shouldBe (0.6f plusOrMinus 1.5f * STEP)
            }
        }

        it("splashes within 0.25 tile of the spot during the tell, with the dig sound") {
            start()
            burrow()
            runTo(DesertManState.BURROW_TELL)
            val splashes = rig.spawnPositionsOf<Splash>().size
            val digs = rig.sounds(SoundAsset.DIG_SOUND)
            runTo(DesertManState.ERUPT)
            val tell = rig.spawnPositionsOf<Splash>().drop(splashes).dropLast(1)
            (tell.size >= 5) shouldBe true
            tell.forEach { abs(it.x - 18f * T) shouldBeLessThanOrEqual 0.25f * T + TOL }
            (rig.sounds(SoundAsset.DIG_SOUND) - digs) shouldBe 1
        }

        listOf(1f to LEFT_WALL + HALF_BODY, 23f to RIGHT_WALL - HALF_BODY).forEach { (playerX, expected) ->
            it("clamps the target x of a target at $playerX tiles to half a body width inside the walls") {
                start(playerX = playerX)
                burrow()
                runTo(DesertManState.ERUPT)
                rig.bossCenter().x shouldBe (expected plusOrMinus TOL)
            }
        }
    }

    describe("the erupt") {
        it("starts on the sand at the trail end with a dust puff and the erupt frames once, with no move") {
            start()
            burrow()
            runTo(DesertManState.ERUPT)
            rig.spawnPositionsOf<DustPuff>().last() shouldBe Vector2(18f * T, FLOOR)
            rig.spawnPositionsOf<Splash>().last() shouldBe Vector2(18f * T, FLOOR)
            val indices = mutableListOf<Int>()
            while (rig.animKey == "erupt") {
                rig.bossCenter().x shouldBe (18f * T plusOrMinus TOL)
                rig.bossBody.getY() shouldBe (FLOOR plusOrMinus TOL)
                rig.bossBody.physics.velocity.y shouldBe 0f
                indices.add(rig.animIndex)
                rig.tick()
            }
            indices.size * STEP shouldBe (0.3f plusOrMinus 1.5f * STEP)
            indices.toSet() shouldBe setOf(0, 1, 2)
            indices.zipWithNext().forEach { (a, b) -> (b >= a) shouldBe true }
            (rig.bossBody.physics.velocity.y > 0f) shouldBe true
        }

        data class Case(val name: String, val playerX: Float, val playerY: Float)

        listOf(
            Case("the target is straight above", 18f, 0f),
            Case("the target is high and to the right", 21f, 7f),
            Case("the target is far to the left", 3f, 0f),
            Case("the target hugs the right wall at the top", 21.5f, 8.5f)
        ).forEach { case ->
            it("jumps to the target, under the wall tops, when ${case.name}; no air friction until the erupt ends") {
                start()
                burrow()
                runTo(DesertManState.ERUPT)
                rig.place(case.playerX, case.playerY)
                rig.ticksUntil(1f) { rig.bossBody.physics.velocity.y > 0f } shouldBe true
                val center = rig.bossCenter()
                val target = rig.playerBody.getCenter(Vector2())
                val maxRise = CEILING - BODY_HEIGHT / 2f - center.y
                val rise = max(0f, target.y + T - center.y)
                val step = rig.services.settings.performance.fixedStep
                val ceilingSpeed = GRAVITY * (sqrt(step * step / 4f + 2f * maxRise / GRAVITY) - step / 2f)
                val vy = max(15f * T, min(sqrt(2f * GRAVITY * rise), ceilingSpeed))
                val vx = ((target.x.coerceIn(LEFT_WALL + HALF_BODY, RIGHT_WALL - HALF_BODY) - center.x) / (vy / GRAVITY))
                    .coerceIn(-12f * T, 12f * T)
                rig.bossBody.physics.velocity.y shouldBe (vy plusOrMinus 0.1f)
                rig.bossBody.physics.velocity.x shouldBe (vx plusOrMinus 0.1f)
                if (case.playerX == 18f) abs(rig.bossBody.physics.velocity.x) shouldBe (0f plusOrMinus TOL)
                if (case.playerX == 3f) rig.bossBody.physics.velocity.x shouldBe -12f * T
                var apex = 0f
                var damagerApex = 0f
                while (rig.state == DesertManState.ERUPT) {
                    rig.bossBody.physics.applyFrictionX shouldBe false
                    rig.bossBody.physics.applyFrictionY shouldBe false
                    apex = max(apex, rig.bossBody.getMaxY())
                    damagerApex = max(damagerApex, rig.rect(rig.fixture(FixtureType.DAMAGER)).getMaxY())
                    rig.tick()
                }
                val apexBetweenStepsSlack = GRAVITY * STEP * STEP / 8f
                apex shouldBeLessThanOrEqual CEILING + TOL + vy * (STEP - step) / 2f + apexBetweenStepsSlack
                if (case.playerY == 8.5f) {
                    apex shouldBe (CEILING plusOrMinus 0.25f * T)
                    damagerApex shouldBeGreaterThanOrEqual rig.playerBody.getY()
                }
                rig.state shouldBe DesertManState.STAND
                rig.bossBody.getY() shouldBe (FLOOR plusOrMinus TOL)
                rig.bossBody.physics.applyFrictionX shouldBe true
                rig.bossBody.physics.applyFrictionY shouldBe true
            }
        }

        it("shows the tornado spin frames while rising and the jump frame while falling") {
            start()
            burrow()
            runTo(DesertManState.ERUPT)
            rig.ticksUntil(1f) { rig.bossBody.physics.velocity.y > 0f } shouldBe true
            val indices = mutableListOf<Int>()
            while (rig.bossBody.physics.velocity.y > 0f) {
                rig.tick()
                if (rig.bossBody.physics.velocity.y > 0f) {
                    rig.animKey shouldBe "tornado_spin"
                    indices.add(rig.animIndex)
                }
            }
            indices.toSet() shouldBe setOf(0, 1, 2, 3)
            rig.tick()
            rig.animKey shouldBe "jump"
        }

        it("keeps the short-punch size of 0.75 x 0.5 tile while rising, after a long punch") {
            start()
            rig.attack(DesertManState.PUNCH, FAIL, PASS)
            rig.ticksUntil(4f) { rig.state == DesertManState.STAND } shouldBe true
            burrow()
            runTo(DesertManState.ERUPT)
            rig.ticksUntil(1f) { rig.armDamager.isActive() } shouldBe true
            val arm = rig.rect(rig.armDamager)
            arm.getWidth() shouldBe (0.75f * T plusOrMinus TOL)
            arm.getHeight() shouldBe (0.5f * T plusOrMinus TOL)
            if (rig.boss.facing == Facing.LEFT) arm.getMaxX() shouldBe (rig.bossBody.getX() plusOrMinus 0.5f * T)
            else arm.getX() shouldBe (rig.bossBody.getMaxX() plusOrMinus 0.5f * T)
        }

        it("sends a light source event at its body in a dark room") {
            start(dark = true)
            burrow()
            runTo(DesertManState.ERUPT)
            rig.tick()
            val light = rig.lights.last()
            light.first shouldBe DesertManState.ERUPT
            light.second shouldBe rig.bossCenter()
        }

        it("sends no light source event in a lit room") {
            start()
            burrow()
            runTo(DesertManState.STAND)
            rig.lights.size shouldBe 0
        }
    }
})
