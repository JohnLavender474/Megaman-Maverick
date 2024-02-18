package com.megaman.maverick.game.utils

import com.engine.common.GameLogger
import com.engine.common.extensions.objectSetOf
import com.engine.events.Event
import com.engine.events.IEventListener
import com.megaman.maverick.game.events.EventType

object TestObject : IEventListener {

    const val TAG = "TestObject"

    override val eventKeyMask = objectSetOf<Any>(EventType.END_ROOM_TRANS)

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "event: $event")
    }

}