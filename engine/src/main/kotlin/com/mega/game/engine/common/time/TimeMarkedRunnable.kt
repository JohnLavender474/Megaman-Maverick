package com.mega.game.engine.common.time

class TimeMarkedRunnable(val time: Float, val runnable: () -> Unit) : Runnable, Comparable<TimeMarkedRunnable> {

    constructor(
        time: Float, runnable: Runnable
    ) : this(time, { runnable.run() })

    override fun run() = runnable()

    override fun compareTo(other: TimeMarkedRunnable) = time.compareTo(other.time)

    override fun equals(other: Any?) = other is TimeMarkedRunnable && other.time.equals(time)

    override fun hashCode() = time.hashCode()
}
