package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames
import com.megaman.maverick.game.entities.bosses.d1000.D1000Orbs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.utils.TickFrames
import com.maverick.game.engine.drawables.sprites.GameSprite
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import java.io.File
import javax.imageio.ImageIO

private const val TOLERANCE = 0.01f
private const val FRAMES_DIR = "../assets/sprites/frames/Bosses3"
private const val MID_X_TILES = 8.25f

private data class BeamTick(
    val shown: Boolean, val frame: Int, val bottom: Float, val height: Float, val centerX: Float,
    val pupilCenterX: Float, val pupilY: Float, val eyeX: Float, val eyeDrawY: Float, val bombs: Int,
    val thrusterShown: Boolean, val thrusterFrame: Int, val thrusterCenterX: Float, val thrusterTop: Float
)

class D1000EyeBeamTest : DescribeSpec({

    var rig: D1000Rig? = null

    afterEach {
        rig?.dispose()
        rig = null
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false), distance = D1000Distance.MID)

    fun stripFrame(sprite: GameSprite, height: Int) =
        if (sprite.hidden) -1 else Math.round(minOf(sprite.v, sprite.v2) * RIG_TEXTURE_SIZE) / height

    fun sample(rig: D1000Rig): BeamTick {
        val beam = rig.core().sprites[D1000Core.BEAM]
        val pupil = rig.core().sprites[D1000Core.PUPIL]
        val thruster = rig.core().sprites[D1000Core.THRUSTER]
        val eye = rig.core().getComponent(BodyComponent::class)!!.body.getCenter(Vector2())
        val drawn = rig.core().defaultSprite
        return BeamTick(
            !beam.hidden, stripFrame(beam, D1000Frames.BEAM_HEIGHT),
            beam.y, beam.height, beam.x + beam.width / 2f, pupil.x + pupil.width / 2f, pupil.y, eye.x,
            drawn.y + drawn.height / 2f, rig.spawnedOf<D1000Bomb>().size,
            !thruster.hidden, stripFrame(thruster, D1000Frames.THRUSTER_SIZE),
            thruster.x + thruster.width / 2f, thruster.y + thruster.height
        )
    }

    fun volley(hard: Boolean, count: Int): Pair<D1000Rig, List<BeamTick>> {
        val made = D1000Rig().also { rig = it }
        made.flyBombs = false
        if (hard) made.services.gameState.setDifficultyMode(DifficultyMode.HARD)
        made.placeX(MID_X_TILES, 0f)
        made.startFight()
        made.rollFor(D1000Action.BRACKET, opening.copy(hard = hard))
        val ticks = ArrayList<BeamTick>()
        made.ticksUntil(12f) {
            ticks.add(sample(made))
            made.spawnedOf<D1000Bomb>().size == count
        } shouldBe true
        return made to ticks
    }

    fun List<BeamTick>.runs(): List<IntRange> {
        val out = ArrayList<IntRange>()
        var start = -1
        forEachIndexed { i, t ->
            if (t.shown && start < 0) start = i
            if (!t.shown && start >= 0) {
                out.add(start until i)
                start = -1
            }
        }
        if (start >= 0) out.add(start until size)
        return out
    }

    describe("the eye's flashlight in a volley") {
        listOf(false to D1000Tuning.BRACKET_COUNT, true to D1000Tuning.BRACKET_COUNT_HARD).forEach { (hard, count) ->
            it("shines from the pupil to the floor only while the eye holds each mark (${if (hard) "Hard" else "Normal"}), then flashes before the orb") {
                val (made, ticks) = volley(hard, count)
                val lock = D1000Orbs.MARK_DUR
                val runs = ticks.runs()
                runs.size shouldBe count
                runs.forEachIndexed { mark, range ->
                    val run = ticks.subList(range.first, range.last + 1)
                    withClue("mark $mark: ${run.size} ticks") {
                        (run.size in (lock / RIG_DELTA).toInt() - 2..(lock / RIG_DELTA).toInt() + 1) shouldBe true
                        run.map { it.eyeX }.distinct().size shouldBe 1
                        run.all { it.bombs == mark } shouldBe true
                        run.forEach {
                            it.bottom shouldBe (RIG_FLOOR plusOrMinus TOLERANCE)
                            (it.height > 7f * RIG_PPM) shouldBe true
                            (it.bottom + it.height >= it.pupilY) shouldBe true
                            it.centerX shouldBe (it.pupilCenterX plusOrMinus TOLERANCE)
                            it.centerX shouldBe (it.eyeX plusOrMinus 2f * TickFrames.SCALE + TickFrames.SCALE / 2f)
                        }
                        val frames = run.map { it.frame }
                        val flash = frames.dropWhile { it < D1000Frames.BEAM_LOOP_FRAMES }
                        withClue("frames $frames") {
                            frames.takeWhile { it < D1000Frames.BEAM_LOOP_FRAMES }.distinct().size shouldBe
                                minOf(D1000Frames.BEAM_LOOP_FRAMES, frames.size - flash.size)
                            flash shouldBe flash.sorted()
                            flash.distinct() shouldBe
                                (D1000Frames.BEAM_LOOP_FRAMES until D1000Frames.BEAM_LOOP_FRAMES + D1000Frames.BEAM_FLASH_FRAMES).toList()
                        }
                    }
                }
                val lanes = made.spawnedOf<D1000Bomb>().map { it.targetX }
                runs.forEachIndexed { mark, range -> ticks[range.last].centerX shouldBe (lanes[mark] plusOrMinus 0.2f * RIG_PPM) }
            }
        }

        it("has art with ${D1000Frames.BEAM_LOOP_FRAMES} loop frames and ${D1000Frames.BEAM_FLASH_FRAMES} flash frames") {
            val art = ImageIO.read(File("$FRAMES_DIR/D1000Core/beam.png"))
            art.width shouldBe D1000Frames.BEAM_WIDTH
            art.height shouldBe D1000Frames.BEAM_HEIGHT * (D1000Frames.BEAM_LOOP_FRAMES + D1000Frames.BEAM_FLASH_FRAMES)
        }
    }

    describe("the eye's thruster") {
        it("shows under the nozzle; it loops the idle flame while the eye holds a mark and flares in the signal and the flight before it") {
            val (_, ticks) = volley(false, D1000Tuning.BRACKET_COUNT)
            val runs = ticks.runs()
            ticks.forEach {
                it.thrusterShown shouldBe true
                it.thrusterCenterX shouldBe (it.eyeX plusOrMinus TickFrames.SCALE + TOLERANCE)
                it.thrusterTop shouldBe (it.eyeDrawY - (D1000Frames.THRUSTER_TOP - 12) * TickFrames.SCALE plusOrMinus TOLERANCE)
            }
            runs.forEach { range ->
                withClue("lock at tick ${range.first}") {
                    ticks.subList(range.first, range.last + 1).all { it.thrusterFrame < D1000Frames.THRUSTER_IDLE_FRAMES } shouldBe true
                    ticks.subList(range.first - 3, range.first).all { it.thrusterFrame >= D1000Frames.THRUSTER_IDLE_FRAMES } shouldBe true
                }
            }
        }

        it("bounces the eye down ${D1000Core.BOUNCE_DIP / TickFrames.SCALE} art px in each signal, then back") {
            val (_, ticks) = volley(false, D1000Tuning.BRACKET_COUNT)
            val runs = ticks.runs()
            for (k in 1 until runs.size) {
                val rest = ticks[runs[k - 1].last].eyeDrawY
                val gap = ticks.subList(runs[k - 1].last + 1, runs[k].first)
                withClue("signal before mark $k") {
                    gap.minOf { it.eyeDrawY } shouldBe (rest - D1000Core.BOUNCE_DIP plusOrMinus TickFrames.SCALE + TOLERANCE)
                    ticks[runs[k].first].eyeDrawY shouldBe (rest plusOrMinus TOLERANCE)
                }
            }
        }

        it("has art with ${D1000Frames.THRUSTER_IDLE_FRAMES} idle frames and ${D1000Frames.THRUSTER_FLARE_FRAMES} flare frames") {
            val art = ImageIO.read(File("$FRAMES_DIR/D1000Core/thruster.png"))
            art.width shouldBe D1000Frames.THRUSTER_SIZE
            art.height shouldBe D1000Frames.THRUSTER_SIZE * (D1000Frames.THRUSTER_IDLE_FRAMES + D1000Frames.THRUSTER_FLARE_FRAMES)
        }
    }
})
