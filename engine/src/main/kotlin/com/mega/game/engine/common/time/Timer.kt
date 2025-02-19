package com.mega.game.engine.common.time

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.interfaces.IJustFinishable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import java.util.*
import kotlin.math.min

class Timer(duration: Float) : Updatable, Resettable, IJustFinishable {

    companion object {
        const val DEFAULT_TIME = 1f
    }

    var duration = duration
        private set
    var time = 0f
        private set
    var justFinished = false
        private set

    // the array of runnables which should be added to the queue on every reset
    internal var runnables = Array<TimeMarkedRunnable>()

    // the queue which is polled on every update and refilled with the elements in [runnables] on every reset
    internal var runnableQueue = PriorityQueue<TimeMarkedRunnable>()

    private var runOnFirstUpdate: (() -> Unit)? = null
    private var runOnJustFinished: (() -> Unit)? = null
    private var runOnFinished: (() -> Unit)? = null

    private var firstUpdate = true

    constructor() : this(DEFAULT_TIME)

    constructor(duration: Float, vararg runnables: TimeMarkedRunnable) : this(duration, Array(runnables))

    constructor(duration: Float, runnables: Array<TimeMarkedRunnable>) : this(duration, false, runnables)

    constructor(duration: Float, setToEnd: Boolean, runnables: Array<TimeMarkedRunnable>) : this(duration) {
        addRunnables(runnables)
        time = if (setToEnd) duration else 0f
    }

    override fun update(delta: Float) {
        if (firstUpdate) {
            runOnFirstUpdate?.invoke()
            firstUpdate = false
        }

        val finishedBefore = isFinished()

        val oldTime = time
        time = min(this@Timer.duration, time + delta)

        var qTime = runnableQueue.peek()?.time
        while (qTime != null && qTime <= time) {
            val runnable = runnableQueue.poll()

            when {
                runnable.shouldRunOnlyWhenJustPassedTime() -> if (qTime >= oldTime) runnable.run()
                else -> runnable.run()
            }

            qTime = runnableQueue.peek()?.time
        }

        justFinished = !finishedBefore && isFinished()

        if (justFinished) runOnJustFinished?.invoke()

        if (isFinished()) runOnFinished?.invoke()
    }

    override fun reset() {
        time = 0f

        firstUpdate = true
        justFinished = false

        runnableQueue.clear()
        runnables.forEach { runnableQueue.add(it) }
    }

    fun setRunOnFirstupdate(runnable: (() -> Unit)?): Timer {
        runOnFirstUpdate = runnable
        return this
    }

    fun setRunOnJustFinished(runnable: (() -> Unit)?): Timer {
        runOnJustFinished = runnable
        return this
    }

    fun setRunOnFinished(runnable: (() -> Unit)?): Timer {
        runOnFinished = runnable
        return this
    }

    fun resetDuration(duration: Float): Timer {
        this.duration = duration
        reset()
        return this
    }

    fun getRatio() = if (this@Timer.duration > 0f) min(time / this@Timer.duration, 1f) else 0f

    fun isAtBeginning() = time == 0f

    override fun isFinished() = time >= this@Timer.duration

    override fun isJustFinished() = justFinished

    fun addRunnable(runnable: TimeMarkedRunnable): Timer {
        runnableQueue.add(runnable)
        this.runnables.add(runnable)
        return this
    }

    fun addRunnables(vararg runnables: TimeMarkedRunnable): Timer {
        runnables.forEach { addRunnable(it) }
        return this
    }

    fun addRunnables(runnables: Iterable<TimeMarkedRunnable>): Timer {
        runnables.forEach { addRunnable(it) }
        return this
    }

    fun clearRunnables(): Timer {
        runnables.clear()
        runnableQueue.clear()
        return this
    }

    fun setToEnd(allowJustFinished: Boolean = true): Timer {
        val oldTime = time
        time = this@Timer.duration
        justFinished = if (allowJustFinished) oldTime != time else false
        return this
    }
}
