package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.ObjectMap
import java.util.function.BiFunction


fun <K, V> ObjectMap<K, V>.computeValues(function: (K, V) -> V) {
    val keys = keys().toArray()
    keys.forEach { key -> put(key, function(key, get(key))) }
}


fun <K, V> ObjectMap<K, V>.computeValues(function: BiFunction<K, V, V>) =
    computeValues { key, value -> function.apply(key, value) }