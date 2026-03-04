package com.mega.game.engine.common.extensions

import com.badlogic.gdx.utils.Queue

fun <T> Queue<T>.has(element: T): Boolean {
    for (i in 0 until size) {
        if (get(i) == element) return true
    }
    return false
}

fun <T> Queue<T>.offsetFromLast(offset: Int): T {
    if (offset < 0) throw IllegalArgumentException("Cannot offset by negative number")
    if (offset >= size) throw IndexOutOfBoundsException("Offset $offset is out of bounds for queue of size $size")
    return get(size - 1 - offset)
}
