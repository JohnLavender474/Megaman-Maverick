package com.mega.game.engine.state

import com.badlogic.gdx.utils.Array


class Transition<T>(val condition: (Array<Any?>) -> Boolean, val nextState: IState<T>)
