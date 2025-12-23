package com.mega.game.engine.behaviors

/**
 * A base implementation of [IBehavior]. When extending this class, only the [evaluate] method is required to be
 * overridden, with the other methods of the interface being optional.
 */
abstract class AbstractBehaviorImpl : IBehavior {

    private var runningNow = false

    /**
     * Default implementation is a no-op.
     *
     * @param delta the delta time since the previous frame
     */
    override fun act(delta: Float) {
        // default no-op implementation
    }

    /**
     * Default implementation is a no-op.
     */
    override fun init() {
        // default no-op implementation
    }

    /**
     * Default implementation is a no-op.
     */
    override fun end() {
        // default no-op implementation
    }

    /**
     * If the behavior is currently active.
     *
     * @return if the behavior is active
     */
    override fun isActive() = runningNow

    /**
     * If the behavior is currently running, then the behavior is immediately ended, otherwise nothing happens.
     */
    override fun reset() {
        if (runningNow) {
            end()
            runningNow = false
        }
    }

    /**
     * Updates the behavior by evaluating the behavior and then running the process method which corresponds to the
     * behavior's current state.
     *
     * @param delta the delta time since the previous frame
     */
    override fun update(delta: Float) {
        val runningPrior = runningNow
        runningNow = evaluate(delta)

        if (runningNow && !runningPrior) init()
        if (runningNow) act(delta)
        if (!runningNow && runningPrior) end()
    }
}
