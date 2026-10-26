package com.maverick.game.engine.events

import com.badlogic.gdx.utils.Array

class Signal<T> {

    private val listeners = Array<(T) -> Unit>()
    private val pendingConnects = Array<(T) -> Unit>()
    private val pendingDisconnects = Array<(T) -> Unit>()
    private var emitting = false

    fun connect(listener: (T) -> Unit) {
        if (emitting) pendingConnects.add(listener) else listeners.add(listener)
    }

    fun disconnect(listener: (T) -> Unit) {
        if (emitting) pendingDisconnects.add(listener) else listeners.removeValue(listener, true)
    }

    fun emit(value: T) {
        if (emitting) throw IllegalStateException("Signal emitted again from one of its own listeners")

        emitting = true
        try {
            for (i in 0 until listeners.size) listeners[i](value)
        } finally {
            emitting = false
            for (i in 0 until pendingDisconnects.size) listeners.removeValue(pendingDisconnects[i], true)
            pendingDisconnects.clear()
            listeners.addAll(pendingConnects)
            pendingConnects.clear()
        }
    }
}
