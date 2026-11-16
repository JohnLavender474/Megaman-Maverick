package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.D1000DebrisWarning
import com.megaman.maverick.game.entities.decorations.DustPuff
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.megamanx.MegamanXValues
import com.megaman.maverick.game.entities.state.BossPhase
import com.megaman.maverick.game.players.PlayerDamageNegotiator
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import com.megaman.maverick.game.entities.bosses.d1000.D1000Moves
import com.megaman.maverick.game.entities.bosses.d1000.D1000Escape
import com.megaman.maverick.game.entities.bosses.d1000.D1000Tier
import com.megaman.maverick.game.entities.bosses.d1000.D1000Distance
import com.megaman.maverick.game.entities.bosses.d1000.D1000Action
import com.megaman.maverick.game.entities.bosses.d1000.D1000Director
import com.megaman.maverick.game.entities.bosses.d1000.D1000Ceiling

private const val TOLERANCE = 0.01f
private const val MID_X_TILES = 10.75f - 2.5f
private const val LANE_GAP = 2.5f * RIG_PPM
private const val FRAMES_DIR = "../assets/sprites/frames/Bosses3"
private val SEEDS = (1L..8L).toList()

class D1000CeilingDebrisTest : DescribeSpec({

    var rig: D1000Rig? = null

    afterEach {
        rig?.dispose()
        rig = null
        MegaEntityFactory.reset()
        unmockkAll()
    }

    val opening = d1000Context(cyclesSinceCombo = D1000Director.comboGap(D1000Tier.NORMAL, false))

    fun newRig() = D1000Rig().also { rig = it }

    fun body(piece: D1000CeilingDebris) = piece.getComponent(BodyComponent::class)!!.body

    fun ceilingCrash(rig: D1000Rig, seed: Long, xTiles: Float = MID_X_TILES, overdrive: Boolean = false) {
        rig.placeX(xTiles, 0f)
        if (overdrive) {
            rig.spawnBoss()
            rig.hitEye(3)
            rig.ticks(ENTRY_SECONDS)
            rig.boss.bossComponent.phase = BossPhase.FIGHT
            rig.ticks(1.15f)
        } else rig.startFight()
        val tier = if (overdrive) D1000Tier.OVERDRIVE else D1000Tier.NORMAL
        rig.rollFor(
            D1000Action.CEILING,
            opening.copy(
                tier = tier,
                distance = if (abs(xTiles - RIG_ARENA_WIDTH / RIG_PPM / 2f) <= 5f) D1000Distance.MID else D1000Distance.FAR,
                cyclesSinceCombo = D1000Director.comboGap(tier, false)
            )
        )
        val start = rig.bossCenter().y
        rig.ticksUntil(6f) { rig.bossCenter().y > start + RIG_PPM } shouldBe true
        rig.randomSource.setSeed(seed)
        rig.ticksUntil(3f) { rig.spawnedOf<D1000DebrisWarning>().isNotEmpty() } shouldBe true
    }

    describe("the lanes of the Ceiling Crash") {
        it("each lane drops 2 to 3 pieces: one big chunk first, then small ones 0.05 to 0.15 s apart, in a spread of 0.3 tile around the lane") {
            val rig = newRig()
            ceilingCrash(rig, 3L)
            val warnings = rig.spawnedOf<D1000DebrisWarning>()
            warnings.size shouldBe 6
            warnings.forEach { warning ->
                withClue("lane ${(warning.position.x - RIG_ARENA_X) / RIG_PPM}") {
                    (warning.pieces.size in D1000CeilingDebris.MIN_PIECES..D1000CeilingDebris.MAX_PIECES) shouldBe true
                    warning.spread shouldBe (D1000Ceiling.CEILING_DEBRIS_SPREAD plusOrMinus TOLERANCE)
                    val first = warning.pieces.first()
                    D1000CeilingDebris.BIG_KEYS.contains(first.key, false) shouldBe true
                    first.startX shouldBe (warning.position.x plusOrMinus TOLERANCE)
                    first.delay shouldBe 0f
                    warning.chunkKey shouldBe first.key
                    var last = 0f
                    warning.pieces.forEachIndexed { i, piece ->
                        if (i > 0) {
                            D1000CeilingDebris.SMALL_KEYS.contains(piece.key, false) shouldBe true
                            (abs(piece.startX - warning.position.x) <= D1000Ceiling.CEILING_DEBRIS_SPREAD + TOLERANCE) shouldBe true
                            val gap = piece.delay - last
                            (gap >= D1000CeilingDebris.MIN_GAP - TOLERANCE && gap <= D1000CeilingDebris.MAX_GAP + TOLERANCE) shouldBe true
                        }
                        last = piece.delay
                        (abs(piece.velocity.x) <= D1000CeilingDebris.MAX_DRIFT_SPEED) shouldBe true
                        (piece.velocity.y <= 0f && piece.velocity.y >= -D1000CeilingDebris.MAX_START_SPEED) shouldBe true
                        (abs(piece.spin) in D1000CeilingDebris.MIN_SPIN..D1000CeilingDebris.MAX_SPIN) shouldBe true
                    }
                }
            }
            val spins = warnings.flatMap { w -> w.pieces.map { it.spin } }
            (spins.any { it > 0f } && spins.any { it < 0f }) shouldBe true
            val keys = warnings.flatMap { w -> w.pieces.map { it.key } }.toSet()
            (keys.size >= 3) shouldBe true
        }

        it("the pieces are rolled again on each spawn: two seeds give different lanes") {
            val a = newRig().let { r ->
                ceilingCrash(r, 1L)
                r.spawnedOf<D1000DebrisWarning>().flatMap { w -> w.pieces.map { Triple(it.key, it.startX, it.velocity.x) } }
            }
            rig!!.dispose()
            MegaEntityFactory.reset()
            val b = newRig().let { r ->
                ceilingCrash(r, 2L)
                r.spawnedOf<D1000DebrisWarning>().flatMap { w -> w.pieces.map { Triple(it.key, it.startX, it.velocity.x) } }
            }
            (a != b) shouldBe true
        }
    }

    describe("the falling piece") {
        it("takes its body size from its chunk, spins, does 1 damage to X, and breaks on the floor with a dust puff") {
            val rig = newRig()
            ceilingCrash(rig, 4L)
            rig.ticksUntil(1f) { rig.spawnedOf<D1000CeilingDebris>().isNotEmpty() } shouldBe true
            val piece = rig.spawnedOf<D1000CeilingDebris>().first()
            val size = D1000CeilingDebris.size(piece.key)
            body(piece).getWidth() shouldBe (size.x plusOrMinus TOLERANCE)
            body(piece).getHeight() shouldBe (size.y plusOrMinus TOLERANCE)
            PlayerDamageNegotiator(rig.x).get(piece) shouldBe 1
            piece.rotation shouldBe 0f
            rig.ticks(0.1f)
            (piece.rotation * piece.spin > 0f) shouldBe true
            (abs(piece.rotation) >= abs(piece.spin) * 0.08f && abs(piece.rotation) <= abs(piece.spin) * 0.12f) shouldBe true
            piece.sprites.values().first().rotation shouldBe (piece.rotation plusOrMinus TOLERANCE)
            rig.ticksUntil(2f) { rig.destroyed.contains(piece, true) } shouldBe true
            (body(piece).getY() <= RIG_FLOOR + TOLERANCE) shouldBe true
            rig.spawnedOf<DustPuff>().isNotEmpty() shouldBe true
        }

        it("the chunk art has the sizes of the size table") {
            D1000CeilingDebris.SIZES.forEach { entry ->
                val image = ImageIO.read(File("$FRAMES_DIR/${D1000CeilingDebris.TAG}/${entry.key}.png"))
                withClue(entry.key) {
                    image.width.toFloat() shouldBe entry.value.x
                    image.height.toFloat() shouldBe entry.value.y
                }
            }
        }

        it("the warning hides its hanging chunk once the big piece falls") {
            val rig = newRig()
            ceilingCrash(rig, 5L)
            val warning = rig.spawnedOf<D1000DebrisWarning>().first()
            rig.tick()
            warning.sprites["chunk"].hidden shouldBe false
            rig.ticksUntil(1f) { rig.spawnedOf<D1000CeilingDebris>().isNotEmpty() } shouldBe true
            rig.tick()
            warning.sprites["chunk"].hidden shouldBe true
        }
    }

    describe("the worst spread") {
        it("the spread plus the biggest chunk stays inside the lane margin at the wall pocket, and leaves X a cell between two lanes") {
            val reach = D1000Ceiling.CEILING_DEBRIS_SPREAD + D1000CeilingDebris.MAX_RADIUS
            (reach <= D1000Ceiling.CEILING_POCKET_LANE) shouldBe true
            val needed = MegamanXValues.BODY_WIDTH * RIG_PPM + 2f * D1000Moves.NORMAL.margin + 2f * D1000Moves.NORMAL.speed * D1000Escape.STEP
            (LANE_GAP - 2f * reach >= needed) shouldBe true
        }

        SEEDS.forEach { seed ->
            listOf(0.6f to true, MID_X_TILES to false).forEach { (xTiles, overdrive) ->
            it("no piece lands in the 1.25-tile wall pocket or outside the arena, and every lane gap keeps a free span for X (seed $seed, X at $xTiles tiles, Overdrive $overdrive)") {
                val rig = newRig()
                ceilingCrash(rig, seed, xTiles, overdrive)
                val lanes = rig.spawnedOf<D1000DebrisWarning>().map { it.position.x }.sorted()
                (lanes.size >= if (overdrive) 3 else 6) shouldBe true
                var minX = Float.POSITIVE_INFINITY
                var maxX = Float.NEGATIVE_INFINITY
                rig.ticksUntil(4f) {
                    rig.spawnedOf<D1000CeilingDebris>().forEach { piece ->
                        if (rig.destroyed.contains(piece, true)) return@forEach
                        minX = min(minX, body(piece).getX())
                        maxX = max(maxX, body(piece).getMaxX())
                    }
                    val pieces = rig.spawnedOf<D1000CeilingDebris>()
                    pieces.isNotEmpty() && pieces.all { rig.destroyed.contains(it, true) }
                } shouldBe true
                val pieces = rig.spawnedOf<D1000CeilingDebris>()
                pieces.size shouldBe rig.spawnedOf<D1000DebrisWarning>().sumOf { it.pieces.size }
                (minX >= rig.arena.getX() + D1000Ceiling.CEILING_POCKET - TOLERANCE) shouldBe true
                (maxX <= rig.arena.getMaxX() - D1000Ceiling.CEILING_POCKET + TOLERANCE) shouldBe true
                val landed = pieces.map { body(it).getX() to body(it).getMaxX() }
                lanes.zipWithNext().forEach { (a, b) ->
                    val left = landed.filter { (l, r) -> (l + r) / 2f < (a + b) / 2f && r > a - RIG_PPM }.maxOfOrNull { it.second } ?: a
                    val right = landed.filter { (l, r) -> (l + r) / 2f >= (a + b) / 2f && l < b + RIG_PPM }.minOfOrNull { it.first } ?: b
                    withClue("lanes ${(a - RIG_ARENA_X) / RIG_PPM} and ${(b - RIG_ARENA_X) / RIG_PPM}") {
                        (right - left >= MegamanXValues.BODY_WIDTH * RIG_PPM + 2f * D1000Moves.NORMAL.margin) shouldBe true
                    }
                }
            }
            }
        }
    }

    describe("the trap guard") {
        it("forecasts a lane as the whole spread with the biggest chunk, in the crossing window and as a wall strip") {
            val rig = newRig()
            rig.placeX(MID_X_TILES, 0f)
            rig.startFight()
            val guard = rig.boss.guard
            guard.begin(rig.arena, rig.xBody, D1000Moves.NORMAL)
            val before = guard.escape.count
            val lane = RIG_ARENA_X + 8f * RIG_PPM
            val hangY = RIG_FLOOR + RIG_ARENA_HEIGHT - 0.75f * RIG_PPM
            guard.addDebris(lane, hangY, D1000Ceiling.CEILING_DEBRIS_SPREAD, 1f)
            guard.escape.count shouldBe before + 2
            val reach = D1000Ceiling.CEILING_DEBRIS_SPREAD + D1000CeilingDebris.MAX_RADIUS
            val column = guard.escape.danger(before)
            column.left shouldBe (lane - reach plusOrMinus TOLERANCE)
            column.right shouldBe (lane + reach plusOrMinus TOLERANCE)
            column.wallOnly shouldBe false
            val fast = D1000CeilingDebris.fallTime(hangY - D1000CeilingDebris.MAX_RADIUS - rig.xBody.getMaxY(), D1000CeilingDebris.MAX_START_SPEED)
            val slow = D1000CeilingDebris.fallTime(hangY - RIG_FLOOR, 0f) + D1000CeilingDebris.MAX_LAST_DELAY
            column.from shouldBe (1f + fast plusOrMinus TOLERANCE)
            column.to shouldBe (1f + slow plusOrMinus TOLERANCE)
            val strip = guard.escape.danger(before + 1)
            strip.wallOnly shouldBe true
            strip.left shouldBe (lane - reach plusOrMinus TOLERANCE)
            strip.from shouldBe (1f plusOrMinus TOLERANCE)
        }

        it("a lane that sits in the wall pocket is pushed out of it: the envelope reaches from the lane to the pocket edge") {
            val rig = newRig()
            rig.startFight()
            val guard = rig.boss.guard
            guard.begin(rig.arena, rig.xBody, D1000Moves.NORMAL)
            val before = guard.escape.count
            val lane = RIG_ARENA_X + 0.5f * RIG_PPM
            guard.addDebris(lane, RIG_FLOOR + RIG_ARENA_HEIGHT - 0.75f * RIG_PPM, 0f, 0f)
            val column = guard.escape.danger(before)
            column.left shouldBe (lane - D1000CeilingDebris.MAX_RADIUS plusOrMinus TOLERANCE)
            column.right shouldBe (RIG_ARENA_X + D1000Ceiling.CEILING_POCKET + D1000CeilingDebris.MAX_RADIUS plusOrMinus TOLERANCE)
        }

        it("reads the rolled pieces of a live warning and of a live piece: each one is held with its own start, drift and size") {
            val rig = newRig()
            ceilingCrash(rig, 6L)
            val guard = rig.boss.guard
            fun covered(x: Float, left: Float, right: Float, radius: Float) =
                (0 until guard.escape.count).map { guard.escape.danger(it) }.any {
                    it.wallOnly && it.left <= left - radius + TOLERANCE && it.right >= right + radius - TOLERANCE
                }
            guard.begin(rig.arena, rig.xBody, D1000Moves.NORMAL)
            rig.spawnedOf<D1000DebrisWarning>().forEach { warning ->
                warning.pieces.forEach { piece ->
                    val time = D1000CeilingDebris.fallTime(warning.hangPoint.y - RIG_FLOOR, -piece.velocity.y)
                    val landing = piece.startX + piece.velocity.x * time
                    withClue("piece ${piece.key} of lane ${(warning.position.x - RIG_ARENA_X) / RIG_PPM}") {
                        covered(piece.startX, min(piece.startX, landing), max(piece.startX, landing), piece.radius) shouldBe true
                    }
                }
            }
            rig.ticksUntil(1f) { rig.spawnedOf<D1000CeilingDebris>().isNotEmpty() } shouldBe true
            rig.ticks(0.2f)
            guard.begin(rig.arena, rig.xBody, D1000Moves.NORMAL)
            rig.spawnedOf<D1000CeilingDebris>().forEach { piece ->
                if (rig.destroyed.contains(piece, true)) return@forEach
                val b = body(piece)
                val center = b.getCenter(Vector2())
                val time = D1000CeilingDebris.fallTime(center.y - RIG_FLOOR, -b.physics.velocity.y)
                val landing = center.x + b.physics.velocity.x * time
                withClue("live piece ${piece.key}") {
                    covered(center.x, min(center.x, landing), max(center.x, landing), D1000CeilingDebris.radius(piece.key)) shouldBe true
                }
            }
            val keys = rig.spawnedOf<D1000CeilingDebris>().map { it.key }
            keys shouldContain rig.spawnedOf<D1000DebrisWarning>().first().chunkKey
        }
    }
})
