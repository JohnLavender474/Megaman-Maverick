package com.mega.game.engine.common.objects

import java.util.*

class InsertionOrderPriorityQueue<T: Comparable<T>> : Queue<T>, MutableCollection<T> {

    private val queue = PriorityQueue<GamePair<T, Long>>(object : Comparator<GamePair<T, Long>> {
        override fun compare(o1: GamePair<T, Long>, o2: GamePair<T, Long>): Int {
            val (element1, index1) = o1
            val (element2, index2) = o2

            val comparison = element1.compareTo(element2)
            if (comparison != 0) return comparison

            return index1.compareTo(index2)
        }
    })
    private var index = 0L

    override val size = queue.size

    override fun add(e: T): Boolean {
        val entry = e pairTo index

        if (queue.add(entry)) {
            index++
            return true
        }

        return false
    }

    override fun offer(e: T): Boolean {
        val entry = e pairTo index

        if (queue.offer(entry)) {
            index++
            return true
        }

        return false
    }

    override fun remove(): T = queue.remove().first

    override fun poll(): T? = queue.poll()?.first

    override fun element(): T = queue.element().first

    override fun peek(): T? = queue.peek()?.first

    override fun addAll(elements: Collection<T>): Boolean {
        elements.forEach { element -> add(element) }
        return true
    }

    override fun clear() = queue.clear()

    override fun iterator() = InsertionOrderPriorityQueueIterator(queue.iterator())

    override fun remove(element: T): Boolean {
        val iter = iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            if (next == element) {
                iter.remove()
                return true
            }
        }
        return false
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        elements.forEach { remove(it) }
        return true
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        var removed = false

        val iter = iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            if (!elements.contains(next)) {
                iter.remove()
                removed = true
            }
        }

        return removed
    }

    override fun contains(element: T): Boolean = queue.any { it.first == element }

    override fun containsAll(elements: Collection<T>) = elements.all { element -> contains(element) }

    override fun isEmpty() = queue.isEmpty()
}

class InsertionOrderPriorityQueueIterator<T>(iterator: MutableIterator<GamePair<T, Long>>) :
    MutableIteratorWrapper<GamePair<T, Long>, T>(iterator, { pair -> pair.first })




