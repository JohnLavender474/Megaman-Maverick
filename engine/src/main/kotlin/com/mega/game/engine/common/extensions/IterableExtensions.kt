package com.mega.game.engine.common.extensions

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet

fun <T> Iterable<T>.toGdxArray(): Array<T> {
    val array = Array<T>()
    this.forEach { array.add(it) }
    return array
}

fun <T> Iterable<T>.toObjectSet(): ObjectSet<T> {
    val set = ObjectSet<T>()
    this.forEach { set.add(it) }
    return set
}

fun <T> Iterable<T>.toOrderedSet(): OrderedSet<T> {
    val set = OrderedSet<T>()
    this.forEach { set.add(it) }
    return set
}