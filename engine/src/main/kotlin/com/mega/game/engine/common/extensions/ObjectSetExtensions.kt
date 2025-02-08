package com.mega.game.engine.common.extensions

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import kotlin.random.Random

fun <T> ObjectSet<T>.removeIf(predicate: (T) -> Boolean): ObjectSet<T> {
    val iter = iterator()
    while (iter.hasNext) {
        val value = iter.next()
        if (predicate.invoke(value)) iter.remove()
    }
    return this
}

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

fun <T> OrderedSet<T>.random(): T {
    val randomIndex = MathUtils.random(0, size - 1)

    // TODO: OrderedSet doesn't allow fetching elements by index without removing,
    //   so remove the elemnt and then add it back to the set
    val item = removeIndex(randomIndex)
    add(item)

    return item
}

fun <T> OrderedSet<T>.superRandom(): T? {
    if (size == 0) return null

    val random = Random(System.currentTimeMillis()).nextInt(0, size)

    val item = removeIndex(random)
    add(item)

    return item
}

