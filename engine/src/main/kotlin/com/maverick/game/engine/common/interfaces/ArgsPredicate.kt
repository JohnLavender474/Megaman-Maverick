package com.maverick.game.engine.common.interfaces


interface ArgsPredicate<T> {


    fun test(arg: T): Boolean
}
