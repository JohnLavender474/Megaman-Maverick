package com.mega.game.engine.cullables

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import java.util.function.Predicate


class CullableOnEvent(
    private val cullOnEvent: (Event) -> Boolean,
    override val eventKeyMask: ObjectSet<Any> = ObjectSet()
) : ICullable, IEventListener {

    companion object {
        const val TAG = "CullableOnEvent"
    }

    private var cull: Boolean = false


    constructor(
        cullOnEvent: Predicate<Event>,
        eventKeyMask: ObjectSet<Any> = ObjectSet()
    ) : this(cullOnEvent::test, eventKeyMask)

    override fun shouldBeCulled(delta: Float) = cull

    override fun onEvent(event: Event) {
        if (!cull && cullOnEvent(event)) cull = true
    }

    override fun reset() {
        cull = false
    }

    override fun toString() = "CullableOnEvent"
}
