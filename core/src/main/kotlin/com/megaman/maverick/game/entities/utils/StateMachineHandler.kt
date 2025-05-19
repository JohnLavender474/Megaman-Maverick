package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.orderedMapOfEntries
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.state.StateMachine

abstract class StateMachineHandler<T>(
    private val stateMachine: StateMachine<T>,
    timers: Array<GamePair<T, Timer>>
) : Resettable, Updatable {

    private val stateTimers = orderedMapOfEntries(timers)

    override fun reset() {
        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }
    }

    override fun update(delta: Float) {
        val currentState = getCurrentState()
        val stateTimer = getStateTimer()

        if (shouldUpdateTimerFor(currentState)) stateTimer?.update(delta)

        if (shouldGoToNextState(currentState, stateTimer)) {
            stateTimer?.reset()

            val newState = stateMachine.next()
            onChangeState(newState, currentState)
        }
    }

    protected fun getStateTimer(): Timer? = stateTimers[getCurrentState()]

    fun getCurrentState() = stateMachine.getCurrentElement()

    open fun shouldGoToNextState(state: T, timer: Timer?) = timer?.isFinished() == true

    open fun onChangeState(current: T, previous: T) {}

    open fun shouldUpdateTimerFor(state: T) = true
}
