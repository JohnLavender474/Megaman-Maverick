package com.megaman.maverick.game.entities.bosses.d1000

import com.badlogic.gdx.math.Vector2
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.decorations.D1000DebrisWarning
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import com.megaman.maverick.game.entities.bosses.D1000
import com.megaman.maverick.game.entities.bosses.D1000State

// TODO: the D-1000 fight (its behaviors, mechanics, tuning and anti-trap rules) was AI-generated from owner play tests; it needs more scrutiny and refinement (see the branch NOTES of 081f, section "AI-generated fight")
internal class D1000Ram(private val boss: D1000) : D1000Move {

    companion object {
        const val WIND_UP_SPEED = 4.25f * ConstVals.PPM
        const val RAM_ACCELERATION = 112.5f * ConstVals.PPM
        const val RAM_STUCK_DUR = 1.5f
        const val RICOCHET_AIM_DUR = 0.6f
        const val WALL_CRASH_FIRST = 0.5f * ConstVals.PPM
        const val WALL_CRASH_STEP = 1.75f * ConstVals.PPM
        const val DAZE_GAP = 0.75f * ConstVals.PPM
        const val DAZE_HEIGHT = 3f * ConstVals.PPM
        const val WALL_SHAKE = 0.5f
        const val WALL_IMPACT_X = 0.1875f
        const val WALL_IMPACT_Y = 0.0625f
        const val RAM_STRIP = 1.5f * ConstVals.PPM
        const val RAM_STRIP_AFTER = 0.5f
    }

    private enum class Step { START, WIND_UP, CHARGE, STUCK, RISE, RICOCHET_AIM }

    private var step = Step.START
    private var time = 0f
    private var bouncesLeft = 0

    override val holdsEyeShots: Boolean
        get() = step == Step.WIND_UP || step == Step.CHARGE || step == Step.RICOCHET_AIM

    override fun begin() = goTo(Step.START)

    private fun goTo(next: Step) {
        step = next
        time = 0f
    }

    override fun update(delta: Float) {
        time += delta
        when (step) {
            Step.START -> {
                boss.setAnim("walk")
                bouncesLeft = boss.tuning.ricochetBounces
                goTo(Step.WIND_UP)
            }

            Step.WIND_UP -> {
                boss.facePlayer()
                trackHeight(delta)
                if (time >= boss.tuning.ramWindUpDur) startCharge()
            }

            Step.CHARGE -> {
                boss.speedX = min(boss.speedX + RAM_ACCELERATION * delta, boss.tuning.ramMaxSpeed)
                if (boss.moveX(delta, boss.speedX)) {
                    boss.speedX = 0f
                    boss.impact(WALL_SHAKE, WALL_IMPACT_X, WALL_IMPACT_Y, bouncesLeft == 0)
                    wallCrash()
                    if (bouncesLeft > 0) {
                        bouncesLeft--
                        boss.facing = boss.facing.opposite()
                        boss.setAnim("walk")
                        goTo(Step.RICOCHET_AIM)
                    } else {
                        boss.core?.daze(
                            Vector2(
                                boss.origin.x - boss.facing.value * (D1000.HALF_WIDTH + DAZE_GAP),
                                boss.floor + DAZE_HEIGHT
                            )
                        )
                        boss.setAnim("stand")
                        goTo(Step.STUCK)
                    }
                }
            }

            Step.STUCK -> {
                if (time >= RAM_STUCK_DUR - D1000Lamp.LEAD_DUR) boss.tellFollowUp()
                if (time >= RAM_STUCK_DUR) {
                    boss.setAnim("walk")
                    goTo(Step.RISE)
                }
            }

            Step.RISE -> if (boss.moveToHover(delta, true)) boss.finishAction(D1000State.WALK)

            Step.RICOCHET_AIM -> {
                trackHeight(delta, true)
                if (time >= RICOCHET_AIM_DUR) startCharge()
            }
        }
    }

    private fun trackHeight(delta: Float, hold: Boolean = false) {
        val target = boss.hoverTarget()
        if (hold && boss.approachBlocked(target)) return
        boss.origin.y = approach(boss.origin.y, target, WIND_UP_SPEED, delta)
    }

