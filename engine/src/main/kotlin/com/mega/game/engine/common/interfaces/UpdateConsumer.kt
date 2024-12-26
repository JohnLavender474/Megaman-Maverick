package com.mega.game.engine.common.interfaces


interface UpdateConsumer<T> {


    fun consumeUpdate(delta: Float, value: T)
}