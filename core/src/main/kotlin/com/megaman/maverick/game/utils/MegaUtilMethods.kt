package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

object MegaUtilMethods {

    private class DelayedRun(val id: String) : Updatable {

        val timer = Timer(1f)
        var action: (() -> Unit)? = null
        var finished = false
        var services: MegaServices? = null

        val remove: () -> Unit = {
            services!!.updatables.remove(id)
            release(this)
        }

        override fun update(delta: Float) {
            timer.update(delta)
            if (timer.isJustFinished()) {
                finished = true
                action!!.invoke()

                services!!.runQueue.addLast(remove)
            }
        }
    }

    private val freeDelayedRuns = Array<DelayedRun>()
    private var delayedRunCount = 0

    private fun release(run: DelayedRun) {
        run.action = null
        run.services = null
        freeDelayedRuns.add(run)
    }

    fun delayRun(services: MegaServices, delay: Float, action: () -> Unit): String {
        if (delay <= 0f) throw IllegalArgumentException("Delay must be greater than 0: $delay")

        val run = if (freeDelayedRuns.isEmpty) DelayedRun("delay_run_${delayedRunCount++}") else freeDelayedRuns.pop()
        run.services = services
        run.action = action
        run.finished = false
        run.timer.resetDuration(delay)

        services.updatables.put(run.id, run)

        return run.id
    }

    fun cancelDelayRun(services: MegaServices, id: String) {
        val run = services.updatables.get(id) ?: return
        if (run !is DelayedRun) throw IllegalArgumentException("Not an id of delayRun: $id")
        if (run.finished) return
        services.updatables.remove(id)
        release(run)
    }

    fun getSmallFontSize() = (ConstVals.PPM / 3f).roundToInt()

    fun getDefaultFontSize() = (ConstVals.PPM / 2f).roundToInt()

    fun getLargeFontSize() = (ConstVals.PPM / 1.5f).roundToInt()

    fun calculateJumpImpulse(
        source: Vector2,
        target: Vector2,
        verticalBaseImpulse: Float,
        horizontalScalar: Float = 1f,
        verticalScalar: Float = 1f
    ) = calculateJumpImpulse(
        source.x,
        source.y,
        target.x,
        target.y,
        horizontalScalar,
        verticalBaseImpulse,
        verticalScalar,
        GameObjectPools.fetch(Vector2::class)
    )

    private fun calculateJumpImpulse(
        sourceX: Float, sourceY: Float,
        targetX: Float, targetY: Float,
        horizontalScalar: Float, verticalBaseImpulse: Float,
        verticalScalar: Float,
        out: Vector2
    ): Vector2 {
        val horizontalDistance = targetX - sourceX
        val verticalDistance = targetY - sourceY

        val impulseX = horizontalDistance * horizontalScalar
        val impulseY = verticalBaseImpulse + (verticalDistance * verticalScalar)

        return out.set(impulseX, impulseY)
    }

    fun interpolate(start: Vector2, target: Vector2, delta: Float) =
        UtilMethods.interpolate(start, target, delta, GameObjectPools.fetch(Vector2::class))

    fun snapToNearest90(rotation: Float) = ((rotation / 90f).roundToInt() * 90f) % 360f

    fun snapToFloor90(rotation: Float) = (floor(rotation / 90f).toInt() * 90f) % 360f

    fun snapToCeil90(rotation: Float) = (ceil(rotation / 90f).toInt() * 90f) % 360f
}
