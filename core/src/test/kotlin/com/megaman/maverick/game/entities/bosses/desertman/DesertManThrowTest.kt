package com.megaman.maverick.game.entities.bosses.desertman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.bosses.DesertMan.DesertManState
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.world.body.FixtureType
import com.maverick.game.engine.world.body.BodyComponent
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.abs

private const val TOL = 0.01f
private const val ANGLE_TOL = 0.05f
private const val HALF_SPRITE = 1.75f * T

private class ThrowRow(
    val name: String,
    val hard: Boolean,
    val phase2: Boolean,
    val windUp: Float,
    val hold: Float,
    val volleys: Int,
    val needles: Int,
    val step: Float,
    val speed: Float
)

private val ROWS = listOf(
    ThrowRow("normal", false, false, 0.6f, 0.35f, 1, 5, 20f, 10f),
    ThrowRow("normal phase 2", false, true, 0.5f, 0.3f, 2, 5, 18f, 11f),
    ThrowRow("hard", true, false, 0.45f, 0.3f, 2, 5, 18f, 12f),
    ThrowRow("hard phase 2", true, true, 0.4f, 0.25f, 3, 7, 15f, 12f)
)

private class ThrowFrame(
    val key: String?,
    val index: Int,
    val facing: Facing,
    val centerX: Float,
    val bodyY: Float,
    val tremble: Float,
    val armOrFistOn: Boolean,
    val burstHidden: Boolean,
    val burstCenter: Vector2,
    val needles: List<Properties>
)

private fun angleDiff(a: Float, b: Float) = abs(((a - b + 540f) % 360f) - 180f)

