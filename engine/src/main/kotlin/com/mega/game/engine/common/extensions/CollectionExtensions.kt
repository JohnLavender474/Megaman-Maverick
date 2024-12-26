package com.mega.game.engine.common.extensions

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.objects.ImmutableCollection
import java.util.function.Predicate

fun <T> Collection<T>.toImmutableCollection() = ImmutableCollection(this)

inline fun <reified R> Collection<*>.filterType(predicate: (R) -> Boolean): Array<R> {
    val array = Array<R>()
    forEach {
        if (it is R && predicate(it)) {
            array.add(it)
        }
    }
    return array
}

inline fun <reified R> Collection<*>.filterTyle(predicate: Predicate<R>) = filterType<R> { predicate.test(it) }

fun <T> Collection<T>.toGdxArray(): Array<T> {
    val array = Array<T>()
    forEach { array.add(it) }
    return array
}