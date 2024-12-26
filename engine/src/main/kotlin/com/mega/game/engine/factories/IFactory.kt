package com.mega.game.engine.factories

interface IFactory<T> {

    fun fetch(key: Any): T?
}
