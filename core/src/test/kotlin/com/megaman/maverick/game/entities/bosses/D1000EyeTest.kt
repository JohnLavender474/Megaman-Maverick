package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.megamanx.MegamanXSabreSlash
import com.megaman.maverick.game.entities.megamanx.SabreKind
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.EnemyShot
import com.megaman.maverick.game.entities.state.BossPhase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.assertions.withClue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import java.util.Random
import kotlin.math.abs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames

private const val HOME_TILES_X = 10.75f + 3.25f
private const val HOME_TILES_Y = 8f
private const val DRIFT_X_TILES = 0.375f
private const val DRIFT_Y_TILES = 0.25f
private const val SHOT_TILES_PER_SECOND = 10f
private const val DRIFT_SPEED_TILES = 1.875f
private const val DRIFT_HOLD_SECONDS = 0.4f

class D1000EyeTest : DescribeSpec({

    lateinit var rig: D1000Rig

    beforeEach { rig = D1000Rig() }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    fun coreCenter(): Vector2 = rig.core().getComponent(BodyComponent::class)!!.body.getCenter(Vector2())

    fun drawn(): Pair<String, Int> {
        val animator = rig.core().getComponent(AnimationsComponent::class)!!
            .getAnimator(AnimationsComponent.DEFAULT_KEY) as Animator
        val key = animator.currentKey!!
        return key to rig.core().defaultSprite.regionY / D1000Frames.CORE[key].height
    }

    fun coreShots() = rig.spawnedOf<EnemyShot>().count { it.kind == EnemyShot.Kind.CORE }

    fun seedRoll(low: Boolean) {
        val seed = (0L until Long.MAX_VALUE).first {
            val roll = Random(it).nextFloat()
            if (low) roll < 0.05f else roll > 0.95f
        }
        UtilMethods.random.setSeed(seed)
    }

    fun shootAtEye(tilesBefore: Float): Bullet {
        val eye = coreCenter()
        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo rig.x,
                ConstKeys.POSITION pairTo Vector2(eye.x - tilesBefore * RIG_PPM, eye.y),
                ConstKeys.TRAJECTORY pairTo Vector2(SHOT_TILES_PER_SECOND * RIG_PPM, 0f)
            )
        )
        bullet.getComponent(BodyComponent::class)!!.body.preProcess()
        return bullet
    }

    fun openFight() {
        rig.startFight()
        rig.rollFor(D1000Action.SUMMON, opening)
        rig.ticksUntil(2f) { rig.core().vulnerable } shouldBe true
    }

    fun idleFight() {
        rig.startFight()
        rig.rollFor(D1000Action.WALK, opening)
        rig.ticks(0.3f)
    }

    describe("the blink") {
        it("blinks: the open eye (1.5 s), a half lid (3 ticks), the shut eye (5 ticks), the half lid (3 ticks)") {
            idleFight()
            val runs = ArrayList<Pair<Int, Int>>()
            var restarted = false
            rig.ticksUntil(6f) {
                val (key, frame) = drawn()
                if (key == "core") {
                    if (runs.isNotEmpty() && runs.last().first == frame) runs[runs.size - 1] = frame to runs.last().second + 1
                    else runs.add(frame to 1)
                } else {
                    runs.clear()
                    restarted = true
                }
                restarted && runs.size >= 5
            } shouldBe true
            runs[0].first shouldBe 0
            D1000Frames.CORE["core"].count shouldBe 3
            (abs(runs[0].second - 90) <= 1) shouldBe true
            runs[1].first shouldBe 1
            (abs(runs[1].second - 3) <= 1) shouldBe true
            runs[2].first shouldBe D1000Frames.CORE_SHUT_FRAME
            (abs(runs[2].second - 5) <= 1) shouldBe true
            runs[3].first shouldBe 1
            runs[4].first shouldBe 0
        }

        it("keeps the eye box on the iris through the blink") {
            idleFight()
            rig.ticksUntil(4f) {
                drawn() == ("core" to 1)
            } shouldBe true
            D1000Frames.EYE_BOXES["core"][1] shouldBe D1000Frames.EYE_BOXES["core"][0]
        }
    }

    describe("the angry tell") {
        it("shows the angry eye for at least 0.25 s before each eyeball shot") {
            idleFight()
            val keys = ArrayList<String>()
            var shots = coreShots()
            var checked = 0
            rig.ticksUntil(40f) {
                val now = coreShots()
                if (now > shots) {
                    val before = keys.takeLast((0.25f / RIG_DELTA).toInt())
                    withClue("shot ${now} after ${keys.takeLast(40)}") { before.all { it == "core_angry" } shouldBe true }
                    checked++
                    shots = now
                }
                keys.add(drawn().first)
                checked >= 3
            } shouldBe true
        }
    }

    describe("the idle drift") {
        it("floats around home inside a small box") {
            idleFight()
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            rig.ticksUntil(3f) {
                val center = coreCenter()
                val x = (center.x - RIG_ARENA_X) / RIG_PPM
                val y = (center.y - RIG_FLOOR) / RIG_PPM
                (abs(x - HOME_TILES_X) <= DRIFT_X_TILES + 0.01f) shouldBe true
                (abs(y - HOME_TILES_Y) <= DRIFT_Y_TILES + 0.01f) shouldBe true
                minX = minOf(minX, x)
                maxX = maxOf(maxX, x)
                false
            }
            (maxX - minX > 0.7f) shouldBe true
        }

        it("moves at one constant speed with no easing, and holds still between moves") {
            idleFight()
            rig.ticks(1f)
            val speeds = ArrayList<Float>()
            rig.ticks(RIG_DELTA)
            var last = coreCenter()
            repeat((3f / RIG_DELTA).toInt()) {
                rig.tick()
                val now = coreCenter()
                speeds.add(now.dst(last) / RIG_DELTA / RIG_PPM)
                last = now
            }
            val moving = speeds.filter { it > 0.01f }
            moving.isNotEmpty() shouldBe true
            moving.all { it <= DRIFT_SPEED_TILES + 0.05f } shouldBe true
            (moving.count { abs(it - DRIFT_SPEED_TILES) > 0.05f } <= moving.size / 5) shouldBe true
            var holds = 0
            var still = 0
            speeds.forEach {
                if (it <= 0.01f) still++ else {
                    if (still * RIG_DELTA >= DRIFT_HOLD_SECONDS - 0.05f) holds++
                    still = 0
                }
            }
            (holds >= 2) shouldBe true
        }

        it("draws the eye on whole art pixels while the hit body moves smoothly") {
            idleFight()
            var offGrid = 0
            rig.ticksUntil(3f) {
                val sprite = rig.core().defaultSprite
                val artX = sprite.x / TickFrames.SCALE
                val artY = sprite.y / TickFrames.SCALE
                (abs(artX - Math.round(artX)) <= 0.001f) shouldBe true
                (abs(artY - Math.round(artY)) <= 0.001f) shouldBe true
                val center = coreCenter()
                if (abs(center.x / TickFrames.SCALE - Math.round(center.x / TickFrames.SCALE)) > 0.01f) offGrid++
                false
            }
            (offGrid > 0) shouldBe true
        }
    }

    describe("the damage") {
        it("takes a bullet and the charged shots into the boss health, with no stun") {
            openFight()
            rig.core().takeDamageFrom(rig.bullet()) shouldBe true
            rig.boss.getCurrentHealth() shouldBe ConstVals.MAX_HEALTH - D1000.BULLET_DAMAGE
            rig.boss.fighting shouldBe true
            rig.core().vulnerable shouldBe true
            (drawn().first != "core_vanish") shouldBe true
        }

        it("blinks and ignores the next shot for 0.5 s after a shot hit") {
            openFight()
            rig.core().takeDamageFrom(rig.halfCharge()) shouldBe true
            rig.boss.getCurrentHealth() shouldBe ConstVals.MAX_HEALTH - D1000.HALF_CHARGE_DAMAGE
            var hiddenTicks = 0
            repeat(10) {
                rig.tick()
                if (rig.core().defaultSprite.hidden) hiddenTicks++
            }
            (hiddenTicks > 0) shouldBe true
            rig.core().canBeDamagedBy(rig.bullet()) shouldBe false
            rig.core().canBeDamagedBy(rig.sabre()) shouldBe true
            rig.ticks(D1000Core.HURT_DUR - 10 * RIG_DELTA)
            rig.core().canBeDamagedBy(rig.bullet()) shouldBe true
            rig.core().defaultSprite.hidden shouldBe false
        }

        it("takes a full charge for 4") {
            openFight()
            rig.core().takeDamageFrom(rig.fullCharge()) shouldBe true
            rig.boss.getCurrentHealth() shouldBe ConstVals.MAX_HEALTH - D1000.FULL_CHARGE_DAMAGE
            rig.boss.fighting shouldBe true
        }

        it("takes the sabre as its weakness: damage and the stun") {
            openFight()
            rig.core().takeDamageFrom(rig.sabre()) shouldBe true
            rig.boss.getCurrentHealth() shouldBe ConstVals.MAX_HEALTH - D1000.SABRE_DAMAGE
            rig.boss.fighting shouldBe false
            rig.boss.shielded shouldBe true
            rig.core().vulnerable shouldBe false
            rig.ticksUntil(3f) { !rig.boss.shielded } shouldBe true
            rig.ticks(1f)
            rig.boss.getCurrentHealth() shouldBe ConstVals.MAX_HEALTH - D1000.SABRE_DAMAGE - D1000.STUN_DAMAGE
        }
    }

    describe("the dodge") {
        it("swerves out of the lane of an X shot that comes toward it") {
            idleFight()
            val start = coreCenter()
            shootAtEye(3f)
            seedRoll(low = true)
            rig.ticks(0.25f)
            val moved = coreCenter().sub(start)
            (moved.y >= 1f * RIG_PPM) shouldBe true
            (abs(moved.x) <= 0.5f * RIG_PPM) shouldBe true
            val dodgedY = coreCenter().y
            rig.ticksUntil(0.6f) { drawn().first == "core" && coreCenter().y <= dodgedY - 0.4f * RIG_PPM } shouldBe true
        }

        it("rolls once per shot: a missed roll lets that shot through") {
            idleFight()
            val start = coreCenter()
            shootAtEye(3f)
            seedRoll(low = false)
            rig.tick()
            seedRoll(low = true)
            rig.ticks(0.25f)
            (coreCenter().dst(start) <= 0.5f * RIG_PPM) shouldBe true
        }

        it("ignores a shot that flies away from it") {
            idleFight()
            val start = coreCenter()
            val eye = coreCenter()
            MegaEntityFactory.fetch(Bullet::class)!!.also {
                it.spawn(
                    props(
                        ConstKeys.OWNER pairTo rig.x,
                        ConstKeys.POSITION pairTo Vector2(eye.x + RIG_PPM, eye.y),
                        ConstKeys.TRAJECTORY pairTo Vector2(SHOT_TILES_PER_SECOND * RIG_PPM, 0f)
                    )
                )
                it.getComponent(BodyComponent::class)!!.body.preProcess()
            }
            seedRoll(low = true)
            rig.ticks(0.25f)
            (coreCenter().dst(start) <= 0.5f * RIG_PPM) shouldBe true
        }

        it("swerves away and up when X starts a sabre slash in reach") {
            idleFight()
            rig.placeX(HOME_TILES_X - 1.5f, HOME_TILES_Y - 1.5f)
            val start = coreCenter()
            MegaEntityFactory.fetch(MegamanXSabreSlash::class)!!
                .spawn(props(ConstKeys.OWNER pairTo rig.x, ConstKeys.TYPE pairTo SabreKind.AIR))
            seedRoll(low = true)
            rig.ticks(0.25f)
            val moved = coreCenter().sub(start)
            (moved.x >= 0.7f * RIG_PPM) shouldBe true
            (moved.y >= 0.5f * RIG_PPM) shouldBe true
        }

        it("never dodges while the body is stunned") {
            idleFight()
            rig.boss.stun()
            rig.tick()
            val start = coreCenter()
            shootAtEye(3f)
            seedRoll(low = true)
            rig.ticks(0.25f)
            (coreCenter().dst(start) <= 0.5f * RIG_PPM) shouldBe true
        }

        it("never dodges in the Overdrive rage") {
            rig.spawnBoss()
            rig.hitEye(3)
            rig.ticks(ENTRY_SECONDS)
            rig.boss.bossComponent.phase = BossPhase.FIGHT
            rig.ticks(0.5f)
            rig.boss.enraging shouldBe true
            val start = coreCenter()
            shootAtEye(3f)
            seedRoll(low = true)
            rig.ticks(0.25f)
            (coreCenter().dst(start) <= 0.5f * RIG_PPM) shouldBe true
        }
    }
})
