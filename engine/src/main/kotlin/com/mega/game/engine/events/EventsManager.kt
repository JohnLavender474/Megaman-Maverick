package com.mega.game.engine.events

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.mega.game.engine.common.objects.SimpleQueueSet
import java.util.*


class EventsManager : Runnable {

    companion object {
        const val TAG = "EventsManager"
    }

    internal val listeners = OrderedSet<IEventListener>()
    internal val listenersToAdd = SimpleQueueSet<IEventListener>()
    internal val listenersToRemove = SimpleQueueSet<IEventListener>()

    internal val events = OrderedMap<Any, Array<Event>>()
    internal val eventsToAdd = LinkedList<Event>()

    internal var running = false
        private set

    private var setToClearListeners = false


    fun submitEvent(event: Event) {
        if (running) eventsToAdd.add(event) else submitEventNow(event)
    }

    private fun submitEventNow(event: Event) {
        val eventKey = event.key
        events.putIfAbsentAndGet(eventKey, Array()).add(event)
    }


    fun isListener(listener: IEventListener) = listeners.contains(listener)


    fun isQueuedToBeAdded(listener: IEventListener) = listenersToAdd.contains(listener)


    fun isQueuedToBeRemoved(listener: IEventListener) = listenersToRemove.contains(listener)


    fun addListener(listener: IEventListener) =
        if (running) listenersToAdd.add(listener) else addListenerNow(listener)

    private fun addListenerNow(listener: IEventListener) = listeners.add(listener)


    fun removeListener(listener: IEventListener) =
        if (running) listenersToRemove.add(listener) else removeListenerNow(listener)

    private fun removeListenerNow(listener: IEventListener): Boolean = listeners.remove(listener)


    fun clearListeners() = if (running) setToClearListeners = true else clearListenersNow()

    private fun clearListenersNow() = listeners.clear()


    override fun run() {
        running = true

        while (!eventsToAdd.isEmpty()) submitEventNow(eventsToAdd.poll())
        while (!listenersToAdd.isEmpty()) addListenerNow(listenersToAdd.remove())
        while (!listenersToRemove.isEmpty()) removeListenerNow(listenersToRemove.remove())
        if (setToClearListeners) {
            clearListenersNow()
            setToClearListeners = false
        }

        listeners.forEach { listener ->
            val eventKeyMask = listener.eventKeyMask
            events.forEach {
                val eventKey = it.key
                val eventsArray = it.value
                if (eventKeyMask.contains(eventKey)) eventsArray.forEach { event -> listener.onEvent(event) }
            }
        }
        events.clear()
        running = false
    }
}
