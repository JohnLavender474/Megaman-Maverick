package com.mega.game.engine.state


class Transition<T>(val condition: () -> Boolean, val nextState: IState<T>)