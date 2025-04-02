package com.mega.game.engine.state

import com.badlogic.gdx.utils.Array
import kotlin.enums.EnumEntries

class EnumStateMachineBuilder<E : Enum<E>>() {

    companion object {

        inline fun <reified E : Enum<E>> create(fill: Boolean = true): EnumStateMachineBuilder<E> {
            val entries = E::class.java.enumConstants
            val builder = EnumStateMachineBuilder<E>()
            if (fill) entries.forEach { builder.state(it) }
            return builder
        }
    }

    private val builder = StateMachineBuilder<E>()

    fun load(entries: EnumEntries<E>): EnumStateMachineBuilder<E> {
        entries.forEach { builder.state(it.name, it) }
        return this
    }

    fun initialState(e: E): EnumStateMachineBuilder<E> {
        builder.initialState(e.name)
        return this
    }

    fun state(e: E): EnumStateMachineBuilder<E> {
        builder.state(e.name, e)
        return this
    }

    fun transition(fromState: E, toState: E, condition: (Array<Any?>) -> Boolean): EnumStateMachineBuilder<E> {
        builder.transition(fromState.name, toState.name, condition)
        return this
    }

    fun setOnChangeState(onChangeState: ((E, E) -> Unit)? = null): EnumStateMachineBuilder<E> {
        builder.setOnChangeState(onChangeState)
        return this
    }

    fun setTriggerChangeWhenSameElement(triggerChangeWhenSameElement: Boolean): EnumStateMachineBuilder<E> {
        builder.setTriggerChangeWhenSameElement(triggerChangeWhenSameElement)
        return this
    }

    fun setCallOnChangeOnReset(callOnChangeOnReset: Boolean): EnumStateMachineBuilder<E> {
        builder.setCallOnChangeOnReset(callOnChangeOnReset)
        return this
    }

    fun build() = builder.build()
}
