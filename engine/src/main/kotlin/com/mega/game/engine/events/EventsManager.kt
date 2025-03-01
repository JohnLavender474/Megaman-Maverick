package com.mega.game.engine.events

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.extensions.putIfAbsentAndGet
import com.mega.game.engine.common.objects.SimpleQueueSet

class EventsManager : Runnable {

    companion object {
        const val TAG = "EventsManager"
    }

    internal val listeners = OrderedSet<IEventListener>()
    internal val listenersToAdd = SimpleQueueSet<IEventListener>()
    internal val listenersToRemove = SimpleQueueSet<IEventListener>()

    internal val eventsMap = OrderedMap<Any, Array<Event>>()
    internal val eventsToAdd = Queue<Event>()

    internal var running = false
        private set

    private var setToClearListeners = false


    fun submitEvent(event: Event) {
        if (running) eventsToAdd.addLast(event) else submitEventNow(event)
    }

    private fun submitEventNow(event: Event) {
        val eventKey = event.key
        eventsMap.putIfAbsentAndGet(eventKey) { Array() }.add(event)
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

        while (!eventsToAdd.isEmpty) submitEventNow(eventsToAdd.removeFirst())
        while (!listenersToAdd.isEmpty()) addListenerNow(listenersToAdd.remove())
        while (!listenersToRemove.isEmpty()) removeListenerNow(listenersToRemove.remove())

        if (setToClearListeners) {
            clearListenersNow()

            setToClearListeners = false
        }

        listeners.forEach { listener ->
            listener.eventKeyMask.forEach { eventKey ->
                val events = eventsMap[eventKey]
                events?.forEach { event -> listener.onEvent(event) }
            }
        }

        eventsMap.clear()

        running = false
    }
}
