package com.mega.game.engine.common.time

/**
 * A runnable which should run in a timer only when the specified [time] has transpired.
 *
 * @property time The time at which to run
 * @property runnable The runnable to run
 * @property runOnlyWhenJustPassedTime If this is true, then the runnable is run in an update only
 * when the previous time is less than the [time] value and the new time is greater than the [time]
 * value. Otherwise, the runnable is run once the current time becomes greater than the [time] value
 * regardless of the previous time's value.
 */
class TimeMarkedRunnable(
    val time: Float,
    val runnable: () -> Unit
) : Runnable, Comparable<TimeMarkedRunnable> {

    private var runOnlyWhenJustPassedTime: Boolean = true

    constructor(
        time: Float, runnable: Runnable
    ) : this(time, { runnable.run() })

    fun setToRunOnlyWhenJustPassedTime(runOnlyWhenJustPassedTime: Boolean): TimeMarkedRunnable {
        this.runOnlyWhenJustPassedTime = runOnlyWhenJustPassedTime
        return this
    }

    fun shouldRunOnlyWhenJustPassedTime() = runOnlyWhenJustPassedTime

    override fun run() = runnable()

    override fun compareTo(other: TimeMarkedRunnable) = time.compareTo(other.time)

    override fun equals(other: Any?) = other is TimeMarkedRunnable && other.time.equals(time)

    override fun hashCode() = time.hashCode()
}
