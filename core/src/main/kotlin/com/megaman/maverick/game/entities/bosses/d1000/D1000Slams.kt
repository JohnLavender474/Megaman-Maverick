package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.D1000DebrisWarning
import com.megaman.maverick.game.entities.hazards.D1000Shockwave
import com.megaman.maverick.game.entities.utils.TickFrames
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import com.megaman.maverick.game.entities.bosses.D1000
import com.megaman.maverick.game.entities.bosses.D1000State

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
internal class D1000SlamWaves(private val boss: D1000) {

    companion object {
        const val SHOCKWAVE_AFTER_STUN = 0.4f
    }

    private var wavesIn = -1f
    private var wavesX = 0f

    fun reset() {
        wavesIn = -1f
    }

    fun schedule(x: Float) {
        wavesIn = boss.tuning.xStunDur + SHOCKWAVE_AFTER_STUN
        wavesX = x
    }

    fun update(delta: Float) {
        if (wavesIn < 0f) return
        wavesIn -= delta
        if (wavesIn > 0f) return
        wavesIn = -1f
        Facing.entries.forEach { direction ->
            val wave = MegaEntityFactory.fetch(D1000Shockwave::class)!!
            wave.spawn(
                props(
                    ConstKeys.POSITION pairTo Vector2(wavesX + direction.value * D1000.HALF_WIDTH, boss.floor),
                    ConstKeys.FACING pairTo direction,
                    ConstKeys.BOUNDS pairTo GameRectangle(boss.arena)
                )
            )
        }
    }

    fun forecast(x: Float, landIn: Float) = Facing.entries.forEach { direction ->
        boss.guard.addShockwave(
            x + direction.value * D1000.HALF_WIDTH, direction, landIn + boss.tuning.xStunDur + SHOCKWAVE_AFTER_STUN
        )
    }
}

internal abstract class D1000Slam(protected val boss: D1000) : D1000Move {

    companion object {
        const val CROUCH_GRAVITY = 56f * ConstVals.PPM
        const val CROUCH_SHAKE_DUR = 0.5f
        const val CROUCH_HOLD_DUR = 0.75f
        const val LAND_IMPACT_X = 0.03125f
        const val LAND_IMPACT_Y = 0.125f
        const val DROP_LEAD = 0.15f
        private val CROUCH_ANIMS = arrayOf("crouch_drop", "crouch_shake")
        val CROUCH_REACH = CROUCH_ANIMS.maxOf { frontReach(it) }

        private fun frontReach(anim: String): Float {
            val def = D1000Frames.BODY[anim]!!
            val box = GameRectangle()
            return D1000Frames.BODY_BOXES[anim]!!.maxOf { boxes ->
                (0 until boxes.size / 4).maxOf { -TickFrames.placeBox(box, def, boxes, it * 4, 0f, 0f, false).getX() }
            }
        }
    }

    protected var time = 0f

    override val holdsEyeShots: Boolean
        get() = false

    protected fun drop(delta: Float) = boss.fall(delta, CROUCH_GRAVITY, boss.floor + D1000.CROUCH_HEIGHT)

    protected fun landing(debris: Boolean) {
        boss.impact(D1000.LAND_SHAKE, LAND_IMPACT_X, LAND_IMPACT_Y, true)
        if (debris) {
            val center = boss.playerX()
            boss.tuning.crouchDebris.forEach { offset ->
                val x = center + offset * ConstVals.PPM
                if (boss.laneInArena(x)) boss.spawnDebris(x, boss.arena.getMaxY() - D1000.DEBRIS_TOP)
            }
        }
        if (!boss.tuning.slamShockwaves) return
        boss.slamWaves.schedule(boss.origin.x)
    }

    protected fun hold(): Boolean {
        if (time >= CROUCH_HOLD_DUR - D1000Lamp.LEAD_DUR) boss.tellFollowUp()
        boss.shakeOffset = if (time < CROUCH_SHAKE_DUR && flickerOn(time)) D1000.SHAKE_STEP else 0f
        if (time < CROUCH_HOLD_DUR && !boss.touchedPlayer) return false
        boss.tellFollowUp()
        boss.shakeOffset = 0f
        boss.setAnim("walk")
        return true
    }

