package com.mega.game.engine.state

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap

class StateMachineBuilder<T> {

    private val stateDefinitions = ObjectMap<String, T>()
    private val transitionDefinitions = Array<Triple<String, String, () -> Boolean>>()
    private var initialStateName: String? = null
    private var onChangeState: ((T, T) -> Unit)? = null
    private var triggerChangeWhenSameElement = false

    fun state(name: String, element: T): StateMachineBuilder<T> {
        stateDefinitions.put(name, element)
        return this
    }

    fun states(receiver: (states: ObjectMap<String, T>) -> Unit): StateMachineBuilder<T> {
        receiver.invoke(stateDefinitions)
        return this
    }

    fun transition(fromState: String, toState: String, condition: () -> Boolean): StateMachineBuilder<T> {
        transitionDefinitions.add(Triple(fromState, toState, condition))
        return this
    }

    fun initialState(name: String): StateMachineBuilder<T> {
        initialStateName = name
        return this
    }

    fun setOnChangeState(onChangeState: ((T, T) -> Unit)? = null): StateMachineBuilder<T> {
        this.onChangeState = onChangeState
        return this
    }

    fun setTriggerChangeWhenSameElement(trigger: Boolean): StateMachineBuilder<T> {
        triggerChangeWhenSameElement = trigger
        return this
    }

    fun build(): StateMachine<T> {
        val states = mutableMapOf<String, IState<T>>()
        stateDefinitions.forEach { states[it.key] = DefaultStateImpl(it.value) }
        val initialState = states[initialStateName]
            ?: throw IllegalStateException("Initial state $initialStateName needs to be added via the [state] method")
        for ((fromStateName, toStateName, condition) in transitionDefinitions) {
            val fromState = states[fromStateName]
                ?: throw IllegalArgumentException("Building transition with \"from\" state: state $fromStateName not found")
            val toState = states[toStateName]
                ?: throw IllegalArgumentException("Building transition with \"to\" state: state $toStateName not found")
            fromState.addTransition(condition, toState)
        }
        val stateMachine = StateMachine(initialState)
        onChangeState?.let { stateMachine.onChangeState = it }
        stateMachine.triggerChangeWhenSameElement = triggerChangeWhenSameElement
        return stateMachine
    }
}
