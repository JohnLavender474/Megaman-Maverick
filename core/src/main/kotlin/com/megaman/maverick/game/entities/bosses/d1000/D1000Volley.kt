package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.body
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.decorations.D1000BombMarker
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import com.megaman.maverick.game.entities.bosses.D1000
import com.megaman.maverick.game.entities.bosses.D1000Core
import com.megaman.maverick.game.entities.bosses.D1000State

enum class D1000GuidePhase { SIGNAL, FLIGHT, LOCK }

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
internal class D1000Volley(private val boss: D1000) : D1000Move {

    companion object {
        const val TAG = "D1000Volley"
    }

    private enum class Step { START, FENCE_SPOT, RISE, GUIDE, END }

    var pattern = D1000BombPattern.AIMED
    private var step = Step.START
    private var time = 0f
    var phase = D1000GuidePhase.SIGNAL
        private set
    var phaseTime = 0f
        private set
    var guideX = 0f
        private set
    var guideY = 0f
        private set
    var wallX = 0f
        private set
    val horizontal: Boolean
        get() = pattern == D1000BombPattern.WALL
    private var railX = 0f
    private var low = 0f
    private var high = 0f
    var eyeGuides = false
        private set
    private var mark = 0
    private var target = 0f
    private var endDur = 0f
    private var marker: D1000BombMarker? = null
    private val roles = Array<D1000Role>()
    private val forecastRoles = Array<D1000Role>()
    private val fence = FloatArray(D1000Orbs.MAX_LANES)
    private val forecastFence = FloatArray(D1000Orbs.MAX_LANES)
    private var fenceCount = 0
    private var forecastFenceCount = 0
    private var sweepNext = 0
    private val thrown = FloatArray(D1000Orbs.MAX_LANES * 2)
    private var thrownCount = 0

    override val holdsEyeShots: Boolean
        get() = step == Step.GUIDE || step == Step.END

    val guiding: Boolean
        get() = step == Step.GUIDE

    val clock: Float
        get() = time

    val role: D1000Role?
        get() = if (step == Step.GUIDE) roles[mark] else null

    fun chargeProgress(): Float? =
        if (step == Step.GUIDE && phase == D1000GuidePhase.LOCK) min(phaseTime / D1000Orbs.MARK_DUR, 1f) else null

    fun reset() {
        goTo(Step.START)
        pattern = D1000BombPattern.AIMED
        eyeGuides = false
        marker = null
        roles.clear()
    }

    override fun begin() = goTo(Step.START)

    private fun goTo(next: Step) {
        step = next
        time = 0f
    }

    override fun update(delta: Float) {
        time += delta
        when (step) {
            Step.START -> if (pattern == D1000BombPattern.FENCE) {
                boss.setAnim("walk")
                goTo(Step.FENCE_SPOT)
            } else if (horizontal) {
                boss.setAnim("walk")
                goTo(Step.RISE)
            } else {
                boss.setAnim("throw")
                val script = boss.services.random.nextInt(0, D1000Orbs.scriptCount(pattern) - 1)
                D1000Orbs.roles(pattern, script, D1000Orbs.count(pattern, boss.tuning), roles)
                beginGuide()
            }

            Step.FENCE_SPOT -> if (moveToFenceSpot(delta)) {
                boss.setAnim("throw")
                fenceCount = D1000Orbs.fenceLanes(boss.arena, boss.playerX(), fence)
                D1000Orbs.roles(pattern, 0, fenceCount, roles)
                beginGuide()
            }

            Step.RISE -> {
                boss.origin.y = approach(boss.origin.y, boss.highestHover, D1000.RISE_SPEED, delta)
                if (boss.origin.y == boss.highestHover) beginWall()
            }

            Step.GUIDE -> {
                val lockX = if (horizontal) wallX else target
                if (phase == D1000GuidePhase.LOCK) boss.facing = if (lockX < boss.origin.x) Facing.LEFT else Facing.RIGHT
                else boss.facePlayer()
                updateGuide(delta)
            }

            Step.END -> {
                if (time >= endDur - D1000Lamp.LEAD_DUR) boss.tellFollowUp()
                if (time >= endDur) boss.finishAction(D1000State.WALK)
            }
        }
    }

