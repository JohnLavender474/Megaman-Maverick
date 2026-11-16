package com.megaman.maverick.game.entities.bosses.desertman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.bosses.DesertMan.DesertManState
import com.megaman.maverick.game.entities.projectiles.Needle
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs

private const val TOL = 0.01f
private const val GRAVITY = 22.5f * T
private const val RISE_DUR = 12f / 22.5f
private val FAN_1 = listOf(90f, 60f, 30f, 0f, 330f, 300f, 270f)
private val FAN_2 = listOf(75f, 45f, 15f, 345f, 315f, 285f)

private class SpinRow(
    val name: String,
    val hard: Boolean,
    val phase2: Boolean,
    val tell: Float,
    val bursts: Int,
    val delay: Float
)

private val ROWS = listOf(
    SpinRow("normal", false, false, 0.6f, 2, 0.6f),
    SpinRow("normal phase 2", false, true, 0.5f, 2, 0.5f),
    SpinRow("hard", true, false, 0.35f, 3, 0.4f),
    SpinRow("hard phase 2", true, true, 0.3f, 4, 0.35f)
)

private class SpinFrame(
    val state: DesertManState,
    val key: String?,
    val index: Int,
    val center: Vector2,
    val velocity: Vector2,
    val whooshes: Int,
    val needles: List<Properties>
)

private fun angleDiff(a: Float, b: Float) = abs(((a - b + 540f) % 360f) - 180f)

