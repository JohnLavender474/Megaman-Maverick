package com.mega.game.engine.common.time

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import kotlin.math.min

class Timer(_duration: Float) : Updatable, Resettable {

    companion object {
        const val DEFAULT_TIME = 1f
    }

    internal var runnables: Array<TimeMarkedRunnable> = Array()
    internal var runnableQueue = Queue<TimeMarkedRunnable>()

    var duration = _duration
        private set
    var time = 0f
        private set
    var justFinished = false
        private set

    var runOnFirstUpdate: (() -> Unit)? = null
    var runOnFinished: (() -> Unit)? = null

    private var firstUpdate = true

    constructor() : this(DEFAULT_TIME)

    constructor(
        duration: Float, vararg _runnables: TimeMarkedRunnable
    ) : this(duration, Array(_runnables))

    constructor(
        duration: Float, _runnables: Array<TimeMarkedRunnable>
    ) : this(duration, false, _runnables)

    constructor(
        duration: Float, setToEnd: Boolean, _runnables: Array<TimeMarkedRunnable>
    ) : this(duration) {
        setRunnables(_runnables)
        time = if (setToEnd) duration else 0f
    }

    override fun update(delta: Float) {
        if (firstUpdate) {
            runOnFirstUpdate?.invoke()
            firstUpdate = false
        }
        val finishedBefore = isFinished()
        time = min(duration, time + delta)
        while (!runnableQueue.isEmpty && runnableQueue.first().time <= time) runnableQueue.removeFirst().run()
        justFinished = !finishedBefore && isFinished()
        if (justFinished) runOnFinished?.invoke()
    }

    override fun reset() {
        time = 0f
        justFinished = false
        firstUpdate = true
        runnableQueue.clear()
        val temp = Array(runnables)
        temp.sort()
        temp.forEach { runnableQueue.addLast(it) }
    }

    fun resetDuration(duration: Float) {
        this.duration = duration
        reset()
    }

    fun getRatio() = if (duration > 0f) min(time / duration, 1f) else 0f

    fun isAtBeginning() = time == 0f

    fun isFinished() = time >= duration

    fun isJustFinished() = justFinished

    fun setRunnables(_runnables: Array<TimeMarkedRunnable>): Timer {
        runnables.clear()
        runnables.addAll(_runnables)

        runnableQueue.clear()
        val temp = Array(_runnables)
        temp.sort()
        temp.forEach { runnableQueue.addLast(it) }

        return this
    }

    fun clearRunnables(): Timer {
        runnables.clear()
        runnableQueue.clear()
        return this
    }

    fun setToEnd(allowJustFinished: Boolean = true): Timer {
        val oldTime = time
        time = duration
        justFinished = if (allowJustFinished) oldTime != time else false
        return this
    }
}