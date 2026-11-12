package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.projectiles.JunkShot
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import java.io.File
import javax.imageio.ImageIO

private const val TOLERANCE = 0.01f
private const val MID_X_TILES = RIG_ARENA_WIDTH / RIG_PPM / 2f - 2.5f
private const val FLANK_X_TILES = 5f
private const val FLANK_SIDE_TILES = 2.5f
private const val FLANK_UP_TILES = 1f
private const val X_CENTER_TILES = 0.75f
private const val FRAMES_DIR = "../assets/sprites/frames/Bosses3"
private const val ENEMY_FRAMES_DIR = "../assets/sprites/frames/Enemies2"

private data class Glow(val shown: Boolean, val frame: Int)

class D1000EyeGlowTest : DescribeSpec({

    lateinit var rig: D1000Rig

    beforeEach { rig = D1000Rig() }

    afterEach {
        rig.dispose()
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    fun glowSprite() = rig.core().sprites[D1000Core.GLOW]

    fun glow() = glowSprite().let {
        Glow(!it.hidden, if (it.hidden) -1 else Math.round(minOf(it.v, it.v2) * RIG_TEXTURE_SIZE) / D1000Frames.EYE_GLOW_SIZE)
    }

    fun coreShots() = rig.spawnedOf<JunkShot>().count { it.type == JunkShot.Type.CORE }

    fun coreTiles(): Vector2 = rig.core().getComponent(com.maverick.game.engine.world.body.BodyComponent::class)!!
        .body.getCenter(Vector2()).let { Vector2((it.x - RIG_ARENA_X) / RIG_PPM, (it.y - RIG_FLOOR) / RIG_PPM) }

    fun ticksUntilShot(maxSeconds: Float, seen: MutableList<Glow>): Boolean {
        val before = coreShots()
        return rig.ticksUntil(maxSeconds) {
            if (coreShots() > before) true else {
                seen.add(glow())
                false
            }
        }
    }

    fun List<Glow>.checkTell(tellDur: Float) {
        val tellTicks = (tellDur / RIG_DELTA).toInt()
        val tell = takeLast(tellTicks - 1)
        withClue("the glow shows through the tell: $tell") { tell.all { it.shown } shouldBe true }
        withClue("hidden before the tell") { getOrNull(size - tellTicks - 2)?.shown shouldBe false }
        val frames = tell.map { it.frame }
        withClue("the frames run 0 to 3 in hard cuts, then the flash: $frames") {
            frames.distinct() shouldBe (0..D1000Frames.EYE_GLOW_FLASH).toList()
            frames shouldBe frames.sorted()
            val flash = frames.count { it == D1000Frames.EYE_GLOW_FLASH }
            (flash in 2..(D1000Core.GLOW_FLASH_DUR / RIG_DELTA).toInt()) shouldBe true
        }
    }

    fun checkPlace() {
        val eye = rig.core().defaultSprite
        val glow = glowSprite()
        glow.hidden shouldBe false
        glow.width shouldBe (D1000Frames.EYE_GLOW_SIZE * 2f plusOrMinus TOLERANCE)
        (glow.x + glow.width / 2f) shouldBe (eye.x + eye.width / 2f plusOrMinus TOLERANCE)
        (glow.y + glow.height / 2f) shouldBe (eye.y + eye.height / 2f plusOrMinus TOLERANCE)
        glow.isFlipX shouldBe eye.isFlipX
    }

    describe("the pot shot") {
        it("glows through the ${D1000Tuning.POT_TELL_DUR} s tell before an idle shot, centered on the eye, and goes out at the shot") {
            rig.startFight()
            rig.rollFor(D1000Action.WALK, opening)
            val tellTicks = (D1000Tuning.POT_TELL_DUR / RIG_DELTA).toInt()
            var checked = 0
            var budget = 30 * 60
            while (checked < 1 && budget > 0) {
                val seen = ArrayList<Glow>()
                val before = coreShots()
                while (coreShots() == before && budget-- > 0) {
                    seen.add(glow())
                    rig.tick()
                }
                if (coreShots() == before) break
                if (seen.takeLastWhile { it.shown }.size >= tellTicks - 1) {
                    seen.checkTell(D1000Tuning.POT_TELL_DUR)
                    glow().shown shouldBe false
                    checked++
                }
            }
            checked shouldBe 1
        }

        it("flips with the eye when X is on its right") {
            rig.placeX(19f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.SPREAD, opening)
            rig.ticksUntil(3f) { glow().shown } shouldBe true
            rig.core().facingRight shouldBe true
            rig.core().defaultSprite.isFlipX shouldBe true
            checkPlace()
        }

        it("stays dark while the body throws a volley and the eye holds its shots") {
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening.copy(distance = D1000Distance.MID))
            rig.ticksUntil(3f) { rig.boss.chargeProgress() != null } shouldBe true
            rig.ticksUntil(3f) {
                glow().shown shouldBe false
                false
            }
            coreShots() shouldBe 0
            (rig.spawnedOf<D1000Bomb>().size >= 2) shouldBe true
        }
    }

    describe("the burst") {
        it("glows through the ${D1000Tuning.BURST_TELL_DUR} s tell before the first shot only; the later shots of the burst get no glow") {
            rig.startFight()
            rig.rollFor(D1000Action.SUMMON, opening)
            val seen = ArrayList<Glow>()
            ticksUntilShot(3f, seen) shouldBe true
            seen.checkTell(D1000Tuning.BURST_TELL_DUR)
            glow().shown shouldBe false
            rig.ticksUntil(1f) {
                glow().shown shouldBe false
                coreShots() == D1000Core.SHOT_TIMES.size
            } shouldBe true
        }
    }

    describe("the spread") {
        it("glows through the ${D1000Core.SPREAD_TELL_DUR} s tell and goes out at the fan") {
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.SPREAD, opening)
            val seen = ArrayList<Glow>()
            ticksUntilShot(3f, seen) shouldBe true
            seen.checkTell(D1000Core.SPREAD_TELL_DUR)
            coreShots() shouldBe D1000Core.SPREAD_ANGLES.size
            glow().shown shouldBe false
            rig.ticks(0.5f)
            glow().shown shouldBe false
        }

        it("goes out with the eye in the hurt blink") {
            rig.placeX(1f, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.SPREAD, opening)
            rig.ticksUntil(3f) { glow().shown } shouldBe true
            rig.core().takeDamageFrom(rig.halfCharge()) shouldBe true
            var blinks = 0
            repeat(10) {
                rig.tick()
                if (rig.core().defaultSprite.hidden) {
                    blinks++
                    glow().shown shouldBe false
                }
            }
            (blinks > 0) shouldBe true
        }
    }

    describe("the flank") {
        fun arrives() = rig.ticksUntil(4f) {
            val eye = coreTiles()
            eye.x in (FLANK_X_TILES + FLANK_SIDE_TILES - TOLERANCE)..(FLANK_X_TILES + FLANK_SIDE_TILES + TOLERANCE) &&
                eye.y in (X_CENTER_TILES + FLANK_UP_TILES - TOLERANCE)..(X_CENTER_TILES + FLANK_UP_TILES + TOLERANCE)
        }

        listOf(false, true).forEach { hard ->
            val pause = if (hard) D1000Tuning.FLANK_PAUSE_HARD else D1000Tuning.FLANK_PAUSE
            it("${if (hard) "Hard" else "Normal"}: dark in the first half of the pause, glows through the second half, out at the shots") {
                if (hard) rig.services.gameState.setDifficultyMode(DifficultyMode.HARD)
                rig.placeX(FLANK_X_TILES, 0f)
                rig.startFight()
                rig.rollFor(D1000Action.FLANK, opening.copy(hard = hard))
                arrives() shouldBe true
                val seen = ArrayList<Glow>()
                ticksUntilShot(2f, seen) shouldBe true
                val pauseTicks = (pause / RIG_DELTA).toInt()
                withClue("the glow is dark in the first half of the pause: ${seen.takeLast(pauseTicks).map { it.frame }}") {
                    seen.takeLast(pauseTicks).take(pauseTicks / 2 - 3).none { it.shown } shouldBe true
                }
                val pauseSeen = seen.dropLastWhile { !it.shown }
                (seen.size - pauseSeen.size <= 1) shouldBe true
                pauseSeen.checkTell(pause / 2f)
                glow().shown shouldBe false
                rig.ticksUntil(1f) {
                    glow().shown shouldBe false
                    coreShots() == (if (hard) D1000Tuning.FLANK_SHOTS_HARD else D1000Tuning.FLANK_SHOTS)
                } shouldBe true
            }
        }

        it("goes out when a sabre hit in the pause sends the eye away") {
            rig.placeX(FLANK_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.FLANK, opening)
            arrives() shouldBe true
            rig.ticksUntil(1f) { glow().shown } shouldBe true
            rig.core().takeDamageFrom(rig.sabre()) shouldBe true
            rig.tick()
            rig.ticksUntil(1.5f) {
                glow().shown shouldBe false
                false
            }
            rig.boss.stunned shouldBe true
            coreShots() shouldBe 0
        }
    }

    describe("the strip") {
        it("holds ${D1000Frames.EYE_GLOW_FRAMES} frames of ${D1000Frames.EYE_GLOW_SIZE} px in the colors of the eye orb") {
            val strip = ImageIO.read(File("$FRAMES_DIR/D1000Core/glow.png"))
            strip.width shouldBe D1000Frames.EYE_GLOW_SIZE
            strip.height shouldBe D1000Frames.EYE_GLOW_FRAMES * D1000Frames.EYE_GLOW_SIZE
            val orb = ImageIO.read(File("$ENEMY_FRAMES_DIR/JunkShot/core.png"))
            val colors = (0 until orb.width).flatMap { x -> (0 until orb.height).map { y -> orb.getRGB(x, y) } }
                .filter { (it ushr 24) != 0 }.map { it and 0xFFFFFF }.toSet()
            (0 until strip.width).forEach { x ->
                (0 until strip.height).forEach { y ->
                    val argb = strip.getRGB(x, y)
                    if ((argb ushr 24) != 0) withClue("($x, $y)") { colors.contains(argb and 0xFFFFFF) shouldBe true }
                }
            }
        }
    }
})
