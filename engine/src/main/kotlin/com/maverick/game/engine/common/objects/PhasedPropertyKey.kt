package com.maverick.game.engine.common.objects

import com.maverick.game.engine.common.enums.ProcessState

class PhasedPropertyKey<T : Any>(val name: String) {

    private val keys = Array(ProcessState.entries.size) { PropertyKey<T>("${name}_${ProcessState.entries[it].name}") }

    operator fun get(state: ProcessState) = keys[state.ordinal]

    override fun toString() = name
}