    protected fun fallTime() = fallTimeFrom(boss.origin.y)

    protected fun fallTimeFrom(y: Float) = sqrt(2f * max(y - boss.floor - D1000.CROUCH_HEIGHT, 0f) / CROUCH_GRAVITY)

    protected fun addLanding(landIn: Float, x: Float, debris: Boolean) {
        val guard = boss.guard
        guard.escape.stun(landIn, landIn + boss.tuning.xStunDur)
        if (debris) boss.tuning.crouchDebris.forEach { offset ->
            val lane = boss.playerX() + offset * ConstVals.PPM
            if (boss.laneInArena(lane)) boss.addDebrisAt(lane, landIn + D1000DebrisWarning.LEAD_DUR)
        }
        if (boss.tuning.slamShockwaves) boss.slamWaves.forecast(x, landIn)
    }

    protected fun addDrop(landIn: Float, x: Float) {
        boss.guard.addFootprint(
            boss.crouchBoxes(x, boss.playerX() > x), 0f, landIn - DROP_LEAD, D1000Escape.HORIZON, false
        )
        addLanding(landIn, x, false)
    }

    override fun allowed(startIn: Float): Boolean {
        forecast(startIn)
        return boss.guard.escapes()
    }
}

internal class D1000Crouch(boss: D1000) : D1000Slam(boss) {

    private enum class Step { START, DROP, HOLD, RISE }

    private var step = Step.START

    override fun begin() = goTo(Step.START)

    private fun goTo(next: Step) {
        step = next
        time = 0f
    }

    override fun update(delta: Float) {
        time += delta
        when (step) {
            Step.START -> {
                boss.touchedPlayer = false
                boss.speedY = 0f
                boss.setAnim("crouch_drop")
                goTo(Step.DROP)
            }

            Step.DROP -> if (drop(delta)) {
                boss.setAnim("crouch_shake")
                landing(true)
                goTo(Step.HOLD)
            }

            Step.HOLD -> if (hold()) goTo(Step.RISE)

            Step.RISE -> if (boss.moveToHover(delta)) boss.finishAction(D1000State.THINK)
        }
    }

    override fun forecastNow() {
        if (step == Step.START || step == Step.DROP) addLanding(fallTime(), boss.origin.x, true)
    }

    override fun forecast(startIn: Float) {
        val boxes = boss.bodyBoxes()
        val landIn = startIn + fallTime()
        if (!boss.guard.touches(boxes)) boss.guard.addFootprint(
            boss.crouchBoxes(boss.origin.x, boss.isFacing(Facing.RIGHT)), 0f, landIn - DROP_LEAD, D1000Escape.HORIZON, false
        )
        addLanding(landIn, boss.origin.x, true)
    }
}

internal class D1000Ceiling(boss: D1000) : D1000Slam(boss) {

    companion object {
        const val CEILING_RISE_SPEED = 10f * ConstVals.PPM
        const val CEILING_TRACK_SPEED = 5f * ConstVals.PPM
        const val CEILING_TRACK_DUR = 1f
        const val CEILING_LANE_GAP = 2.5f * ConstVals.PPM
        const val CEILING_DEBRIS_SPREAD = 0.3f * ConstVals.PPM
        const val CEILING_POCKET = 1.25f * ConstVals.PPM
        const val CEILING_POCKET_LANE = 0.75f * ConstVals.PPM
    }

    private enum class Step { START, RISE, TRACK, JITTER, DROP, HOLD, RETURN }

    private var step = Step.START

    override fun begin() = goTo(Step.START)

    private fun goTo(next: Step) {
        step = next
        time = 0f
    }

