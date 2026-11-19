package com.megaman.maverick.game.entities.bosses.elecdevil

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameLine
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.bosses.ElecDevilBody
import com.megaman.maverick.game.entities.bosses.ElecDevilBodyPiece
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants
import com.megaman.maverick.game.entities.bosses.ElecDevilState
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_BODY_WIDTH
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PHASE_1_LAUNCHES = 3
private const val LAUNCHES = 9
private const val RANDOM_TARGET_EVERY = 3
private const val CROUCH_HEIGHT = MEGAMAN_BODY_WIDTH
private const val PLAYER_X_MIN = 5.5f
private const val PLAYER_X_MAX = 9.5f
private const val PLAYER_X_STEP = 0.25f

private class FlightSample(val time: Float, val x: Float)

private class LaunchedPiece(
    val launch: Int,
    val phase: Int,
    val startRow: Int,
    val targetRow: Int,
    val startX: Float,
    val targetX: Float,
    val lead: ElecDevilBodyPiece?
) {
    val samples = mutableListOf<FlightSample>()
}

private class LiveWireRun {
    lateinit var fight: ElecDevilRig
    val pieces = LinkedHashMap<ElecDevilBodyPiece, LaunchedPiece>()
    val crouchLows = mutableListOf<Pair<String, Float>>()
    var floorY = 0f

    fun rowOf(y: Float) = ((y - floorY) / (ElecDevilConstants.PIECE_HEIGHT * T) - 0.5f).roundToInt()

    fun bandOf(piece: LaunchedPiece) = LiveWireLinks.bandOf(piece.startRow)

    fun inCrouchLink(piece: ElecDevilBodyPiece): Boolean {
        val launched = pieces[piece] ?: return false
        if (launched.lead != null) return bandOf(launched) == LiveWireBand.CROUCH_UNDER
        return pieces.values.any { it.lead == piece && bandOf(it) == LiveWireBand.CROUCH_UNDER }
    }

    fun run() {
        fight = ElecDevilRig()
        fight.startFight()
        floorY = fight.bodyOf(fight.rig.spawned.filterIsInstance<ElecDevilBody>().first()).getY()
        var launch = 1
        var appears = 0
        var lastState = fight.state
        fight.rig.ticksUntil(600f, each = { fight.rig.runEvents() }) {
            if (fight.state != lastState) {
                if (fight.state == ElecDevilState.LAUNCH) launch++
                if (fight.state == ElecDevilState.APPEAR) appears++
                if (fight.state == ElecDevilState.STAND && appears == PHASE_1_LAUNCHES && fight.boss.healthPhase == 1)
                    fight.toPhase2()
                lastState = fight.state
            }
            record(launch)
            appears >= LAUNCHES
        } shouldBe true
    }

    private fun record(launch: Int) {
        val rig = fight.rig
        rig.spawned.filterIsInstance<ElecDevilBodyPiece>().forEach { piece ->
            if (rig.destroyed.contains(piece, true)) return@forEach
            val launched = pieces.getOrPut(piece) {
                val props = rig.spawnProps[piece]
                val start = props.get(ConstKeys.START, Vector2::class)!!
                val target = props.get(ConstKeys.TARGET, Vector2::class)!!
                LaunchedPiece(launch, fight.boss.healthPhase, rowOf(start.y), rowOf(target.y), start.x, target.x, piece.linkedPiece)
            }
            val body = fight.bodyOf(piece)
            launched.samples.add(FlightSample(rig.time, body.getCenter(Vector2()).x))
            if (!inCrouchLink(piece)) return@forEach
            body.fixtures[FixtureType.DAMAGER].forEach { fixture ->
                if (!fixture.isActive()) return@forEach
                when (val shape = fixture.getShape()) {
                    is GameCircle -> crouchLows.add("circle of $piece" to shape.getY() - shape.getRadius())
                    is GameLine -> crouchLows.add("arc of $piece" to shape.getY())
                }
            }
        }
    }
}

