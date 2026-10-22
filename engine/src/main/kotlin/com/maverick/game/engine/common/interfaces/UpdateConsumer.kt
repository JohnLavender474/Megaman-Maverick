package com.maverick.game.engine.common.interfaces


interface UpdateConsumer<T> {


    fun consumeUpdate(delta: Float, value: T)
}