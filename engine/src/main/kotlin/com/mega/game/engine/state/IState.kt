package com.mega.game.engine.state

import com.badlogic.gdx.utils.Array

interface IState<T> {

    var element: T

    fun addTransition(condition: (params: Array<Any?>) -> Boolean, nextState: IState<T>): Boolean

    fun getNextState(params: Array<Any?> = Array()): IState<T>?
}