class ElecDevilLiveWireFightTest : DescribeSpec({

    var run = LiveWireRun()

    beforeSpec { run.run() }

    afterSpec {
        run.fight.dispose()
        run = LiveWireRun()
    }

    fun links() = run.pieces.entries.filter { it.value.lead != null }

    describe("the Live Wire links of $LAUNCHES launches") {
        it("join 2 pieces whose start rows and target rows are all in one band") {
            val links = links()
            links.size shouldBeGreaterThan 0
            links.forEach { (piece, trail) ->
                val lead = run.pieces[trail.lead!!]!!
                val rows = listOf(lead.startRow, trail.startRow, lead.targetRow, trail.targetRow)
                withClue("$piece in launch ${trail.launch}: rows $rows") {
                    rows.map { LiveWireLinks.bandOf(it) }.distinct().size shouldBe 1
                }
            }
            (1..LAUNCHES).forEach { launch ->
                val inLaunch = links.filter { it.value.launch == launch }
                val row = ElecDevilTuning.of(hard = false, phase = run.pieces.values.first { it.launch == launch }.phase)
                inLaunch.size shouldBeLessThanOrEqual row.liveWireLinks
            }
            withClue("a linked pair in a random-target launch") {
                links.count { it.value.launch % RANDOM_TARGET_EVERY == 0 } shouldBeGreaterThan 0
            }
            withClue("a phase-2 launch") { run.pieces.values.count { it.phase == 2 } shouldBeGreaterThan 0 }
        }

        it("keep the target rows of each column a permutation of its rows in the random-target launches") {
            (RANDOM_TARGET_EVERY..LAUNCHES step RANDOM_TARGET_EVERY).forEach { launch ->
                val launched = run.pieces.values.filter { it.launch == launch }
                launched.size shouldBe ElecDevilConstants.PIECE_COUNT
                withClue("launch $launch moves some pieces to other rows") {
                    launched.count { it.startRow != it.targetRow } shouldBeGreaterThan 0
                }
                launched.groupBy { (it.targetX / T * 100f).roundToInt() }.forEach { (column, inColumn) ->
                    withClue("launch $launch, column at x ${column / 100f}") {
                        inColumn.map { it.targetRow }.sorted() shouldBe inColumn.map { it.startRow }.sorted()
                        inColumn.map { it.targetRow }.distinct().size shouldBe inColumn.size
                    }
                }
            }
        }

        it("keep each CROUCH_UNDER piece circle and arc above a crouching Megaman at all times") {
            val crouchTop = run.floorY + CROUCH_HEIGHT * T
            run.crouchLows.size shouldBeGreaterThan 0
            run.crouchLows.forEach { (shape, low) ->
                withClue("$shape lowest at ${(low - run.floorY) / T} tiles") { (low > crouchTop) shouldBe true }
            }
        }

        it("let a JUMP_OVER pair pass a player in at most JUMP_CLEAR_TIME - JUMP_TIMING_SLACK") {
            val limit = LiveWireLinks.JUMP_CLEAR_TIME - LiveWireLinks.JUMP_TIMING_SLACK
            val reach = (LiveWireLinks.PIECE_RADIUS + MEGAMAN_BODY_WIDTH / 2f) * T
            val step = run.fight.rig.step
            var firstLaunchPairs = 0
            var crossColumnPairs = 0
            var randomLaunchPairs = 0
            var worst = 0f
            links().filter { run.bandOf(it.value) == LiveWireBand.JUMP_OVER }.forEach { (piece, trail) ->
                val lead = run.pieces[trail.lead!!]!!
                if (trail.launch == 1) firstLaunchPairs++
                if (trail.launch % RANDOM_TARGET_EVERY == 0) randomLaunchPairs++
                if (abs(lead.targetX - trail.targetX) > 1f) crossColumnPairs++
                var playerX = PLAYER_X_MIN
                while (playerX <= PLAYER_X_MAX) {
                    val x = playerX * T
                    val reached = lead.samples.firstOrNull { abs(it.x - x) < reach }
                    val left = trail.samples.lastOrNull { abs(it.x - x) < reach }
                    if (reached != null && left != null) {
                        val pass = left.time - reached.time
                        worst = maxOf(worst, pass)
                        withClue("$piece in launch ${trail.launch}, player at $playerX tiles: pass $pass s") {
                            pass shouldBeLessThanOrEqual limit + step
                        }
                    }
                    playerX += PLAYER_X_STEP
                }
            }
            withClue("pairs: first launch $firstLaunchPairs, cross-column $crossColumnPairs, random $randomLaunchPairs; worst $worst s") {
                firstLaunchPairs shouldBeGreaterThan 0
                crossColumnPairs shouldBeGreaterThan 0
            }
        }
    }
})