    override fun update(delta: Float) {
        time += delta
        val origin = boss.origin
        when (step) {
            Step.START -> {
                boss.setAnim("walk")
                goTo(Step.RISE)
            }

            Step.RISE -> {
                origin.y = min(origin.y + CEILING_RISE_SPEED * delta, boss.highestHover)
                if (origin.y == boss.highestHover) {
                    boss.impact(D1000.LAND_SHAKE, LAND_IMPACT_X, LAND_IMPACT_Y, false)
                    debris()
                    boss.setAnim("stand")
                    goTo(Step.TRACK)
                }
            }

            Step.TRACK -> {
                boss.facePlayer()
                origin.x = approach(origin.x, trackX(boss.playerX()), CEILING_TRACK_SPEED, delta)
                if (time >= CEILING_TRACK_DUR) goTo(Step.JITTER)
            }

            Step.JITTER -> {
                boss.shakeOffset = if (flickerOn(time)) D1000.SHAKE_STEP else 0f
                if (time >= boss.tuning.ceilingJitterDur) {
                    boss.shakeOffset = 0f
                    boss.speedY = 0f
                    boss.touchedPlayer = false
                    boss.setAnim("crouch_drop")
                    goTo(Step.DROP)
                }
            }

            Step.DROP -> if (drop(delta)) {
                boss.setAnim("crouch_shake")
                landing(false)
                goTo(Step.HOLD)
            }

            Step.HOLD -> if (hold()) goTo(Step.RETURN)

            Step.RETURN -> if (boss.moveToHover(delta)) boss.finishAction(D1000State.THINK)
        }
    }

    private fun debris() {
        val lanes = boss.tuning.ceilingLanes
        val center = boss.playerX()
        for (k in -lanes - 1..lanes) {
            val x = center + (k + 0.5f) * CEILING_LANE_GAP
            if (lane(x)) boss.spawnDebris(x, boss.arena.getMaxY() - D1000.DEBRIS_TOP, CEILING_DEBRIS_SPREAD)
        }
    }

    private fun lane(x: Float): Boolean {
        val arena = boss.arena
        return x >= arena.getX() + CEILING_POCKET + CEILING_POCKET_LANE &&
            x <= arena.getMaxX() - CEILING_POCKET - CEILING_POCKET_LANE
    }

    private fun trackX(x: Float): Float {
        val arena = boss.arena
        return x.coerceIn(
            max(boss.minX, arena.getX() + CROUCH_REACH + CEILING_POCKET),
            min(boss.maxX, arena.getMaxX() - CROUCH_REACH - CEILING_POCKET)
        )
    }

    private fun dropX(trackLeft: Float): Float {
        val reach = CEILING_TRACK_SPEED * max(trackLeft, 0f)
        return boss.origin.x + (trackX(boss.playerX()) - boss.origin.x).coerceIn(-reach, reach)
    }

    private fun dropFall() = fallTimeFrom(boss.highestHover)

    override fun forecastNow() {
        val origin = boss.origin
        when (step) {
            Step.RISE, Step.TRACK -> {
                val rise = if (step == Step.RISE) max(boss.highestHover - origin.y, 0f) / CEILING_RISE_SPEED else 0f
                val track = CEILING_TRACK_DUR - if (step == Step.TRACK) time else 0f
                addDrop(rise + track + boss.tuning.ceilingJitterDur + dropFall(), dropX(track))
            }

            Step.JITTER -> addDrop(max(boss.tuning.ceilingJitterDur - time, 0f) + dropFall(), origin.x)
            Step.DROP -> addDrop(fallTime(), origin.x)
            else -> Unit
        }
    }

    override fun forecast(startIn: Float) {
        val boxes = boss.bodyBoxes()
        val rise = max(boss.highestHover - boss.origin.y, 0f) / CEILING_RISE_SPEED
        boss.guard.addFootprint(boxes, 0f, 0f, startIn + rise, true)
        val lanes = boss.tuning.ceilingLanes
        for (k in -lanes - 1..lanes) {
            val lane = boss.playerX() + (k + 0.5f) * CEILING_LANE_GAP
            if (lane(lane)) boss.addDebrisAt(lane, startIn + rise + D1000DebrisWarning.LEAD_DUR, CEILING_DEBRIS_SPREAD)
        }
        addDrop(startIn + rise + CEILING_TRACK_DUR + boss.tuning.ceilingJitterDur + dropFall(), dropX(CEILING_TRACK_DUR))
    }
}
