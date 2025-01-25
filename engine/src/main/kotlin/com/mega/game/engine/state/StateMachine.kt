package com.mega.game.engine.state

import com.mega.game.engine.common.interfaces.Resettable

class StateMachine<T>(
    var initialState: IState<T>,
    var onChangeState: ((currentElement: T, previousElement: T) -> Unit)? = null,
    var triggerChangeWhenSameElement: Boolean = false,
    var callOnChangeStateOnReset: Boolean = false
) : Resettable {

    private var currentState: IState<T> = initialState

    fun setState(state: IState<T>, callOnChangeState: Boolean = true) {
        val previousState = currentState
        currentState = state
        if (callOnChangeState) onChangeState?.invoke(currentState.element, previousState.element)
    }

    fun next(): T {
        val nextState = currentState.getNextState()
        if (nextState != null && (currentState != nextState || triggerChangeWhenSameElement)) {
            val previousStateElement = currentState.element
            currentState = nextState
            onChangeState?.invoke(currentState.element, previousStateElement)
        }
        return currentState.element
    }

    fun getCurrent() = currentState.element

    override fun reset() {
        val previousState = currentState
        currentState = initialState
        if (callOnChangeStateOnReset) onChangeState?.invoke(currentState.element, previousState.element)
    }
}
