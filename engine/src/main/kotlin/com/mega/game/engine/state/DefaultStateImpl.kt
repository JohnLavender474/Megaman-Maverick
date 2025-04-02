package com.mega.game.engine.state

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.objects.MutableArray

class DefaultStateImpl<T>(
    override var element: T,
    val transitions: MutableCollection<Transition<T>> = MutableArray<Transition<T>>()
) : IState<T> {

    override fun addTransition(condition: (Array<Any?>) -> Boolean, nextState: IState<T>) =
        transitions.add(Transition(condition, nextState))

    override fun getNextState(params: Array<Any?>): IState<T>? =
        transitions.firstOrNull { transition -> transition.condition(params) }?.nextState
}
