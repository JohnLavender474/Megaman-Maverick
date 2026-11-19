package com.megaman.maverick.game.entities.bosses.elecdevil

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.megaman.maverick.game.entities.bosses.ElecDevil
import com.megaman.maverick.game.entities.bosses.ElecDevilClaw
import com.megaman.maverick.game.entities.bosses.ElecDevilState
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_BODY_HEIGHT
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_BODY_WIDTH
import com.megaman.maverick.game.players.PlayerDamageNegotiator
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.abs

private const val POSITION_TOLERANCE = 0.5f
private const val DRAWN_ARC_HALF_THICKNESS = 8f
private const val SAFE_STEP = 0.25f
private const val TETHER = "tether"

private class ClawTick(
    val state: ElecDevilState,
    val phase: ClawPhase,
    val handActive: List<Boolean>,
    val claw: ElecDevilClaw?,
    val center: Vector2?,
    val spriteHidden: Boolean,
    val fixturesActive: List<Boolean>,
    val tetherHidden: Boolean,
    val tetherAlpha: Float,
    val tetherStart: Vector2?,
    val tetherEnd: Vector2?,
    val lines: List<GameLine>,
    val clawRect: GameRectangle?
)

private class ClawRun {
    lateinit var fight: ElecDevilRig
    val ticks = mutableListOf<ClawTick>()
    lateinit var socket: Vector2
    var floorY = 0f
    var bodyFrontX = 0f
    var stateAfterHand: ElecDevilState? = null
    var clawDestroyed = false

    fun run() {
        fight = ElecDevilRig()
        fight.startFight()
        fight.untilState(ElecDevilState.HAND)
        val active = fight.bodyOf(fight.activeBody())
        floorY = active.getY()
        bodyFrontX = active.getMaxX()
        socket = fight.handCenter()
        var lastClaw: ElecDevilClaw? = null
        while (fight.state == ElecDevilState.HAND && ticks.size < 2000) {
            record()?.let { lastClaw = it }
            fight.rig.tick()
            fight.rig.runEvents()
        }
        stateAfterHand = fight.state
        repeat(10) {
            record()
            fight.rig.tick()
        }
        clawDestroyed = lastClaw != null && fight.rig.destroyed.contains(lastClaw, true)
    }

    private fun record(): ElecDevilClaw? {
        val handActive = fight.handFixtures().map { it.isActive() }
        val claw = fight.liveClaw()
        if (claw == null) {
            ticks.add(
                ClawTick(
                    fight.state, fight.boss.getClawPhase(), handActive, null, null, true, emptyList(), true, 0f, null,
                    null, emptyList(), null
                )
            )
            return null
        }
        val sprites = claw.getComponent(SpritesComponent::class)!!
        sprites.preProcess(fight.rig.step)
        val body = fight.bodyOf(claw)
        val fixtures = body.fixtures[FixtureType.DAMAGER].toList() + body.fixtures[FixtureType.SHIELD].toList()
        val startCap = sprites.sprites["${TETHER}_cap_0"]
        val endCap = sprites.sprites["${TETHER}_cap_1"]
        ticks.add(
            ClawTick(
                state = fight.state,
                phase = fight.boss.getClawPhase(),
                handActive = handActive,
                claw = claw,
                center = body.getCenter(Vector2()),
                spriteHidden = sprites.sprites[ElecDevilClaw.TAG].hidden,
                fixturesActive = fixtures.map { it.isActive() },
                tetherHidden = startCap.hidden,
                tetherAlpha = startCap.color.a,
                tetherStart = Vector2(startCap.x + startCap.width / 2f, startCap.y + startCap.height / 2f),
                tetherEnd = Vector2(endCap.x + endCap.width / 2f, endCap.y + endCap.height / 2f),
                lines = body.fixtures[FixtureType.DAMAGER].map { it.getShape() }.filterIsInstance<GameLine>()
                    .map { it.copy() },
                clawRect = body.fixtures[FixtureType.DAMAGER].map { it.getShape() }
                    .filterIsInstance<GameRectangle>().single().copy()
            )
        )
        return claw
    }
}

private fun distanceToLine(point: Vector2, from: Vector2, to: Vector2): Float {
    val direction = Vector2(to).sub(from).nor()
    return abs(direction.crs(Vector2(point).sub(from)))
}