    private fun startCharge() {
        boss.setAnim("ram")
        boss.speedX = 0f
        boss.requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)
        goTo(Step.CHARGE)
    }

    private fun wallCrash() {
        val arena = boss.arena
        val facing = boss.facing.value
        val farEdge = boss.origin.x - facing * D1000.HALF_WIDTH
        for (i in 0 until boss.tuning.wallCrashPieces) {
            val x = farEdge - facing * (WALL_CRASH_FIRST + i * WALL_CRASH_STEP)
            if (x < arena.getX() + D1000Orbs.LANE_MARGIN || x > arena.getMaxX() - D1000Orbs.LANE_MARGIN) continue
            boss.spawnDebris(x, arena.getMaxY() - D1000.DEBRIS_TOP)
        }
    }

    private fun ramTime(distance: Float, speed: Float): Float {
        val accelDistance = speed * speed / (2f * RAM_ACCELERATION)
        return if (distance <= accelDistance) sqrt(2f * distance / RAM_ACCELERATION)
        else speed / RAM_ACCELERATION + (distance - accelDistance) / speed
    }

    override fun forecastNow() = Unit

    override fun forecast(startIn: Float) {
        allowed(startIn)
    }

    override fun allowed(startIn: Float): Boolean {
        val boxes = boss.bodyBoxes()
        if (boxes.isEmpty) return false
        val guard = boss.guard
        val arena = boss.arena
        val origin = boss.origin
        val tuning = boss.tuning
        val moves = guard.moves
        val playerX = boss.playerX()
        val toward = if (playerX >= origin.x) 1f else -1f
        val wall = if (toward > 0f) arena.getMaxX() else arena.getX()
        val front = if (toward > 0f) boxes.maxOf { it.getMaxX() } else boxes.minOf { it.getX() }
        val reach = (wall - front) * toward
        val run = reach - 2f * D1000XMoves.HALF_WIDTH
        if (run <= 0f) return false

        val speed = tuning.ramMaxSpeed
        val charge = startIn + tuning.ramWindUpDur
        val runTime = ramTime(run, speed)
        val hit = charge + ramTime(reach, speed)
        val center = origin.y + D1000.BODY_CENTER_ABOVE_ORIGIN
        val drop = center - boxes.minOf { it.getY() } + D1000XMoves.HEIGHT / 2f + moves.margin
        val lowTop = boxes.maxOf { it.getMaxY() } - origin.y + boss.lowestHover - boss.floor + moves.margin
        val walkIn = moves.reaction + max(abs(wall - playerX) - D1000XMoves.HALF_WIDTH, 0f) / moves.speed
        val highThenDrop = walkIn + D1000XMoves.climbTime(drop) <= charge &&
            runTime >= moves.timedReaction + D1000XMoves.fallTime(drop)
        val climbOver = walkIn <= charge && runTime >= moves.timedReaction + D1000XMoves.climbTime(lowTop)
        if (!highThenDrop && !climbOver) return false

        val stripLeft = if (toward > 0f) wall - RAM_STRIP else wall
        val onWall = if (highThenDrop) charge - D1000XMoves.climbTime(drop) else charge
        if (!guard.escape.free(stripLeft, stripLeft + RAM_STRIP, onWall, hit + RAM_STRIP_AFTER)) return false

        val firstOrigin = if (toward > 0f) boss.maxX else boss.minX
        addWallCrash(firstOrigin, toward, hit)
        if (tuning.ricochetBounces > 0) {
            val secondHit = hit + RICOCHET_AIM_DUR + ramTime(boss.maxX - boss.minX, speed)
            val secondOrigin = if (toward > 0f) boss.minX else boss.maxX
            addWallCrash(secondOrigin, -toward, secondHit)
            guard.escape.stun(secondHit, secondHit + tuning.xStunDur)
        } else guard.escape.stun(hit, hit + tuning.xStunDur)
        return guard.escapesFrom(wall - toward * D1000XMoves.HALF_WIDTH, hit)
    }

    private fun addWallCrash(bodyX: Float, toward: Float, hit: Float) {
        val arena = boss.arena
        val farEdge = bodyX - toward * D1000.HALF_WIDTH
        for (i in 0 until boss.tuning.wallCrashPieces) {
            val x = farEdge - toward * (WALL_CRASH_FIRST + i * WALL_CRASH_STEP)
            if (x < arena.getX() + D1000Orbs.LANE_MARGIN || x > arena.getMaxX() - D1000Orbs.LANE_MARGIN) continue
            boss.addDebrisAt(x, hit + D1000DebrisWarning.LEAD_DUR)
        }
    }
}
