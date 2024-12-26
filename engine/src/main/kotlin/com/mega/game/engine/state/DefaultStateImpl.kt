package com.mega.game.engine.state

import com.mega.game.engine.common.objects.MutableArray


class DefaultStateImpl<T>(
    override var element: T,
    val transitions: MutableCollection<Transition<T>> = MutableArray<Transition<T>>()
) : IState<T> {


    override fun addTransition(condition: () -> Boolean, nextState: IState<T>) =
        transitions.add(Transition(condition, nextState))


    override fun getNextState(): IState<T>? =
        transitions.firstOrNull { transition -> transition.condition() }?.nextState
}