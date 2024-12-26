package com.mega.game.engine.common.interfaces


interface ArgsPredicate<T> {


    fun test(arg: T): Boolean
}
