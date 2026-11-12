package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.D1000BombMarker
import com.megaman.maverick.game.entities.decorations.D1000DebrisWarning
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.projectiles.JunkShot
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ceil
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tell
import com.megaman.maverick.game.entities.bosses.d1000.D1000Lamp
import com.megaman.maverick.game.entities.bosses.d1000.D1000Orbs
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Context
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tuning
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames

private const val TOLERANCE = 0.01f
private const val STAND_HEIGHT = 92
private const val MID_X_TILES = RIG_ARENA_WIDTH / RIG_PPM / 2f - 2.5f
private const val NORMAL_WALL_PIECES = 5
private const val WALK_HEIGHT = 91
private const val CROUCH_DROP_HEIGHT = 90
private const val FRAMES_DIR = "../assets/sprites/frames/Bosses3"
private val LAMP_COLORS = setOf(0xFCFCFC, 0xF8D878, 0xF8B800, 0xF83800, 0xAC7C00, 0x881400)

class D1000TelegraphsTest : DescribeSpec({

    var rig: D1000Rig? = null

    fun newRig(arenaHeight: Float = RIG_ARENA_HEIGHT) = D1000Rig(arenaHeight).also { rig = it }

    afterEach {
        rig?.dispose()
        rig = null
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    fun damager(pillar: D1000Pillar): IFixture {
        val out = Array<IFixture>()
        pillar.getComponent(BodyComponent::class)!!.body.getFixtures(out, FixtureType.DAMAGER)
        return out.first()
    }

    fun shownTell(sprite: GameSprite) = D1000Tell.entries.first {
        abs(it.red - sprite.color.r) < TOLERANCE && abs(it.green - sprite.color.g) < TOLERANCE &&
            abs(it.blue - sprite.color.b) < TOLERANCE
    }

    describe("the energy pillar") {
        listOf(10f, 12f).forEach { tiles ->
            it("spans the floor to the ceiling of a $tiles-tile arena, in the hit box and in the drawn segments") {
                val rig = newRig(tiles * RIG_PPM)
                val ceiling = RIG_FLOOR + tiles * RIG_PPM
                rig.flyBombs = true
                rig.placeX(MID_X_TILES, 0f)
                rig.startFight()
                rig.rollFor(D1000Action.BRACKET, opening.copy(distance = D1000Distance.MID))
                rig.ticksUntil(4f) { rig.spawnedOf<D1000Pillar>().isNotEmpty() } shouldBe true
                val pillar = rig.spawnedOf<D1000Pillar>().first()
                rig.ticks(0.2f)
                damager(pillar).isActive() shouldBe true
                val box = damager(pillar).getShape() as GameRectangle
                box.getY() shouldBe (RIG_FLOOR plusOrMinus TOLERANCE)
                box.getMaxY() shouldBe (ceiling plusOrMinus TOLERANCE)

                val segments = pillar.sprites.keys().filter { it.toString().startsWith("pillar_") }
                    .map { pillar.sprites[it] }.filter { !it.hidden }
                segments.size shouldBe ceil(tiles / 2f).toInt()
                segments.minOf { it.y } shouldBe (RIG_FLOOR plusOrMinus TOLERANCE)
                segments.maxOf { it.y + it.height } shouldBe (ceiling plusOrMinus TOLERANCE)
                segments.forEach { abs(it.x + it.width / 2f - box.getCenter(Vector2()).x) shouldBe (0f plusOrMinus 1f) }
            }
        }
    }

    describe("the orb charge in the mouth") {
        data class Charge(
            val action: D1000Action, val tier: D1000Tier, val hard: Boolean, val orbs: Int, val first: Float,
            val distance: D1000Distance = D1000Distance.MID
        )

        listOf(
            Charge(D1000Action.BRACKET, D1000Tier.NORMAL, false, 3, D1000Orbs.MARK_DUR),
            Charge(D1000Action.BRACKET, D1000Tier.NORMAL, true, 4, D1000Orbs.MARK_DUR),
            Charge(D1000Action.FENCE, D1000Tier.NORMAL, false, 5, D1000Orbs.MARK_DUR, opening.distance)
        ).forEach { case ->
            val mode = if (case.hard) "Hard" else "Normal"
            it("${case.action} ($mode): before each orb the charge grows and glows at the throw point for its lead, then goes at the throw") {
                val rig = newRig()
                if (case.hard) rig.services.gameState.setDifficultyMode(DifficultyMode.HARD)
                rig.flyBombs = false
                rig.placeX(if (case.action == D1000Action.FENCE) 5f else MID_X_TILES, 0f)
                rig.startFight()
                rig.rollFor(
                    case.action,
                    opening.copy(distance = case.distance, hard = case.hard,
                        cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, case.hard))
                )
                val charge = rig.boss.sprites[D1000.CHARGE]
                val glow = rig.boss.sprites[D1000.CHARGE_GLOW]
                var shownRun = 0
                var lastCenter = Vector2()
                val frames = mutableListOf<Float>()
                var maxGlow = 0f
                var orbs = 0
                var tick = 0
                while (orbs < case.orbs && tick < 16 * 60) {
                    rig.tick()
                    tick++
                    val bombs = rig.spawnedOf<D1000Bomb>()
                    if (bombs.size > orbs) {
                        val lead = case.first
                        withClue("orb $orbs: charge shown for $shownRun ticks, lead $lead s") {
                            (shownRun >= (lead * 60f).toInt() - 1) shouldBe true
                        }
                        withClue("the orb leaves from the charge point") {
                            val center = bombs.last().getComponent(BodyComponent::class)!!.body.getCenter(Vector2())
                            center.x shouldBe (lastCenter.x plusOrMinus 1f)
                            center.y shouldBe (lastCenter.y plusOrMinus 1f)
                        }
                        charge.hidden shouldBe true
                        if (orbs == 0) {
                            withClue("the charge grows over hard-cut frames, then flashes: $frames") {
                                val distinct = frames.distinct()
                                distinct.size shouldBe D1000Frames.CHARGE_FRAMES
                                distinct.take(D1000Frames.CHARGE_GROW_FRAMES) shouldBe
                                    distinct.take(D1000Frames.CHARGE_GROW_FRAMES).sorted()
                            }
                            maxGlow shouldBe (1f plusOrMinus TOLERANCE)
                        }
                        orbs = bombs.size
                        shownRun = 0
                        frames.clear()
                        maxGlow = 0f
                    } else if (!charge.hidden) {
                        shownRun++
                        frames.add(charge.v)
                        maxGlow = maxOf(maxGlow, glow.color.a)
                        glow.hidden shouldBe false
                        lastCenter = Vector2(charge.x + charge.width / 2f, charge.y + charge.height / 2f)
                    }
                }
                orbs shouldBe case.orbs
            }
        }

        it("the charge and glow strips hold ${D1000Frames.CHARGE_FRAMES} frames of the pillar orb blues") {
            listOf("charge", "charge_glow").forEach { name ->
                val image = ImageIO.read(File("$FRAMES_DIR/D1000/$name.png"))
                image.width shouldBe D1000Frames.CHARGE_SIZE
                image.height shouldBe D1000Frames.CHARGE_FRAMES * D1000Frames.CHARGE_SIZE
            }
            val bomb = ImageIO.read(File("$FRAMES_DIR/D1000Bomb/bomb.png"))
            val blues = (0 until bomb.width).flatMap { x -> (0 until bomb.height).map { y -> bomb.getRGB(x, y) } }
                .filter { (it ushr 24) != 0 }.map { it and 0xFFFFFF }.toSet()
            val charge = ImageIO.read(File("$FRAMES_DIR/D1000/charge.png"))
            (0 until charge.width).forEach { x ->
                (0 until charge.height).forEach { y ->
                    val argb = charge.getRGB(x, y)
                    if ((argb ushr 24) != 0) blues.contains(argb and 0xFFFFFF) shouldBe true
                }
            }
        }
    }

    describe("the bomb landing marker") {
        it("shows the landing lane under the eye from the lock start, holds it ${D1000Orbs.MARK_DUR} s while X moves, and the orb flies to it") {
            val rig = newRig()
            rig.flyBombs = true
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening.copy(distance = D1000Distance.MID))
            rig.ticksUntil(4f) { rig.spawnedOf<D1000BombMarker>().isNotEmpty() } shouldBe true
            rig.boss.chargeProgress()!! shouldBe (0f plusOrMinus 0.05f)
            val marker = rig.spawnedOf<D1000BombMarker>().single()
            val held = marker.position.x
            val eye = rig.core().getComponent(BodyComponent::class)!!.body.getCenter(Vector2()).x
            held shouldBe (eye plusOrMinus TOLERANCE)
            rig.placeX(MID_X_TILES - 3f, 0f)
            var ticks = 0
            while (rig.spawnedOf<D1000Bomb>().isEmpty() && ticks < 120) {
                rig.tick()
                marker.position.x shouldBe (held plusOrMinus TOLERANCE)
                ticks++
            }
            (ticks in (D1000Orbs.MARK_DUR * 60f).toInt() - 2..(D1000Orbs.MARK_DUR * 60f).toInt() + 2) shouldBe true
            val bomb = rig.spawnedOf<D1000Bomb>().single()
            bomb.targetX shouldBe (held plusOrMinus TOLERANCE)
            rig.spawnedOf<D1000BombMarker>().size shouldBe 1
            rig.destroyed.contains(marker, true) shouldBe false
        }

        it("shows on the floor at each bomb's landing x from the throw, and leaves when that bomb lands") {
            val rig = newRig()
            rig.flyBombs = true
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening.copy(distance = D1000Distance.MID))
            rig.ticksUntil(4f) { rig.spawnedOf<D1000Bomb>().size == 1 } shouldBe true
            val bombs = rig.spawnedOf<D1000Bomb>()
            val markers = rig.spawnedOf<D1000BombMarker>()
            markers.size shouldBe 1
            bombs.zip(markers).forEach { (bomb, marker) ->
                marker.position.x shouldBe (rig.landingX(bomb) plusOrMinus TOLERANCE)
                marker.position.y shouldBe (RIG_FLOOR plusOrMinus TOLERANCE)
                rig.destroyed.contains(marker, true) shouldBe false
            }
            rig.tick()
            markers.forEach { it.defaultSprite.y shouldBe (RIG_FLOOR plusOrMinus TOLERANCE) }

            var ticks = 0
            while (bombs.any { !rig.destroyed.contains(it, true) } && ticks++ < 120) {
                bombs.zip(markers).forEach { (bomb, marker) ->
                    rig.destroyed.contains(marker, true) shouldBe rig.destroyed.contains(bomb, true)
                }
                rig.tick()
            }
            markers.forEach { rig.destroyed.contains(it, true) shouldBe true }
        }
    }

    describe("the debris warning") {
        it("shows at the ceiling over each Ceiling Crash lane, and the hanging chunk falls from the hang point after the lead") {
            val rig = newRig()
            fun junk() = rig.spawnedOf<D1000CeilingDebris>()
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.CEILING, opening.copy(distance = D1000Distance.MID))
            rig.ticksUntil(3f) { rig.spawnedOf<D1000DebrisWarning>().isNotEmpty() } shouldBe true
            val warnings = rig.spawnedOf<D1000DebrisWarning>()
            warnings.size shouldBe 6
            junk().size shouldBe 0
            rig.tick()
            warnings.forEach {
                val dust = it.sprites["dust"]
                dust.y + dust.height shouldBe (RIG_FLOOR + RIG_ARENA_HEIGHT plusOrMinus TOLERANCE)
            }
            rig.ticks(D1000DebrisWarning.LEAD_DUR - 0.1f)
            junk().size shouldBe 0
            rig.ticksUntil(0.2f) { junk().size >= 6 } shouldBe true
            junk().size shouldBe 6
            warnings.zip(junk()).forEach { (warning, piece) ->
                val center = piece.getComponent(BodyComponent::class)!!.body.getCenter(Vector2())
                center.x shouldBe (warning.hangPoint.x plusOrMinus TOLERANCE)
                center.y shouldBe (warning.hangPoint.y plusOrMinus TOLERANCE)
                piece.key shouldBe warning.chunkKey
                rig.destroyed.contains(warning, true) shouldBe false
            }
            rig.ticks(D1000CeilingDebris.MAX_LAST_DELAY + 0.05f)
            warnings.forEach { rig.destroyed.contains(it, true) shouldBe true }
            junk().size shouldBe warnings.sumOf { it.pieces.size }
        }

        it("Wall Crash: a warning comes before each piece, at the same place") {
            val rig = newRig()
            fun junk() = rig.spawnedOf<D1000CeilingDebris>()
            rig.startFight()
            rig.rollFor(D1000Action.RAM, opening)
            rig.ticksUntil(4f) { rig.spawnedOf<D1000DebrisWarning>().size == NORMAL_WALL_PIECES } shouldBe true
            junk().size shouldBe 0
            rig.ticksUntil(D1000DebrisWarning.LEAD_DUR + 0.1f) { junk().size >= NORMAL_WALL_PIECES } shouldBe true
            junk().size shouldBe NORMAL_WALL_PIECES
            rig.spawnedOf<D1000DebrisWarning>().zip(junk()).forEach { (warning, piece) ->
                piece.getComponent(BodyComponent::class)!!.body.getCenter(Vector2()).x shouldBe
                    (warning.position.x plusOrMinus TOLERANCE)
            }
        }
    }

    describe("the head lamp") {
        data class Case(
            val action: D1000Action,
            val tell: D1000Tell,
            val context: D1000Context,
            val xTiles: Float,
            val startsWithNewPose: Boolean
        )

        val mid = opening.copy(distance = D1000Distance.MID)
        listOf(
            Case(D1000Action.RAM, D1000Tell.RAM, opening, 1f, true),
            Case(D1000Action.BRACKET, D1000Tell.BOMB, mid, MID_X_TILES, true),
            Case(D1000Action.CEILING, D1000Tell.SLAM, mid, MID_X_TILES, true),
            Case(D1000Action.SPREAD, D1000Tell.EYE, opening, 1f, false)
        ).forEach { case ->
            it("blinks the ${case.tell} color for 0.4 s before ${case.action} starts, then holds it") {
                val rig = newRig()
                rig.placeX(case.xTiles, 0f)
                rig.startFight()
                rig.rollFor(case.action, case.context)
                val lamp = rig.boss.sprites[D1000.LAMP]
                rig.tick()
                shownTell(lamp) shouldBe D1000Tell.OFF

                var lit = 0
                rig.ticksUntil(1f) { shownTell(lamp) == case.tell.also { lit++ } } shouldBe true
                rig.boss.defaultSprite.regionHeight shouldBe STAND_HEIGHT

                val lead = Array<D1000Tell>()
                var poseTicks = -1
                for (i in 0 until 45) {
                    rig.tick()
                    lead.add(shownTell(lamp))
                    if (poseTicks < 0 && rig.boss.defaultSprite.regionHeight != STAND_HEIGHT) poseTicks = i + 1
                }
                val leadTicks = (D1000Lamp.LEAD_DUR * 60f).toInt()
                withClue("the lamp blinks in the lead: $lead") {
                    (0 until leadTicks - 2).any { lead[it] == D1000Tell.OFF } shouldBe true
                }
                (0 until 45).all { lead[it] == case.tell || lead[it] == D1000Tell.OFF } shouldBe true
                (leadTicks + 2 until 45).all { lead[it] == case.tell } shouldBe true
                if (case.startsWithNewPose) poseTicks shouldBeIn (leadTicks plusOrMinus 3)
            }
        }

        it("goes off when the action ends") {
            val rig = newRig()
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening.copy(distance = D1000Distance.MID))
            val lamp = rig.boss.sprites[D1000.LAMP]
            rig.ticksUntil(4f) { rig.spawnedOf<D1000Bomb>().isNotEmpty() } shouldBe true
            shownTell(lamp) shouldBe D1000Tell.BOMB
            rig.ticksUntil(10f) { rig.boss.defaultSprite.regionHeight == WALK_HEIGHT } shouldBe true
            shownTell(lamp) shouldBe D1000Tell.OFF
        }

        data class Combo(val first: D1000Action, val followUp: D1000Action, val xTiles: Float, val context: D1000Context)

        listOf(
            Combo(D1000Action.RAM, D1000Action.CROUCH, 1f, opening),
            Combo(D1000Action.FENCE, D1000Action.RAM, 2.55f, opening)
        ).forEach { combo ->
            it("blinks the color of the follow-up ${combo.followUp} at least 0.4 s before the end of ${combo.first}") {
                val rig = newRig()
                rig.placeX(combo.xTiles, 0f)
                rig.startFight()
                rig.rollFor(combo.first, combo.context)
                val lamp = rig.boss.sprites[D1000.LAMP]
                val first = combo.first.family
                val next = combo.followUp.family
                rig.ticksUntil(3f) { shownTell(lamp) == first && rig.boss.defaultSprite.regionHeight != STAND_HEIGHT } shouldBe
                    true
                val nextPose = if (combo.followUp == D1000Action.CROUCH) CROUCH_DROP_HEIGHT else WALK_HEIGHT

                val seen = Array<D1000Tell>()
                var tellTick = -1
                var startTick = -1
                var tick = 0
                while (startTick < 0 && tick < 24 * 60) {
                    rig.tick()
                    tick++
                    val shown = shownTell(lamp)
                    if (tellTick < 0 && shown == next && next != first) tellTick = tick
                    if (tellTick >= 0) seen.add(shown)
                    if (tellTick >= 0 && rig.boss.defaultSprite.regionHeight == nextPose) startTick = tick
                }
                withClue("the follow-up color shows (first $first, next $next)") { (tellTick > 0) shouldBe true }
                withClue("the follow-up starts") { (startTick > 0) shouldBe true }
                withClue("lead ticks ${startTick - tellTick}") {
                    (startTick - tellTick >= (D1000Lamp.LEAD_DUR * 60f).toInt() - 1) shouldBe true
                }
                withClue("the lamp blinks the follow-up color: $seen") {
                    seen.any { it == D1000Tell.OFF } shouldBe true
                    seen.all { it == next || it == D1000Tell.OFF } shouldBe true
                }
            }
        }

        it("the lamp strip holds only the lamp pixels of each body frame, at the lamp place of the art") {
            val strip = ImageIO.read(File("$FRAMES_DIR/D1000/lamp.png"))
            strip.height shouldBe D1000Frames.LAMP_FRAMES * D1000Frames.LAMP_HEIGHT
            var lit = 0
            D1000Frames.BODY.keys().toList().forEach { anim ->
                val def = D1000Frames.BODY[anim]
                val art = ImageIO.read(File("$FRAMES_DIR/D1000/$anim.png"))
                for (frame in 0 until def.count) {
                    val top = D1000Frames.lampFrame(anim, frame) * D1000Frames.LAMP_HEIGHT
                    for (y in 0 until D1000Frames.LAMP_HEIGHT) for (x in 0 until D1000Frames.LAMP_WIDTH) {
                        if (strip.getRGB(x, top + y) ushr 24 == 0) continue
                        lit++
                        val pixel = art.getRGB(D1000Frames.LAMP_LEFT + x, frame * def.height + D1000Frames.LAMP_TOP + y)
                        withClue("$anim $frame ($x, $y)") { LAMP_COLORS.contains(pixel and 0xFFFFFF) shouldBe true }
                    }
                }
            }
            (lit > 0) shouldBe true
        }
    }

    describe("the glowing face and the moving parts") {
        data class Case(
            val action: D1000Action,
            val tell: D1000Tell,
            val context: D1000Context,
            val xTiles: Float,
            val parts: List<String>
        )

        val mid = opening.copy(distance = D1000Distance.MID)
        val allParts = listOf(D1000.MOUTH, D1000.SCREW)
        listOf(
            Case(D1000Action.RAM, D1000Tell.RAM, opening, 1f, emptyList()),
            Case(D1000Action.BRACKET, D1000Tell.BOMB, mid, MID_X_TILES, listOf(D1000.MOUTH)),
            Case(D1000Action.CEILING, D1000Tell.SLAM, mid, MID_X_TILES, emptyList()),
            Case(D1000Action.SPREAD, D1000Tell.EYE, opening, 1f, listOf(D1000.SCREW))
        ).forEach { case ->
            it("moves only the ${case.parts} parts and pulses the ${case.tell} glow in the lead of ${case.action}") {
                val rig = newRig()
                rig.placeX(case.xTiles, 0f)
                rig.startFight()
                rig.rollFor(case.action, case.context)
                val lamp = rig.boss.sprites[D1000.LAMP]
                val glow = rig.boss.sprites[D1000.GLOW]
                val face = rig.boss.sprites[D1000.FACE]
                rig.tick()
                allParts.all { rig.boss.sprites[it].hidden } shouldBe true
                face.color.r shouldBe (D1000Lamp.FACE_IDLE_RED plusOrMinus TOLERANCE)
                face.color.g shouldBe (D1000Lamp.FACE_IDLE_GREEN plusOrMinus TOLERANCE)
                glow.color.a shouldBe (D1000Lamp.IDLE_GLOW plusOrMinus TOLERANCE)

                rig.ticksUntil(1f) { shownTell(lamp) == case.tell } shouldBe true
                val shownTicks = allParts.associateWith { 0 }.toMutableMap()
                val regions = HashSet<Float>()
                var brightest = 0f
                repeat((D1000Lamp.LEAD_DUR * 60f).toInt()) {
                    rig.tick()
                    allParts.forEach { key -> if (!rig.boss.sprites[key].hidden) shownTicks[key] = shownTicks[key]!! + 1 }
                    if (!rig.boss.sprites[D1000.SCREW].hidden) regions.add(rig.boss.sprites[D1000.SCREW].v)
                    brightest = maxOf(brightest, glow.color.a)
                    withClue("the eyes and the mouth hold the lamp family color") { shownTell(face) shouldBe case.tell }
                    withClue("the glow has the lamp family color") {
                        glow.color.r shouldBe (case.tell.red plusOrMinus TOLERANCE)
                        glow.color.g shouldBe (case.tell.green plusOrMinus TOLERANCE)
                    }
                }
                withClue("parts shown in the lead: $shownTicks") {
                    case.parts.all { shownTicks[it]!! > 0 } shouldBe true
                    (allParts - case.parts.toSet()).all { shownTicks[it] == 0 } shouldBe true
                }
                brightest shouldBe (1f plusOrMinus TOLERANCE)
                if (case.tell == D1000Tell.EYE) withClue("the screw turns") { (regions.size > 1) shouldBe true }
            }
        }

        it("keeps the parts still and the glow at idle after the action ends") {
            val rig = newRig()
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            rig.rollFor(D1000Action.BRACKET, opening.copy(distance = D1000Distance.MID))
            rig.ticksUntil(4f) { rig.spawnedOf<D1000Bomb>().isNotEmpty() } shouldBe true
            rig.boss.sprites[D1000.MOUTH].hidden shouldBe false
            rig.ticksUntil(10f) { rig.boss.defaultSprite.regionHeight == WALK_HEIGHT } shouldBe true
            allParts.all { rig.boss.sprites[it].hidden } shouldBe true
            rig.boss.sprites[D1000.GLOW].color.a shouldBe (D1000Lamp.IDLE_GLOW plusOrMinus TOLERANCE)
        }

        it("the face strip holds only the eye, side light and mouth slit pixels of the lit frames") {
            val strip = ImageIO.read(File("$FRAMES_DIR/D1000/face.png"))
            val lamp = ImageIO.read(File("$FRAMES_DIR/D1000/lamp.png"))
            strip.height shouldBe D1000Frames.LAMP_FRAMES * D1000Frames.LAMP_HEIGHT
            val litFrames = HashSet<String>()
            D1000Frames.BODY.keys().toList().forEach { anim ->
                val def = D1000Frames.BODY[anim]
                val art = ImageIO.read(File("$FRAMES_DIR/D1000/$anim.png"))
                for (frame in 0 until def.count) {
                    val top = D1000Frames.lampFrame(anim, frame) * D1000Frames.LAMP_HEIGHT
                    for (y in 0 until D1000Frames.LAMP_HEIGHT) for (x in 0 until D1000Frames.LAMP_WIDTH) {
                        if (strip.getRGB(x, top + y) ushr 24 == 0) continue
                        litFrames.add("$anim $frame")
                        withClue("$anim $frame ($x, $y) is not also a lamp pixel") {
                            (lamp.getRGB(x, top + y) ushr 24) shouldBe 0
                        }
                        val pixel = art.getRGB(D1000Frames.LAMP_LEFT + x, frame * def.height + D1000Frames.LAMP_TOP + y)
                        withClue("$anim $frame ($x, $y)") { LAMP_COLORS.contains(pixel and 0xFFFFFF) shouldBe true }
                    }
                }
            }
            withClue("the stun frames keep X6's dark eyes: $litFrames") {
                listOf("stagger 1", "fall 0", "land 0", "stand_up 0").none { it in litFrames } shouldBe true
                listOf("stand 0", "walk 0", "ram 0", "throw 3", "crouch_shake 0").all { it in litFrames } shouldBe true
            }
        }

        it("the glow strip lights only the black art next to the face lights") {
            val strip = ImageIO.read(File("$FRAMES_DIR/D1000/glow.png"))
            strip.height shouldBe D1000Frames.LAMP_FRAMES * D1000Frames.LAMP_HEIGHT
            var lit = 0
            D1000Frames.BODY.keys().toList().forEach { anim ->
                val def = D1000Frames.BODY[anim]
                val art = ImageIO.read(File("$FRAMES_DIR/D1000/$anim.png"))
                for (frame in 0 until def.count) {
                    val top = D1000Frames.lampFrame(anim, frame) * D1000Frames.LAMP_HEIGHT
                    for (y in 0 until D1000Frames.LAMP_HEIGHT) for (x in 0 until D1000Frames.LAMP_WIDTH) {
                        if (strip.getRGB(x, top + y) ushr 24 == 0) continue
                        lit++
                        val pixel = art.getRGB(D1000Frames.LAMP_LEFT + x, frame * def.height + D1000Frames.LAMP_TOP + y)
                        withClue("$anim $frame ($x, $y)") { pixel shouldBe 0xFF000000.toInt() }
                    }
                }
            }
            (lit > 0) shouldBe true
        }

        it("the part strips hold one entry per body frame and pose, and every nozzle anchor is inside its body frame") {
            ImageIO.read(File("$FRAMES_DIR/D1000/mouth.png")).height shouldBe
                D1000Frames.LAMP_FRAMES * D1000Frames.MOUTH_HEIGHT
            ImageIO.read(File("$FRAMES_DIR/D1000/screw.png")).height shouldBe
                D1000Frames.LAMP_FRAMES * D1000Frames.SCREW_POSES * D1000Frames.SCREW_HEIGHT
            ImageIO.read(File("$FRAMES_DIR/D1000/puff.png")).height shouldBe
                D1000Frames.PUFF_FRAMES * D1000Frames.PUFF_HEIGHT
            D1000Frames.BODY.keys().toList().forEach { anim ->
                val def = D1000Frames.BODY[anim]
                val nozzles = D1000Frames.NOZZLES[anim]
                nozzles.size shouldBe def.count
                nozzles.forEachIndexed { frame, anchor ->
                    anchor.size shouldBe 2
                    withClue("$anim nozzle $frame") {
                        (anchor[0] in 0 until def.width && anchor[1] in 0 until def.height) shouldBe true
                    }
                }
            }
        }
    }
})

private infix fun Int.plusOrMinus(tolerance: Int) = (this - tolerance)..(this + tolerance)

private infix fun Int.shouldBeIn(range: IntRange) = withClue("$this in $range") { (this in range) shouldBe true }
