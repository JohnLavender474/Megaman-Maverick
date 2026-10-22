package com.maverick.game.engine.common.interfaces

fun interface UpdateFunction<T> {

    fun update(delta: Float, t: T)
}
