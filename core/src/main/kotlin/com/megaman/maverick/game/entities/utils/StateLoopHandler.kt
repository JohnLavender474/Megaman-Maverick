package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.orderedMapOfEntries
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.time.Timer

open class StateLoopHandler<T>(states: Array<T>, timers: Array<GamePair<T, Timer>>): Resettable, Updatable {

    private val stateLoop = Loop(states)
    private val stateTimers = orderedMapOfEntries(timers)

    override fun reset() {
        stateLoop.reset()
        stateTimers.values().forEach { it.reset() }
    }

    override fun update(delta: Float) {
        val currentState = getCurrentState()
        val stateTimer = getStateTimer()

        if (shouldUpdateTimerFor(currentState)) stateTimer?.update(delta)

        if (shouldGoToNextState(currentState, stateTimer)) {
            val newState = stateLoop.next()
            onChangeState(newState, currentState)
        }
    }

    protected fun getStateTimer(): Timer? = stateTimers[getCurrentState()]

    fun getCurrentState() = stateLoop.getCurrent()

    open fun shouldGoToNextState(state: T, timer: Timer?) = timer?.isFinished() == true

    open fun onChangeState(current: T, previous: T) {
        stateTimers[current]?.reset()
    }

    open fun shouldUpdateTimerFor(state: T) = true
}