class ElecDevilClawTest : DescribeSpec({

    var run = ClawRun()

    beforeSpec { run.run() }

    afterSpec {
        run.fight.dispose()
        run = ClawRun()
    }

    val tuning = ElecDevilTuning.NORMAL

    fun inHand(phase: ClawPhase) = run.ticks.filter { it.state == ElecDevilState.HAND && it.phase == phase }

    fun seconds(ticks: List<ClawTick>) = ticks.size * run.fight.rig.step

    fun tolerance() = 2f * run.fight.rig.step

    fun home() = inHand(ClawPhase.WINDUP).first { it.claw != null }.center!!

    fun holdTicks(): List<ClawTick> {
        val out = inHand(ClawPhase.OUT).filter { it.claw != null }
        val far = out.maxBy { it.center!!.x }.center!!
        return out.filter { it.center!!.dst(far) < POSITION_TOLERANCE }
    }

    fun hold() = holdTicks().first().center!!

    describe("the HAND state") {
        it("runs the delay, the windup, the claw out and back and the recoil, then STAND") {
            run.ticks.first().phase shouldBe ClawPhase.DELAY
            seconds(inHand(ClawPhase.DELAY)) shouldBe (tuning.clawDelayDur plusOrMinus tolerance())
            seconds(inHand(ClawPhase.WINDUP)) shouldBe (tuning.clawWindupDur plusOrMinus tolerance())
            seconds(inHand(ClawPhase.RECOIL)) shouldBe
                (ElecDevil.CLAW_RECOIL_DUR plusOrMinus tolerance())
            val phases = run.ticks.filter { it.state == ElecDevilState.HAND }.map { it.phase }
            phases.zipWithNext().filter { (a, b) -> a != b } shouldBe listOf(
                ClawPhase.DELAY to ClawPhase.WINDUP, ClawPhase.WINDUP to ClawPhase.OUT, ClawPhase.OUT to ClawPhase.RECOIL
            )
            run.stateAfterHand shouldBe ElecDevilState.STAND
            run.clawDestroyed shouldBe true
            withClue("the RECOIL starts when the claw is back") {
                inHand(ClawPhase.RECOIL).all { it.claw == null } shouldBe true
            }
        }

        it("spawns the claw at the windup start in its aim state: no sprite, no damage, a flickering guide line") {
            inHand(ClawPhase.DELAY).all { it.claw == null } shouldBe true
            val windup = inHand(ClawPhase.WINDUP)
            windup.drop(1).all { it.claw != null } shouldBe true
            val aim = windup.filter { it.claw != null }
            aim.forEach { tick ->
                tick.spriteHidden shouldBe true
                tick.fixturesActive.any { it } shouldBe false
            }
            val alphas = aim.map { it.tetherAlpha }.distinct()
            withClue("guide alphas $alphas") {
                alphas.size shouldBe 1
                alphas.single() shouldBeLessThan 1f
                alphas.single() shouldBeGreaterThan 0f
            }
            val runs = mutableListOf<Int>()
            var last: Boolean? = null
            aim.forEach { tick ->
                if (tick.tetherHidden == last) runs[runs.size - 1]++ else runs.add(1)
                last = tick.tetherHidden
            }
            withClue("runs of shown and hidden ticks: $runs") {
                runs.size shouldBeGreaterThan 4
                val inner = runs.drop(1).dropLast(1)
                (inner.max() - inner.min()) shouldBeLessThanOrEqual 2
            }
            val back = Vector2(home()).sub(run.socket)
            val end = Vector2(hold()).sub(back)
            aim.filter { !it.tetherHidden }.forEach { tick ->
                tick.tetherStart!!.dst(run.socket) shouldBeLessThan POSITION_TOLERANCE
                tick.tetherEnd!!.dst(end) shouldBeLessThan POSITION_TOLERANCE
            }
        }

        it("flies the claw straight from the socket to the far wall at clawSpeed, holds, and returns the same way") {
            val out = inHand(ClawPhase.OUT).filter { it.claw != null }
            out.forEach { tick ->
                tick.spriteHidden shouldBe false
                tick.fixturesActive.all { it } shouldBe true
                tick.tetherHidden shouldBe false
                tick.tetherAlpha shouldBe 1f
            }
            val home = home()
            val hold = hold()
            val clawBounds = holdTicks().first().clawRect!!
            withClue("the claw at the hold: $clawBounds") {
                clawBounds.getMaxX() shouldBe (run.fight.room.getMaxX() plusOrMinus POSITION_TOLERANCE)
                clawBounds.getY() shouldBe (run.floorY plusOrMinus POSITION_TOLERANCE)
            }
            out.forEach { tick ->
                withClue("the claw at ${tick.center} on the line $home to $hold") {
                    distanceToLine(tick.center!!, home, hold) shouldBeLessThan POSITION_TOLERANCE
                }
            }
            val step = tuning.clawSpeed * T * run.fight.rig.step
            val moves = out.map { it.center!! }.zipWithNext().map { (a, b) -> a.dst(b) }
            val outward = moves.takeWhile { it > POSITION_TOLERANCE }
            outward.dropLast(1).forEach { it shouldBe (step plusOrMinus POSITION_TOLERANCE) }
            seconds(holdTicks()) shouldBe (tuning.clawHoldDur plusOrMinus tolerance())
            val inward = moves.dropWhile { it > POSITION_TOLERANCE }.dropWhile { it <= POSITION_TOLERANCE }
            inward.dropLast(1).forEach { it shouldBe (step plusOrMinus POSITION_TOLERANCE) }
            out.last().center!!.dst(home) shouldBeLessThan step + POSITION_TOLERANCE
        }
    }

    describe("the claw and the tether") {
        it("make the tether hitbox 2 lines either side of the line from the socket to the claw back") {
            val back = Vector2(home()).sub(run.socket)
            inHand(ClawPhase.OUT).filter { it.claw != null }.forEach { tick ->
                val end = Vector2(tick.center!!).sub(back)
                val normal = Vector2(end).sub(run.socket).nor().rotate90(1)
                tick.lines.size shouldBe 2
                val offsets = tick.lines.map { line ->
                    val a = Vector2()
                    val b = Vector2()
                    line.calculateWorldPoints(a, b)
                    val startOffset = Vector2(a).sub(run.socket)
                    val endOffset = Vector2(b).sub(end)
                    startOffset.dot(normal) shouldBe (endOffset.dot(normal) plusOrMinus POSITION_TOLERANCE)
                    abs(startOffset.crs(normal)) shouldBeLessThan POSITION_TOLERANCE
                    startOffset.dot(normal)
                }
                withClue("offsets $offsets") {
                    offsets[0] shouldBe (-offsets[1] plusOrMinus POSITION_TOLERANCE)
                    abs(offsets[0]) shouldBeGreaterThan 0f
                    abs(offsets[0]) shouldBeLessThanOrEqual DRAWN_ARC_HALF_THICKNESS
                }
            }
        }

        it("leave a safe place only under the high end of the slope, near the boss") {
            val hold = holdTicks().first()
            val damagers: List<IGameShape2D> = hold.lines + hold.clawRect!!
            fun safeReach(height: Float): Float {
                val safe = mutableListOf<Boolean>()
                var dx = 0f
                while (run.bodyFrontX + (dx + MEGAMAN_BODY_WIDTH) * T <= run.fight.room.getMaxX()) {
                    val player = GameRectangle(
                        run.bodyFrontX + dx * T, run.floorY, MEGAMAN_BODY_WIDTH * T, height * T
                    )
                    safe.add(damagers.none { it.overlaps(player) || player.overlaps(it) })
                    dx += SAFE_STEP
                }
                val reach = safe.takeWhile { it }.size
                withClue("safe positions in steps of $SAFE_STEP tiles from the boss front, height $height: $safe") {
                    reach shouldBeGreaterThan 0
                    safe.drop(reach).any { it } shouldBe false
                }
                return reach * SAFE_STEP
            }
            val standing = safeReach(MEGAMAN_BODY_HEIGHT)
            val crouching = safeReach(MEGAMAN_BODY_WIDTH)
            withClue("safe up to $standing tiles standing, $crouching tiles crouching") {
                crouching shouldBeGreaterThan standing
            }
        }

        it("hurt the player by 3 and block shots while out, and keep the hand fixtures to the windup and recoil") {
            val claw = run.ticks.first { it.claw != null }.claw!!
            PlayerDamageNegotiator(run.fight.rig.player).get(claw) shouldBe 3
            inHand(ClawPhase.WINDUP).forEach { it.handActive.all { active -> active } shouldBe true }
            inHand(ClawPhase.OUT).forEach { it.handActive.any { active -> active } shouldBe false }
            inHand(ClawPhase.RECOIL).forEach { it.handActive.all { active -> active } shouldBe true }
            run.ticks.filter { it.state != ElecDevilState.HAND }.forEach {
                it.handActive.any { active -> active } shouldBe false
            }
        }
    }

    describe("the end of the claw") {
        it("removes the claw when the boss is defeated") {
            val fight = ElecDevilRig()
            try {
                fight.startFight()
                fight.untilState(ElecDevilState.HAND)
                fight.rig.ticksUntil(5f) { fight.boss.getClawPhase() == ClawPhase.OUT } shouldBe true
                val claw = fight.liveClaw()
                claw shouldNotBe null
                fight.toPhase2()
                repeat(15) { fight.boss.takeDamageFrom(fight.rig.bullet()) }
                fight.rig.checkPoints(fight.boss)
                fight.boss.defeated shouldBe true
                fight.liveClaw() shouldBe claw
                fight.rig.tick()
                fight.rig.destroyed.contains(claw, true) shouldBe true
            } finally {
                fight.dispose()
            }
        }

        it("removes the claw when the boss is destroyed") {
            val fight = ElecDevilRig()
            try {
                fight.startFight()
                fight.untilState(ElecDevilState.HAND)
                fight.rig.ticksUntil(5f) { fight.liveClaw() != null } shouldBe true
                val claw = fight.liveClaw()!!
                fight.boss.destroy()
                fight.boss.onDestroy()
                fight.rig.destroyed.contains(claw, true) shouldBe true
            } finally {
                fight.dispose()
            }
        }
    }
})
