package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.primaryConstructor

object ObjectPools {

    private class ObjectPool<T>(supplier: () -> T, onFetch: ((T) -> Unit)? = null) {

        private val pool = Pool<T>(supplier, onFetch = onFetch)
        private val reclaimables = Array<T>()

        fun fetch(): T {
            val value = pool.fetch()
            reclaimables.add(value)
            return value
        }

        fun reclaim() {
            reclaimables.forEach { pool.free(it) }
            reclaimables.clear()
        }
    }

    private val pools = OrderedMap<KClass<*>, ObjectPool<*>>()

    init {
        putPool(Vector2::class, onFetch = { it.setZero() })
        putPool(GameRectangle::class, onFetch = { it.set(0f, 0f, 0f, 0f) })
        putPool(Rectangle::class, onFetch = { it.set(0f, 0f, 0f, 0f) })
        putPool(GameCircle::class, onFetch = { it.setRadius(0f).setPosition(0f, 0f) })
        putPool(GameLine::class, onFetch = { it.reset() })
        putPool(BoundingBox::class)
    }

    fun <T : Any> putPool(
        clazz: KClass<T>,
        onFetch: ((T) -> Unit)? = null,
        supplier: () -> T = {
            clazz.primaryConstructor?.call()
                ?: throw IllegalArgumentException("No no-args constructor found for class: ${clazz.simpleName}")
        }
    ) {
        pools.put(clazz, ObjectPool<T>(supplier, onFetch))
    }

    fun <T: Any> get(clazz: KClass<T>) = clazz.cast(pools[clazz]!!.fetch())

    fun reclaim() = pools.values().forEach { it.reclaim() }
}
