package com.mega.game.engine.common.extensions

import com.badlogic.gdx.utils.Queue

fun <T> Queue<T>.has(element: T): Boolean {
    for (i in 0 until size) {
        if (get(i) == element) return true
    }
    return false
}
