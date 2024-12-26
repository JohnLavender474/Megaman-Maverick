package com.mega.game.engine.common.extensions

import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet

fun <T> Array<T>.toObjectSet(): ObjectSet<T> {
    val set = ObjectSet<T>()
    forEach { set.add(it) }
    return set
}

fun <T> com.badlogic.gdx.utils.Array<T>.toObjectSet(): ObjectSet<T> {
    val set = ObjectSet<T>()
    forEach { set.add(it) }
    return set
}

fun <T> Array<T>.toOrderedSet(): OrderedSet<T> {
    val set = OrderedSet<T>()
    forEach { set.add(it) }
    return set
}

fun <T> com.badlogic.gdx.utils.Array<T>.toOrderedSet(): OrderedSet<T> {
    val set = OrderedSet<T>()
    forEach { set.add(it) }
    return set
}

fun <T> objectSetOf(vararg elements: T): ObjectSet<T> {
    val set = ObjectSet<T>()
    elements.forEach { set.add(it) }
    return set
}

fun <T> orderedSetOf(vararg elements: T): OrderedSet<T> {
    val set = OrderedSet<T>()
    elements.forEach { set.add(it) }
    return set
}

fun <T> ObjectSet<T>.addAndReturn(element: T): T {
    add(element)
    return element
}

fun <T> ObjectSet<T>.addAllAndReturn(iterable: Iterable<T>): ObjectSet<T> {
    iterable.forEach { add(it) }
    return this
}

fun <T> ObjectSet<T>.addAllAndReturn(vararg elements: T): ObjectSet<T> {
    elements.forEach { add(it) }
    return this
}
