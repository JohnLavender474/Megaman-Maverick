package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.OrderedSet


class MutableOrderedSet<T> : OrderedSet<T>(), MutableCollection<T> {

    override val size: Int
        get() = super.size

    override fun retainAll(elements: Collection<T>): Boolean {
        var changed = false
        elements.forEach {
            if (!contains(it)) {
                remove(it)
                changed = true
            }
        }
        return changed
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var changed = false
        elements.forEach {
            if (contains(it)) {
                remove(it)
                changed = true
            }
        }
        return changed
    }

    override fun addAll(elements: Collection<T>): Boolean {
        var changed = false
        elements.forEach {
            if (!contains(it)) {
                add(it)
                changed = true
            }
        }
        return changed
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        elements.forEach { if (!contains(it)) return false }
        return true
    }
}
