package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GamePolygon
import com.mega.game.engine.common.shapes.GameRectangle
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

object GameObjectPools {

    const val TAG = "GameObjectPools"

    class GameObjectPool<T>(
        supplier: () -> T,
        onFetch: ((T) -> Unit)? = null,
        onFree: ((T) -> Unit)? = null,
        private val callsToWait: Int = DEFAULT_CALLS_TO_WAIT
    ) {

        companion object {
            const val DEFAULT_CALLS_TO_WAIT = 3
        }

        private val pool = Pool<T>(supplier, onFetch = onFetch, onFree = onFree)
        private val reclaimables = Queue<Array<T>>()

        private var temp = 0

        init {
            reclaimables.addLast(Array())
        }

        fun fetch(reclaim: Boolean = true): T {
            val value = pool.fetch()
            if (reclaim) reclaimables.last().add(value)
            return value
        }

        fun free(obj: T) = pool.free(obj)

        fun performNextReclaim() {
            if (reclaimables.size < callsToWait) {
                reclaimables.addLast(Array())
                return
            }

            val array = reclaimables.removeFirst()

            val iter = array.iterator()
            while (iter.hasNext()) {
                val value = iter.next()
                pool.free(value)
                iter.remove()
            }

            reclaimables.addLast(array)
        }

        fun forceReclaimAll() {
            val qIter = reclaimables.iterator()
            while (qIter.hasNext()) {
                val array = qIter.next()

                val arrIter = array.iterator()
                while (arrIter.hasNext()) {
                    val value = arrIter.next()
                    pool.free(value)
                    arrIter.remove()
                }

                qIter.remove()
            }

            temp = 0
        }
    }

    private val pools = OrderedMap<KClass<*>, GameObjectPool<*>>()

    init {
        put(GameRectangle::class, supplier = { GameRectangle() }, onFetch = { it.set(0f, 0f, 0f, 0f) })
        put(GameCircle::class, supplier = { GameCircle() }, onFetch = { it.setRadius(0f).setPosition(0f, 0f) })
        put(GameLine::class, supplier = { GameLine() }, onFetch = { it.reset() })
        put(GamePolygon::class, supplier = { GamePolygon() }, onFetch = { it.reset() })

        put(Vector2::class, supplier = { Vector2() }, onFetch = { it.setZero() })
        put(Vector3::class, supplier = { Vector3() }, onFetch = { it.setZero() })

        put(Rectangle::class, supplier = { Rectangle() }, onFetch = { it.set(0f, 0f, 0f, 0f) })
        put(Polygon::class, supplier = { Polygon() }, onFetch = { it.vertices = floatArrayOf() })

        put(BoundingBox::class, supplier = { BoundingBox() })

        put(IntPair::class, supplier = { IntPair(0, 0) }, onFetch = { it.set(0, 0) })
    }

    fun <T : Any> put(
        clazz: KClass<T>,
        supplier: () -> T,
        onFetchAndFree: (T) -> Unit,
        callsToWait: Int = GameObjectPool.DEFAULT_CALLS_TO_WAIT
    ) = put(clazz, supplier, onFetchAndFree, onFetchAndFree, callsToWait)

    fun <T : Any> put(
        clazz: KClass<T>,
        supplier: () -> T,
        onFetch: ((T) -> Unit)? = null,
        onFree: ((T) -> Unit)? = null,
        callsToWait: Int = GameObjectPool.DEFAULT_CALLS_TO_WAIT
    ) = put(clazz, GameObjectPool<T>(supplier, onFetch, onFree, callsToWait))

    fun <T : Any> put(clazz: KClass<T>, pool: GameObjectPool<T>) {
        GameLogger.debug(TAG, "put(): clazz=${clazz.simpleName}")
        pools.put(clazz, pool)
    }

    fun <T : Any> fetch(clazz: KClass<T>, reclaim: Boolean = true): T {
        val value = clazz.cast(pools[clazz]!!.fetch(reclaim))
        GameLogger.debug(TAG, "fetch(): clazz=${clazz.simpleName}, reclaim=$reclaim, value=$value")
        return value
    }

    fun <T : Any> free(obj: T): Boolean {
        if (!pools.containsKey(obj::class)) {
            GameLogger.error(TAG, "free(): no key for clazz=${obj::class.simpleName}")
            return false
        }

        GameLogger.debug(TAG, "free(): key=${obj::class.simpleName}, obj=$obj")

        val pool = pools[obj::class] as GameObjectPool<T>
        pool.free(obj)

        return true
    }

    fun performNextReclaim() {
        GameLogger.debug(TAG, "performNextReclaim()")
        pools.values().forEach { it.performNextReclaim() }
    }

    fun forceReclaimAll() {
        GameLogger.debug(TAG, "forceReclaimAll()")
        pools.values().forEach { it.forceReclaimAll() }
    }
}
