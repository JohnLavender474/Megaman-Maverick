package com.mega.game.engine.state


interface IState<T> {


    var element: T


    fun addTransition(condition: () -> Boolean, nextState: IState<T>): Boolean


    fun getNextState(): IState<T>?
}