class DesertManNeedleSpinTest : DescribeSpec({

    lateinit var rig: DesertManRig

    afterEach { rig.dispose() }

    fun start(hard: Boolean = false, phase2: Boolean = false, bossX: Float? = null, playerX: Float = 21.5f) {
        rig = DesertManRig(hard)
        rig.startFight(phase2 = phase2)
        bossX?.let { rig.placeBoss(it) }
        rig.place(playerX, 0f)
    }

    fun spin(vararg rolls: Float = floatArrayOf(FAIL, FAIL, PASS)): List<SpinFrame> {
        val frames = mutableListOf<SpinFrame>()
        var logged = rig.spawnLog.size
        fun record() {
            val needles = (logged until rig.spawnLog.size).map { rig.spawnLog[it] }
                .filter { it.first is Needle }.map { it.second }
            logged = rig.spawnLog.size
            frames.add(
                SpinFrame(
                    rig.state, rig.animKey, rig.animIndex, rig.bossCenter(), Vector2(rig.bossBody.physics.velocity),
                    rig.sounds(SoundAsset.WHOOSH_SOUND), needles
                )
            )
        }
        rig.attack(DesertManState.NEEDLE_SPIN, *rolls)
        record()
        rig.ticksUntil(10f, each = { record() }) { rig.state != DesertManState.NEEDLE_SPIN } shouldBe true
        rig.state shouldBe DesertManState.STAND
        return frames
    }

    describe("the guard") {
        it("is checked after the tornado and before the throw") {
            start()
            rig.attack(DesertManState.TORNADO, FAIL, PASS, PASS, PASS)
            rig.dispose()
            start()
            rig.attack(DesertManState.NEEDLE_SPIN, FAIL, FAIL, PASS, PASS)
        }

        it("never comes twice in a row") {
            start()
            spin()
            rig.place(rig.bossCenter().x / T - 13f, 0f)
            rig.attack(DesertManState.THROW, FAIL, FAIL, PASS)
        }
    }

    ROWS.forEach { row ->
        describe("the needle spin, ${row.name}") {
            it("tells for ${row.tell} s standing still, with the flashing crouch, the rev sound and no needle") {
                start(row.hard, row.phase2)
                val revs = rig.sounds(SoundAsset.REV_SOUND)
                val frames = spin()
                val tell = frames.takeWhile { it.key == "needle_spin_tell" }
                tell.size * STEP shouldBe (row.tell plusOrMinus 1.5f * STEP)
                tell.forEach {
                    it.center shouldBe frames.first().center
                    it.needles.size shouldBe 0
                }
                tell.forEachIndexed { k, f ->
                    val cycles = (k + 1) * STEP / 0.08f
                    if (abs(cycles - Math.round(cycles)) > 0.05f) withClue("tick $k") { f.index shouldBe cycles.toInt() % 2 }
                }
                rig.sounds(SoundAsset.REV_SOUND) - revs shouldBe 1
            }

            it("fires ${row.bursts} bursts from the top of the jump, ${row.delay} s apart, each with a hover and a whoosh") {
                start(row.hard, row.phase2)
                val frames = spin()
                val bursts = frames.indices.filter { frames[it].needles.isNotEmpty() }
                bursts.size shouldBe row.bursts
                val first = bursts.first()
                frames[first - 1].velocity.y shouldBe (0f plusOrMinus GRAVITY * STEP)
                (frames[first - 1].velocity.y > 0f) shouldBe true
                bursts.zipWithNext().forEach { (a, b) -> (b - a) * STEP shouldBe (row.delay plusOrMinus 1.5f * STEP) }
                bursts.forEachIndexed { i, b ->
                    withClue("burst ${i + 1}") {
                        val f = frames[b]
                        f.velocity.y shouldBe (5f * T plusOrMinus TOL)
                        f.whooshes - frames[b - 1].whooshes shouldBe 1
                        val fan = if (i % 2 == 0) FAN_1 else FAN_2
                        f.needles.size shouldBe fan.size
                        f.needles.forEachIndexed { n, it ->
                            it.get(ConstKeys.OWNER) shouldBe rig.boss
                            it.get(ConstKeys.GRAVITY) shouldBe -15f * T
                            it.get(ConstKeys.POSITION, Vector2::class)!! shouldBe Vector2(f.center.x, f.center.y + 0.25f * T)
                            val impulse = it.get(ConstKeys.IMPULSE, Vector2::class)!!
                            impulse.len() shouldBe (15f * T plusOrMinus TOL)
                            (angleDiff(impulse.angleDeg(), 90f + fan[n]) <= 0.05f) shouldBe true
                        }
                    }
                }
                frames.drop(first).filter { it.state == DesertManState.NEEDLE_SPIN }
                    .forEach { it.velocity.x shouldBe 0f }
            }

            it("shows needle_spin from the first burst to 0.3 s after the last, then jump until he lands, then STAND") {
                start(row.hard, row.phase2)
                val frames = spin()
                val bursts = frames.indices.filter { frames[it].needles.isNotEmpty() }
                val tell = frames.takeWhile { it.key == "needle_spin_tell" }.size
                frames.subList(tell, bursts.first()).forEach { it.key shouldBe "needle_spin_tuck" }
                val spinning = frames.drop(bursts.first()).takeWhile { it.key == "needle_spin" }
                (bursts.first() + spinning.size - bursts.last()) * STEP shouldBe (0.3f plusOrMinus 1.5f * STEP)
                spinning.map { it.index }.toSet() shouldBe setOf(0, 1, 2, 3)
                val falling = frames.drop(bursts.first() + spinning.size).dropLast(1)
                falling.forEach { it.key shouldBe "jump" }
                frames.last().state shouldBe DesertManState.STAND
                rig.bossBody.getY() shouldBe (FLOOR plusOrMinus TOL)
            }
        }
    }

    describe("the jump") {
        data class Case(val name: String, val bossX: Float?, val playerX: Float, val rolls: FloatArray, val landX: Float)

        listOf(
            Case("a far target: 6 tiles at most", null, 21.5f, floatArrayOf(FAIL, FAIL, PASS), 18f),
            Case("a target 4 tiles away: 1 tile", null, 16f, floatArrayOf(FAIL, FAIL, FAIL, PASS), 13f),
            Case("a target 2 tiles away: no move", null, 10f, floatArrayOf(FAIL, FAIL, FAIL, PASS), 12f),
            Case("a target past the wall: clamped to the arena", 20.5f, 25f, floatArrayOf(FAIL, FAIL, FAIL, PASS), 21.25f)
        ).forEach { case ->
            it("jumps at 12 tiles/s up and moves toward ${case.name}") {
                start(bossX = case.bossX, playerX = case.playerX)
                val frames = spin(*case.rolls)
                val tell = frames.takeWhile { it.key == "needle_spin_tell" }.size
                val jump = frames[tell]
                val x = frames.first().center.x
                val vx = (case.landX * T - x) / RISE_DUR
                withClue("the tick that ends the tell") {
                    jump.velocity.y shouldBe (12f * T plusOrMinus TOL)
                    jump.velocity.x shouldBe (vx plusOrMinus 0.1f)
                }
                frames.drop(tell).takeWhile { it.needles.isEmpty() }.forEach { it.velocity.x shouldBe (vx plusOrMinus 0.1f) }
            }
        }
    }
})
