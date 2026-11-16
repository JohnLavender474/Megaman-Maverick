package com.megaman.maverick.game.utils

import com.badlogic.gdx.utils.Array

class OwnedArray<T : Any>(private val supplier: () -> T) {

    @PublishedApi
    internal val store = Array<T>()

    var size = 0
        private set

    val isEmpty: Boolean
        get() = size == 0

    fun add(): T {
        if (size == store.size) store.add(supplier())
        return store[size++]
    }

    operator fun get(index: Int): T {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("index=$index, size=$size")
        return store[index]
    }

    fun clear() {
        size = 0
    }

    inline fun forEach(action: (T) -> Unit) {
        for (i in 0 until size) action(store[i])
    }

    inline fun any(predicate: (T) -> Boolean): Boolean {
        for (i in 0 until size) if (predicate(store[i])) return true
        return false
    }
}
