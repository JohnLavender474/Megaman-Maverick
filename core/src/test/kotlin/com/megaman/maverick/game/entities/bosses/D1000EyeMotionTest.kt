package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.entities.utils.TickFrames
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import kotlin.math.abs
import kotlin.math.roundToInt
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director

private const val DASH_ART_PX = 3
private const val FIGHT_SECONDS = 40f
private const val SHOT_EVERY = 0.9f
private const val SHOT_LIFE = 0.3f

private class EyeTrack {
    val xs = ArrayList<Int>()
    val ys = ArrayList<Int>()
    val drawnXs = ArrayList<Int>()
    val drawnYs = ArrayList<Int>()

    fun add(center: Vector2) {
        xs.add((center.x / TickFrames.SCALE).roundToInt())
        ys.add((center.y / TickFrames.SCALE).roundToInt())
    }

    fun addDrawn(x: Float, y: Float) {
        drawnXs.add((x / TickFrames.SCALE).roundToInt())
        drawnYs.add((y / TickFrames.SCALE).roundToInt())
    }

    fun drawnFlips(): List<String> {
        val out = ArrayList<String>()
        listOf("x" to drawnXs, "y" to drawnYs).forEach { (axis, values) ->
            for (i in 1 until values.size - 1) if (values[i] != values[i - 1] && values[i + 1] == values[i - 1])
                out.add("$axis frame $i: ${values.subList(maxOf(0, i - 4), minOf(values.size, i + 3))}")
        }
        return out
    }

    fun slowReversals(): List<String> {
        val out = ArrayList<String>()
        listOf("x" to xs, "y" to ys).forEach { (axis, values) ->
            var lastSign = 0
            var lastFrame = -1
            for (i in 1 until values.size) {
                val d = values[i] - values[i - 1]
                if (d == 0) continue
                val sign = if (d > 0) 1 else -1
                val gap = (i - lastFrame) * RIG_DELTA
                if (lastSign != 0 && sign != lastSign && abs(d) < DASH_ART_PX &&
                    gap < D1000Core.REVERSE_PAUSE - RIG_DELTA / 2f
                ) out.add("$axis frame $i: ${values.subList(maxOf(0, i - 4), minOf(values.size, i + 3))}")
                lastSign = sign
                lastFrame = i
            }
        }
        return out
    }
}

class D1000EyeMotionTest : DescribeSpec({

    lateinit var rig: D1000Rig

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    fun newRig(hard: Boolean): D1000Rig {
        rig = D1000Rig(arenaWidth = 18.5f * RIG_PPM)
        if (hard) rig.services.gameState.setDifficultyMode(DifficultyMode.HARD)
        rig.placeX(3f, 0f)
        return rig
    }

    fun start(overdrive: Boolean) {
        if (overdrive) {
            rig.spawnBoss()
            rig.hitEye(3)
            rig.ticks(ENTRY_SECONDS)
            rig.boss.bossComponent.phase = BossPhase.FIGHT
        } else rig.startFight()
    }

    fun eyeCenter(): Vector2 = rig.core().getComponent(BodyComponent::class)!!.body.getCenter(Vector2())

    fun fight(shoot: Boolean, seconds: Float, track: EyeTrack) {
        var bullet: Bullet? = null
        var age = 0f
        var walk = 1f
        var stand = 0f
        var sabreAt = 9f
        var time = 0f
        while (time < seconds) {
            val eye = eyeCenter()
            if (shoot) {
                age += RIG_DELTA
                if (bullet == null && age > SHOT_EVERY) {
                    bullet = MegaEntityFactory.fetch(Bullet::class)!!.also {
                        it.spawn(
                            props(
                                ConstKeys.OWNER pairTo rig.x,
                                ConstKeys.POSITION pairTo Vector2(eye.x - 3f * RIG_PPM, eye.y),
                                ConstKeys.TRAJECTORY pairTo Vector2(10f * RIG_PPM, 0f)
                            )
                        )
                        it.getComponent(BodyComponent::class)!!.body.preProcess()
                    }
                    age = 0f
                } else if (bullet != null && age > SHOT_LIFE) {
                    bullet.destroy()
                    bullet = null
                    age = 0f
                }
            } else if (stand > 0f) stand -= RIG_DELTA else {
                val x = rig.xBody.getCenter(Vector2())
                val next = x.x + walk * 6f * RIG_PPM * RIG_DELTA
                if (next > rig.arena.getX() + 15f * RIG_PPM || next < rig.arena.getX() + 2f * RIG_PPM) {
                    walk = -walk
                    stand = 0.7f
                } else rig.xBody.setCenter(next, x.y)
            }
            if (time >= sabreAt && rig.core().vulnerable) {
                rig.core().takeDamageFrom(rig.sabre())
                sabreAt = time + 11f
            }
            rig.tick()
            track.add(eyeCenter())
            rig.core().defaultSprite.let { track.addDrawn(it.x, it.y) }
            time += RIG_DELTA
        }
    }

    describe("a slow eye move never turns back within 0.1 s of its last step (no 1-frame flip)") {
        listOf(false, true).forEach { hard ->
            listOf(false, true).forEach { overdrive ->
                listOf(true, false).forEach { shoot ->
                    val name = "${if (hard) "Hard" else "Normal"}, ${if (overdrive) "Overdrive" else "base"}, " +
                        if (shoot) "X shoots at the eye" else "X walks"
                    it(name) {
                        newRig(hard)
                        start(overdrive)
                        UtilMethods.random.setSeed(if (hard) 11L else 7L)
                        val track = EyeTrack()
                        fight(shoot, FIGHT_SECONDS, track)
                        val reversals = track.slowReversals()
                        withClue("reversals: $reversals") { reversals.isEmpty() shouldBe true }
                        val flips = track.drawnFlips()
                        withClue("drawn 1-frame flips: $flips") { flips.isEmpty() shouldBe true }
                    }
                }
            }
        }
    }

    describe("the idle drift") {
        it("steps 1 art px every 2 frames along an edge, with no hitch") {
            newRig(false)
            rig.startFight()
            rig.rollFor(D1000Action.WALK, d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false)))
            rig.placeX(3f, 0f)
            rig.ticks(1f)
            val track = EyeTrack()
            repeat((2.5f / RIG_DELTA).toInt()) {
                rig.tick()
                track.add(eyeCenter())
            }
            val gaps = ArrayList<Int>()
            var last = -1
            for (i in 1 until track.xs.size) {
                val moved = track.xs[i] != track.xs[i - 1] || track.ys[i] != track.ys[i - 1]
                if (!moved) continue
                if (last >= 0 && i - last <= 4) gaps.add(i - last)
                last = i
            }
            (gaps.size > 20) shouldBe true
            withClue("frames between steps: $gaps") { gaps.all { it == 2 } shouldBe true }
        }
    }

})
