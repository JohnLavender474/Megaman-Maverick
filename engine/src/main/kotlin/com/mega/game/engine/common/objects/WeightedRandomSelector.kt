package com.mega.game.engine.common.objects

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.OrderedMap


class WeightedRandomSelector<T>(
    vararg items: GamePair<T, Float>
) {

    private val items: OrderedMap<T, Float> = OrderedMap<T, Float>()
    private var weightSum = 0f

    init {
        items.forEach { putItem(it.first, it.second) }
    }


    fun putItem(item: T, weight: Float) {
        if (weight <= 0) throw IllegalArgumentException("Weight must be greater than 0")
        if (items.containsKey(item)) weightSum -= items[item]!!
        items.put(item, weight)
        weightSum += weight
    }


    fun removeItem(item: T) {
        val weight = items.remove(item)
        if (weight != null) weightSum -= weight
    }


    fun getRandomItem(): T {
        val r = MathUtils.random() * weightSum
        var cumWeight = 0f
        for (entry in items) {
            cumWeight += entry.value
            if (cumWeight >= r) return entry.key
        }
        throw IllegalStateException("No items to select from")
    }
}