    private fun beginWall() {
        boss.setAnim("throw")
        val boxes = boss.bodyBoxes()
        low = boss.floor + D1000Orbs.WALL_FLOOR_CLEAR
        high = (if (boxes.isEmpty) boss.origin.y else boxes.minOf { it.getY() }) - D1000Orbs.WALL_BODY_CLEAR
        if (high < low) {
            GameLogger.debug(TAG, "beginWall(): no room between the floor band and the body, end the volley")
            goTo(Step.END)
            endDur = 0f
            return
        }
        wallX = D1000Orbs.wallX(boss.arena, boss.playerX())
        railX = wallX + if (wallX > boss.origin.x) -D1000Orbs.WALL_EYE_GAP else D1000Orbs.WALL_EYE_GAP
        val script = boss.services.random.nextInt(0, D1000Orbs.scriptCount(pattern) - 1)
        D1000Orbs.roles(pattern, script, D1000Orbs.count(pattern, boss.tuning), roles)
        beginGuide()
    }

    private fun beginGuide() {
        if (herds()) roles[roles.size - 1] = D1000Role.HERD
        val eye = boss.core
        eyeGuides = eye?.guide() == true
        val eyeCenter = if (eyeGuides) eye!!.body.getCenter(Vector2()) else null
        guideX = if (horizontal) eyeCenter?.x ?: railX
        else D1000Orbs.clampLane(boss.arena, eyeCenter?.x ?: boss.origin.x)
        guideY = eyeCenter?.y ?: boss.origin.y
        mark = 0
        sweepNext = 0
        thrownCount = 0
        goTo(Step.GUIDE)
        startMark()
    }

    private fun herds() = !horizontal && boss.tuning.tier == D1000Tier.OVERDRIVE && boss.followUpQueued && roles.size > 1

    private fun startMark() {
        phase = D1000GuidePhase.SIGNAL
        phaseTime = 0f
        target = pickLane(roles[mark])
    }

    private fun updateGuide(delta: Float) {
        phaseTime += delta
        when (phase) {
            D1000GuidePhase.SIGNAL -> if (phaseTime >= boss.tuning.guideSignalDur) {
                phase = D1000GuidePhase.FLIGHT
                phaseTime = 0f
            }

            D1000GuidePhase.FLIGHT -> if (horizontal) {
                if (roles[mark] == D1000Role.HUNT) huntHeight().let {
                    val ahead = target - guideY
                    if (abs(it - target) >= D1000Core.MIN_MOVE && (ahead == 0f || ahead * (it - guideY) > 0f)) target = it
                }
                guideX = approach(guideX, railX, D1000Orbs.GUIDE_SPEED, delta)
                guideY = approach(guideY, target, D1000Orbs.GUIDE_SPEED, delta)
                if ((guideY == target && guideX == railX) || phaseTime >= D1000Orbs.GUIDE_FLIGHT_MAX) lock()
            } else {
                if (roles[mark] == D1000Role.HUNT) huntLane().let {
                    val ahead = target - guideX
                    if (abs(it - target) >= D1000Core.MIN_MOVE && (ahead == 0f || ahead * (it - guideX) > 0f)) target = it
                }
                guideX = approach(guideX, target, D1000Orbs.GUIDE_SPEED, delta)
                if (guideX == target || phaseTime >= D1000Orbs.GUIDE_FLIGHT_MAX) lock()
            }

            D1000GuidePhase.LOCK -> if (phaseTime >= D1000Orbs.MARK_DUR) release()
        }
    }

    private fun lock() {
        phase = D1000GuidePhase.LOCK
        phaseTime = 0f
        if (horizontal) {
            target = guideY
            val spawned = MegaEntityFactory.fetch(D1000BombMarker::class)!!
            val rotation = if (wallX < boss.origin.x) -90f else 90f
            spawned.spawn(props(ConstKeys.POSITION pairTo Vector2(wallX, target), ConstKeys.ROTATION pairTo rotation))
            marker = spawned
            return
        }
        target = guideX
        if (roles[mark] == D1000Role.HERD) boss.tellFollowUp()
        if (!boss.laneInArena(target)) return
        val spawned = MegaEntityFactory.fetch(D1000BombMarker::class)!!
        spawned.spawn(props(ConstKeys.POSITION pairTo Vector2(target, boss.floor)))
        marker = spawned
    }

    private fun release() {
        val held = marker
        marker = null
        if (horizontal) boss.throwBomb(wallX, held, target)
        else if (boss.laneInArena(target) && releaseAllowed(target, 0f)) boss.throwBomb(target, held)
        else {
            GameLogger.debug(TAG, "release(): hold orb $mark at $target, it leaves no escape")
            held?.let { if (it.existenceState.liveAfterUpdate) it.destroy() }
        }
        if (thrownCount < thrown.size) thrown[thrownCount++] = target
        mark++
        if (mark < roles.size) {
            startMark()
            return
        }
        val flight = if (horizontal) D1000Bomb.wallFlightTime(boss.chargeX(), wallX) else orbFlight()
        endDur = max(D1000Orbs.PATTERN_END_AFTER, flight + D1000Pillar.START_DUR + D1000Pillar.ACTIVE_DUR)
        goTo(Step.END)
    }

