package com.mega.game.engine.factories

interface IFactory<K, V> {

    fun fetch(key: K): V?
}
