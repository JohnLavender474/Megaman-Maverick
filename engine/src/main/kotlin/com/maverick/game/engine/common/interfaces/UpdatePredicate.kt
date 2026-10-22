package com.maverick.game.engine.common.interfaces

fun interface UpdatePredicate {

    fun test(delta: Float): Boolean
}
