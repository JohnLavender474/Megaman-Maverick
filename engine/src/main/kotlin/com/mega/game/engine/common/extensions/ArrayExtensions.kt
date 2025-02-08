package com.mega.game.engine.common.extensions

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FloatArray
import com.badlogic.gdx.utils.IntArray
import com.badlogic.gdx.utils.Predicate
import java.util.function.Consumer
import java.util.function.Function
import kotlin.random.Random

fun Array<Float>.toGdxFloatArray(out: FloatArray): FloatArray {
    forEach { out.add(it) }
    return out
}

fun <T> gdxArrayOf(vararg elements: T): Array<T> {
    val array = Array<T>()
    elements.forEach { array.add(it) }
    return array
}

fun <T> gdxFilledArrayOf(size: Int, value: T): Array<T> {
    val array = gdxArrayOf<T>()
    (0 until size).forEach { array.add(value) }
    return array
}

fun <T> Array<T>.fill(value: T): Array<T> {
    for (i in 0 until size) set(i, value)
    return this
}

fun <T> Array<T>.filter(predicate: (T) -> Boolean): Array<T> {
    val array = Array<T>()
    forEach { if (predicate(it)) array.add(it) }
    return array
}

fun <T> Array<T>.filter(predicate: Predicate<T>) = filter(predicate::evaluate)

fun <T, R> Array<T>.map(transform: (T) -> R): Array<R> {
    val array = Array<R>()
    forEach { array.add(transform(it)) }
    return array
}

fun <T, R> Array<T>.map(transform: Function<T, R>) = map(transform::apply)

fun <T> Array<T>.processAndFilter(process: (T) -> Unit, filter: (T) -> Boolean): Array<T> {
    val array = Array<T>()
    forEach {
        process(it)
        if (filter(it)) array.add(it)
    }
    return array
}

fun <T> Array<T>.processAndFilter(process: Consumer<T>, filter: Predicate<T>) =
    processAndFilter(process::accept, filter::evaluate)

fun <T> Array<T>.getRandomElements(amount: Int): Array<T> {
    require(amount >= 0) { "Number of indices must not be negative." }
    require(amount <= size) { "Number of indices must not exceed the size of the array." }

    if (amount == 0) return Array()

    val copy = Array(this)
    copy.shuffle()
    copy.truncate(amount)

    return copy
}

fun gdxFloatArrayOf(vararg elements: Float): FloatArray {
    val array = FloatArray()
    elements.forEach { array.add(it) }
    return array
}

fun gdxIntArrayOf(vararg elements: Int): IntArray {
    val array = IntArray()
    elements.forEach { array.add(it) }
    return array
}

fun <T> Array<T>.addAndReturn(element: T): Array<T> {
    add(element)
    return this
}

fun <T> Array<T>.addAllAndReturn(iterable: Iterable<T>): Array<T> {
    iterable.forEach { add(it) }
    return this
}

fun <T> Array<T>.addAllAndReturn(vararg elements: T): Array<T> {
    elements.forEach { add(it) }
    return this
}

fun <T> Array<T>.superRandom(): T? {
    if (size == 0) return null

    val random = Random(System.currentTimeMillis()).nextInt(0, size)
    return items[random]
}