class DesertManThrowTest : DescribeSpec({

    lateinit var rig: DesertManRig

    afterEach { rig.dispose() }

    fun start(hard: Boolean = false, phase2: Boolean = false) {
        rig = DesertManRig(hard)
        rig.startFight(phase2 = phase2)
        rig.place(21.5f, 0f)
    }

    fun throwNeedles(rolls: FloatArray = floatArrayOf(FAIL, FAIL, FAIL, PASS), each: () -> Unit = {}): List<ThrowFrame> {
        val frames = mutableListOf<ThrowFrame>()
        var logged = rig.spawnLog.size
        fun record() {
            val burst = rig.sprite(ConstKeys.BURST)
            val needles = (logged until rig.spawnLog.size).map { rig.spawnLog[it] }
                .filter { it.first is Needle }.map { it.second }
            logged = rig.spawnLog.size
            frames.add(
                ThrowFrame(
                    rig.animKey, rig.animIndex, rig.boss.facing, rig.bossCenter().x, rig.bossBody.getY(),
                    rig.sprite(ConstKeys.MAIN).x - (rig.bossCenter().x - HALF_SPRITE),
                    rig.fixtures(FixtureType.DAMAGER).drop(1).any { it.isActive() } ||
                        rig.fixtures(FixtureType.DAMAGEABLE).drop(1).any { it.isActive() },
                    burst.hidden, Vector2(burst.x + burst.width / 2f, burst.y + burst.height / 2f), needles
                )
            )
        }
        rig.attack(DesertManState.THROW, *rolls)
        record()
        each()
        while (rig.state == DesertManState.THROW) {
            rig.tick()
            if (rig.state == DesertManState.THROW) {
                record()
                each()
            }
        }
        rig.state shouldBe DesertManState.STAND
        return frames
    }

    fun segments(frames: List<ThrowFrame>): List<Pair<String?, List<ThrowFrame>>> {
        val out = mutableListOf<Pair<String?, MutableList<ThrowFrame>>>()
        frames.forEach { f ->
            if (out.isEmpty() || out.last().first != f.key) out.add(f.key to mutableListOf())
            out.last().second.add(f)
        }
        return out
    }

    describe("the guard") {
        it("needs the target center 3 tiles or more away in x") {
            start()
            rig.place(15f, 0f)
            rig.attack(DesertManState.THROW, FAIL, FAIL, FAIL, FAIL, PASS)
            rig.dispose()
            start()
            rig.place(14.9f, 0f)
            rig.rolls(FAIL, FAIL, FAIL, FAIL, PASS)
            rig.ticksUntil(3f) { rig.state != DesertManState.STAND } shouldBe true
            rig.state shouldBe DesertManState.DANCE
        }

        it("is checked after the tornado and before the dance") {
            start()
            rig.attack(DesertManState.TORNADO, FAIL, PASS, PASS, PASS, PASS)
            rig.dispose()
            start()
            rig.attack(DesertManState.THROW, FAIL, FAIL, FAIL, PASS, PASS)
        }

        it("never comes twice in a row") {
            start()
            throwNeedles()
            rig.attack(DesertManState.DANCE, FAIL, FAIL, FAIL, PASS)
        }
    }

    ROWS.forEach { row ->
        describe("the throw, ${row.name}") {
            it("winds up for ${row.windUp} s then 0.3 s, holds the throw pose ${row.hold} s, ${row.volleys} volleys, then STAND") {
                start(row.hard, row.phase2)
                val revs = rig.sounds(SoundAsset.REV_SOUND)
                val bursts = rig.sounds(SoundAsset.BURST_SOUND)
                val parts = segments(throwNeedles())
                parts.map { it.first } shouldBe (0 until row.volleys).flatMap { listOf("throw_windup", "throw") }
                parts.forEachIndexed { i, (key, frames) ->
                    val expected = when {
                        key == "throw" -> row.hold
                        i == 0 -> row.windUp
                        else -> 0.3f
                    }
                    withClue("segment $i $key") { frames.size * STEP shouldBe (expected plusOrMinus 1.5f * STEP) }
                }
                rig.sounds(SoundAsset.REV_SOUND) - revs shouldBe row.volleys
                rig.sounds(SoundAsset.BURST_SOUND) - bursts shouldBe row.volleys
            }

            it("fires fans of ${row.needles} needles ${row.step} degrees apart at ${row.speed} tiles/s, around the aim") {
                start(row.hard, row.phase2)
                val frames = throwNeedles()
                val volleys = frames.filter { it.needles.isNotEmpty() }
                volleys.size shouldBe row.volleys
                volleys.forEachIndexed { v, f ->
                    withClue("volley ${v + 1}") {
                        val origin = Vector2(f.centerX + f.facing.value * 26f * ART_PX, f.bodyY - 0.5f * T + 18f * ART_PX)
                        val aim = Vector2(rig.playerBody.getCenter(Vector2())).sub(origin).angleDeg()
                        val count = if (v % 2 == 1) row.needles + 1 else row.needles
                        f.needles.size shouldBe count
                        val angles = f.needles.map {
                            it.get(ConstKeys.OWNER) shouldBe rig.boss
                            it.containsKey(ConstKeys.GRAVITY) shouldBe false
                            it.get(ConstKeys.POSITION, Vector2::class)!! shouldBe origin
                            val impulse = it.get(ConstKeys.IMPULSE, Vector2::class)!!
                            impulse.len() shouldBe (row.speed * T plusOrMinus 0.01f)
                            impulse.angleDeg()
                        }
                        angles.forEachIndexed { i, angle ->
                            angleDiff(angle, aim + (i - (count - 1) / 2f) * row.step) shouldBeLessThanOrEqual ANGLE_TOL
                        }
                        if (v % 2 == 1) angles.forEach { angleDiff(it, aim) shouldBeGreaterThanOrEqual row.step / 2f - ANGLE_TOL }
                    }
                }
                rig.spawnedOf<Needle>().forEach { needle ->
                    needle.getComponent(BodyComponent::class)!!.body.physics.gravity shouldBe
                        Vector2.Zero
                }
            }
        }
    }

    describe("the aim") {
        it("clamps to 60 degrees up from the facing") {
            start()
            rig.place(16f, 9f)
            val frames = throwNeedles(floatArrayOf(FAIL, FAIL, PASS))
            val angles = frames.first { it.needles.isNotEmpty() }.needles
                .map { it.get(ConstKeys.IMPULSE, Vector2::class)!!.angleDeg() }
            angleDiff(angles[2], 60f) shouldBeLessThanOrEqual ANGLE_TOL
        }
    }

    describe("the throw motion and the parts") {
        it("does not move in x, has no arm or fist damager, and trembles 1 art px in the wind-up only") {
            start(hard = true, phase2 = true)
            val frames = throwNeedles()
            frames.forEach {
                it.centerX shouldBe (frames.first().centerX plusOrMinus TOL)
                it.armOrFistOn shouldBe false
                if (it.key == "throw_windup") abs(it.tremble) shouldBe (ART_PX plusOrMinus TOL)
                else it.tremble shouldBe (0f plusOrMinus TOL)
            }
        }

        it("flashes throw_windup every 0.06 s") {
            start()
            val windUp = segments(throwNeedles()).first().second
            windUp.forEachIndexed { k, f ->
                val cycles = (k + 1) * STEP / 0.06f
                if (abs(cycles - Math.round(cycles)) > 0.05f) withClue("tick $k") {
                    f.index shouldBe cycles.toInt() % 2
                }
            }
        }

        it("plays the throw_burst frames once for 0.15 s at the fist after each release") {
            start(hard = true, phase2 = true)
            val parts = segments(throwNeedles()).filter { it.first == "throw" }
            parts.forEach { (_, frames) ->
                val shown = frames.takeWhile { !it.burstHidden }
                shown.size * STEP shouldBe (0.15f plusOrMinus 1.5f * STEP)
                frames.drop(shown.size).forEach { it.burstHidden shouldBe true }
                shown.forEach {
                    it.burstCenter.x shouldBe (it.centerX + it.facing.value * 29f * ART_PX plusOrMinus TOL)
                    it.burstCenter.y shouldBe (it.bodyY - 0.5f * T + 18f * ART_PX plusOrMinus TOL)
                }
            }
        }

        it("follows the target with the facing during the wind-up") {
            start(hard = true, phase2 = true)
            var moved = false
            val frames = throwNeedles(each = {
                if (!moved && rig.animKey == "throw_windup") {
                    rig.place(3f, 0f)
                    moved = true
                }
            })
            frames.first().facing shouldBe Facing.RIGHT
            frames.last().facing shouldBe Facing.LEFT
            frames.filter { it.needles.isNotEmpty() }.forEach { it.facing shouldNotBe Facing.RIGHT }
        }
    }
})
