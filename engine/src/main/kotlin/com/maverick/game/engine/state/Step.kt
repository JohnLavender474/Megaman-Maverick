package com.maverick.game.engine.state

interface Step<C> {

    fun begin(context: C) {}

    fun isFinished(context: C): Boolean

    fun end(context: C, interrupted: Boolean) {}
}
