package com.maverick.game.engine.common.interfaces

fun interface IContainable<T> {

    fun isContainedIn(container: T): Boolean
}