    private fun playerSpeedX() = boss.targetPlayer.body.physics.velocity.x

    private fun predictedX() = D1000Orbs.aim(boss.arena, boss.playerX(), playerSpeedX(), boss.tuning)

    private fun huntLane(): Float {
        val aim = predictedX()
        if (pattern != D1000BombPattern.FENCE) return aim
        return D1000Orbs.nearestLane(fence, fenceCount, aim)
    }

    private fun playerSpeedY() = boss.targetPlayer.body.physics.velocity.y

    private fun huntHeight() =
        D1000Orbs.aimHeight(boss.targetPlayer.body.getCenter().y, playerSpeedY(), boss.tuning, low, high)

    private fun pickLane(role: D1000Role): Float {
        if (horizontal) return when (role) {
            D1000Role.CUT -> D1000Orbs.cutHeight(huntHeight(), playerSpeedY(), low, high)
            else -> huntHeight()
        }
        val playerX = predictedX()
        val arena = boss.arena
        return when (role) {
            D1000Role.HUNT -> huntLane()
            D1000Role.SWEEP -> fence[sweepNext++]
            D1000Role.CUT -> {
                val side = D1000Orbs.cutSide(arena, playerX, playerSpeedX())
                firstAllowed(D1000Orbs.sideLane(arena, playerX, side, bodyLow(), bodyHigh()), D1000Orbs.sideLane(arena, playerX, -side, bodyLow(), bodyHigh()))
            }

            D1000Role.HERD -> {
                val away = if (playerX >= boss.origin.x) 1 else -1
                firstAllowed(D1000Orbs.sideLane(arena, playerX, away, bodyLow(), bodyHigh()), D1000Orbs.sideLane(arena, playerX, -away, bodyLow(), bodyHigh()))
            }

            D1000Role.FEINT -> {
                repeat(D1000Orbs.FEINT_TRIES) {
                    val lane = D1000Orbs.feintLane(arena, playerX, thrown, thrownCount, bodyLow(), bodyHigh(), boss.services.random)
                    if (lane.isNaN()) return huntLane()
                    if (releaseAllowed(lane, delayTo(lane))) return lane
                }
                huntLane()
            }
        }
    }

    private fun bodyLow() = boss.origin.x - D1000.HALF_WIDTH - D1000Orbs.BODY_LANE_MARGIN

    private fun bodyHigh() = boss.origin.x + D1000.HALF_WIDTH + D1000Orbs.BODY_LANE_MARGIN

    private fun firstAllowed(first: Float, second: Float) =
        if (releaseAllowed(first, delayTo(first)) || !releaseAllowed(second, delayTo(second))) first else second

    private fun delayTo(lane: Float) = boss.tuning.guideSignalDur + flightDur(guideX, lane) + D1000Orbs.MARK_DUR

    private fun flightDur(from: Float, to: Float) = min(abs(to - from) / D1000Orbs.GUIDE_SPEED, D1000Orbs.GUIDE_FLIGHT_MAX)

    private fun orbFlight() = D1000Bomb.flightTime(boss.origin.y + D1000.BOMB_UP, boss.floor)

    private fun releaseAllowed(target: Float, delay: Float): Boolean {
        val guard = boss.guard
        boss.beginGuard()
        val landIn = delay + orbFlight() + D1000Pillar.START_DUR
        if (!boss.liveEscape()) return abs(target - boss.playerX()) >
            guard.moves.speed * landIn + D1000Pillar.WIDTH / 2f + D1000XMoves.HALF_WIDTH +
            guard.moves.margin
        val mark = guard.escape.count
        guard.addFootprint(boss.bodyBoxes(), 0f, 0f, D1000Escape.HORIZON, true)
        guard.addPillar(target, landIn, landIn + D1000Pillar.ACTIVE_DUR)
        val ok = guard.escapes()
        guard.escape.rollBack(mark)
        return ok
    }

    private fun moveToFenceSpot(delta: Float): Boolean {
        val origin = boss.origin
        origin.x = approach(origin.x, boss.middleX, D1000.RISE_SPEED, delta)
        origin.y = approach(origin.y, boss.highestHover, D1000.RISE_SPEED, delta)
        return origin.x == boss.middleX && origin.y == boss.highestHover
    }

    fun dropChargeMark() {
        marker?.let { if (it.existenceState.liveAfterUpdate) it.destroy() }
        marker = null
        if (step == Step.GUIDE) goTo(Step.END)
    }

