package com.mega.game.engine.common.objects

data class Wrapper<T>(var data: T) {

    fun set(data: T) {
        this.data = data
    }

    fun get() = data
}
