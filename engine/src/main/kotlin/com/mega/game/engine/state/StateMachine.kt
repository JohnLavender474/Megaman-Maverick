package com.mega.game.engine.state

import com.mega.game.engine.common.interfaces.Resettable


class StateMachine<T>(
    var initialState: IState<T>,
    var onChangeState: ((currentElement: T, previousElement: T) -> Unit)? = null,
    var triggerChangeWhenSameElement: Boolean = false
) : Resettable {

    private var currentState: IState<T> = initialState


    fun setState(state: IState<T>) {
        currentState = state
    }


    fun next(): T {
        val nextState = currentState.getNextState()
        if (nextState != null && (triggerChangeWhenSameElement || currentState != nextState)) {
            val previousStateElement = currentState.element
            currentState = nextState
            onChangeState?.invoke(currentState.element, previousStateElement)
        }
        return currentState.element
    }


    fun getCurrent() = currentState.element


    override fun reset() {
        currentState = initialState
    }
}