    override fun forecastNow() {
        if (step != Step.GUIDE || horizontal) return
        val flight = orbFlight()
        var t: Float
        var x = guideX
        var first = mark
        if (phase == D1000GuidePhase.LOCK) {
            t = D1000Orbs.MARK_DUR - phaseTime
            addPillar(target, t + flight)
            first++
        } else {
            val signalLeft = if (phase == D1000GuidePhase.SIGNAL) boss.tuning.guideSignalDur - phaseTime else 0f
            val lane = if (roles[mark] == D1000Role.HUNT) huntLane() else target
            t = signalLeft + flightDur(x, lane) + D1000Orbs.MARK_DUR
            addPillar(lane, t + flight)
            x = lane
            first++
        }
        var sweep = sweepNext
        for (i in first until roles.size) {
            val lane = estimate(roles[i], fence, if (roles[i] == D1000Role.SWEEP) sweep++ else -1)
            t += boss.tuning.guideSignalDur + flightDur(x, lane) + D1000Orbs.MARK_DUR
            addPillar(lane, t + flight)
            x = lane
        }
    }

    override fun forecast(startIn: Float) = forecast(pattern, startIn)

    // TODO: the plan forecast assumes the first role script and the nearest feint lane; the run picks a script and feint lanes at random, and only the check at each release covers them (see the 081f NOTES, round 15)
    fun forecast(pattern: D1000BombPattern, startIn: Float) {
        val guard = boss.guard
        val origin = boss.origin
        val boxes = boss.bodyBoxes()
        if (pattern == D1000BombPattern.WALL) {
            guard.addFootprint(boxes, 0f, 0f, D1000Escape.HORIZON, true)
            return
        }
        var move = 0f
        var throwY = origin.y + D1000.BOMB_UP
        if (pattern == D1000BombPattern.FENCE) {
            move = max(abs(boss.middleX - origin.x), abs(boss.highestHover - origin.y)) / D1000.RISE_SPEED
            throwY = boss.highestHover + D1000.BOMB_UP
            guard.addFootprint(boxes, 0f, 0f, startIn + move, true)
            forecastFenceCount = D1000Orbs.fenceLanes(boss.arena, boss.playerX(), forecastFence)
            D1000Orbs.roles(pattern, 0, forecastFenceCount, forecastRoles)
        } else {
            guard.addFootprint(boxes, 0f, 0f, D1000Escape.HORIZON, true)
            D1000Orbs.roles(pattern, 0, D1000Orbs.count(pattern, boss.tuning), forecastRoles)
        }
        val flight = D1000Bomb.flightTime(throwY, boss.floor)
        var x = D1000Orbs.clampLane(boss.arena, boss.core?.body?.getCenter()?.x ?: origin.x)
        var t = startIn + move
        var sweep = 0
        for (i in 0 until forecastRoles.size) {
            val lane = estimate(forecastRoles[i], forecastFence, if (forecastRoles[i] == D1000Role.SWEEP) sweep++ else -1)
            t += boss.tuning.guideSignalDur + flightDur(x, lane) + D1000Orbs.MARK_DUR
            addPillar(lane, t + flight)
            x = lane
        }
    }

    private fun estimate(role: D1000Role, lanes: FloatArray, sweep: Int): Float {
        val arena = boss.arena
        val playerX = predictedX()
        return when (role) {
            D1000Role.HUNT -> if (pattern == D1000BombPattern.FENCE) D1000Orbs.nearestLane(lanes, laneCount(lanes), playerX)
            else playerX

            D1000Role.SWEEP -> lanes[sweep]
            D1000Role.CUT, D1000Role.HERD ->
                D1000Orbs.sideLane(arena, playerX, D1000Orbs.cutSide(arena, playerX, playerSpeedX()), bodyLow(), bodyHigh())

            D1000Role.FEINT -> D1000Orbs.nearLane(arena, playerX, bodyLow(), bodyHigh())
        }
    }

    private fun laneCount(lanes: FloatArray) = if (lanes === fence) fenceCount else forecastFenceCount

    private fun addPillar(lane: Float, landsIn: Float) {
        if (!boss.laneInArena(lane)) return
        val start = landsIn + D1000Pillar.START_DUR
        boss.guard.addPillar(lane, start, start + D1000Pillar.ACTIVE_DUR)
    }

    override fun allowed(startIn: Float): Boolean {
        forecast(startIn)
        return boss.guard.escapes()
    }

    fun allowed(pattern: D1000BombPattern, startIn: Float): Boolean {
        forecast(pattern, startIn)
        return boss.guard.escapes()
    }
}
