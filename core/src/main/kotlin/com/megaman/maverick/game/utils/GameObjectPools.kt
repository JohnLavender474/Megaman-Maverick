package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.IntPair
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.shapes.GamePolygon
import com.maverick.game.engine.common.shapes.GameRectangle
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

object GameObjectPools {

    const val TAG = "GameObjectPools"

    const val SCRATCH_LIFETIME_FRAMES = 3

    var debugChecks = false

    class GameObjectPool<T : Any>(
        private val supplier: () -> T,
        private val onFetch: (T) -> Unit,
        private val poison: (T) -> Unit,
        private val lifetimeFrames: Int = SCRATCH_LIFETIME_FRAMES
    ) {

        private val free = Array<T>()
        private val buckets = Queue<Array<T>>()

        init {
            buckets.addLast(Array())
        }

        fun fetch(): T {
            val value = if (free.isEmpty) supplier() else free.pop()
            onFetch(value)
            buckets.last().add(value)
            return value
        }

        fun performNextReclaim() {
            if (buckets.size < lifetimeFrames) {
                buckets.addLast(Array())
                return
            }

            val bucket = buckets.removeFirst()
            for (i in 0 until bucket.size) {
                val value = bucket[i]
                if (debugChecks) poison(value) else free.add(value)
            }
            bucket.clear()
            buckets.addLast(bucket)
        }
    }

    private val pools = OrderedMap<KClass<*>, GameObjectPool<*>>()

    init {
        put(
            GameRectangle::class,
            supplier = { GameRectangle() },
            onFetch = { it.set(0f, 0f, 0f, 0f) },
            poison = { it.set(Float.NaN, Float.NaN, Float.NaN, Float.NaN) }
        )
        put(
            GameCircle::class,
            supplier = { GameCircle() },
            onFetch = { it.setRadius(0f).setPosition(0f, 0f) },
            poison = { it.setRadius(Float.NaN).setPosition(Float.NaN, Float.NaN) }
        )
        put(
            GameLine::class,
            supplier = { GameLine() },
            onFetch = { it.reset() },
            poison = { it.set(Float.NaN, Float.NaN, Float.NaN, Float.NaN) }
        )
        put(
            GamePolygon::class,
            supplier = { GamePolygon() },
            onFetch = { it.reset() },
            poison = { it.setPosition(Float.NaN, Float.NaN) }
        )
        put(
            Vector2::class,
            supplier = { Vector2() },
            onFetch = { it.setZero() },
            poison = { it.set(Float.NaN, Float.NaN) }
        )
        put(
            Vector3::class,
            supplier = { Vector3() },
            onFetch = { it.setZero() },
            poison = { it.set(Float.NaN, Float.NaN, Float.NaN) }
        )
        put(
            Rectangle::class,
            supplier = { Rectangle() },
            onFetch = { it.set(0f, 0f, 0f, 0f) },
            poison = { it.set(Float.NaN, Float.NaN, Float.NaN, Float.NaN) }
        )
        put(
            Polygon::class,
            supplier = { Polygon() },
            onFetch = { it.vertices = floatArrayOf() },
            poison = { it.setPosition(Float.NaN, Float.NaN) }
        )
        put(
            BoundingBox::class,
            supplier = { BoundingBox() },
            onFetch = { it.min.setZero(); it.max.setZero(); it.update() },
            poison = {
                it.min.set(Float.NaN, Float.NaN, Float.NaN)
                it.max.set(Float.NaN, Float.NaN, Float.NaN)
                it.update()
            }
        )
        put(
            IntPair::class,
            supplier = { IntPair(0, 0) },
            onFetch = { it.set(0, 0) },
            poison = { it.set(Int.MIN_VALUE, Int.MIN_VALUE) }
        )
    }

    private fun <T : Any> put(
        clazz: KClass<T>,
        supplier: () -> T,
        onFetch: (T) -> Unit,
        poison: (T) -> Unit
    ) {
        GameLogger.debug(TAG, "put(): clazz=${clazz.simpleName}")
        pools.put(clazz, GameObjectPool(supplier, onFetch, poison))
    }

    fun <T : Any> fetch(clazz: KClass<T>): T {
        val pool = pools[clazz] ?: throw IllegalArgumentException(
            "No pool for ${clazz.simpleName}; the pooled types are ${pools.keys().joinToString { it.simpleName!! }}"
        )
        return clazz.cast(pool.fetch())
    }

    fun performNextReclaim() {
        for (pool in pools.values()) pool.performNextReclaim()
    }
}
