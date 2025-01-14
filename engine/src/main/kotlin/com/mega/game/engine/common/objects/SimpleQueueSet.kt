package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.extensions.toObjectSet


class SimpleQueueSetIterator<T>(queueSet: SimpleQueueSet<T>) : MutableIterator<T> {

    private val queue = queueSet.queue
    private val set = queueSet.set
    private var index = 0

    override fun hasNext() = index < queue.size

    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        return queue[index++]
    }

    override fun remove() {
        if (index <= 0) throw IllegalStateException("Cannot remove an element before calling next()")
        if (index >= queue.size) throw IllegalStateException(
            "Cannot remove an element because index $index is out of bounds for iterator of size ${queue.size}"
        )
        val value = queue.removeIndex(--index)
        set.remove(value)
    }
}


class SimpleQueueSet<T> : MutableCollection<T>, java.util.Queue<T> {

    internal val queue = Queue<T>()
    internal val set = ObjectSet<T>()

    override val size get() = queue.size

    override fun clear() {
        queue.clear()
        set.clear()
    }

    override fun isEmpty() = queue.isEmpty

    override fun poll() = if (isEmpty()) null else queue.removeFirst().also { set.remove(it) }

    override fun peek() = queue.firstOrNull()

    override fun element(): T = queue.first()

    override fun offer(e: T) = add(e)

    override fun iterator() = SimpleQueueSetIterator(this)

    override fun remove() = poll() ?: throw NoSuchElementException("Queue is empty")

    override fun retainAll(elements: Collection<T>): Boolean {
        val elementSet = elements.toObjectSet()
        val originalSize = size
        iterator().apply { forEachRemaining { if (!elementSet.contains(it)) remove() } }
        return originalSize != size
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var removed = false
        elements.forEach { if (remove(it)) removed = true }
        return removed
    }

    override fun remove(element: T) = if (set.remove(element)) {
        queue.removeValue(element, true)
        true
    } else false

    override fun containsAll(elements: Collection<T>) = elements.all { set.contains(it) }

    override fun contains(element: T) = set.contains(element)

    override fun addAll(elements: Collection<T>): Boolean {
        var added = false
        elements.forEach { if (add(it)) added = true }
        return added
    }

    override fun add(element: T) = if (set.add(element)) {
        queue.addLast(element)
        true
    } else false
}
