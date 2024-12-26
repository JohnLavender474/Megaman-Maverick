package com.mega.game.engine.points

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.components.IGameComponent
import java.util.function.Consumer

class PointsComponent(
    val pointsMap: ObjectMap<Any, Points> = ObjectMap(),
    val pointsListeners: ObjectMap<Any, (Points) -> Unit> = ObjectMap()
) : IGameComponent {

    constructor(vararg _points: GamePair<Any, Points>) : this(_points.asIterable())

    constructor(_points: Iterable<GamePair<Any, Points>>) : this(ObjectMap<Any, Points>().apply {
        _points.forEach {
            put(
                it.first,
                it.second
            )
        }
    })

    fun getPoints(key: Any): Points = pointsMap[key]

    fun putPoints(key: Any, points: Points): Points? = pointsMap.put(key, points)

    fun putPoints(key: Any, min: Int, max: Int, current: Int): Points? =
        putPoints(key, Points(min, max, current))

    fun putPoints(key: Any, value: Int): Points? = putPoints(key, Points(0, value, value))

    fun removePoints(key: Any): Points? = pointsMap.remove(key)

    fun putListener(key: Any, listener: (Points) -> Unit) = pointsListeners.put(key, listener)

    fun putListener(key: Any, listener: Consumer<Points>) = putListener(key, listener::accept)

    fun removeListener(key: Any) = pointsListeners.remove(key)
}

class PointsComponentBuilder(
    private var defaultMin: Int = Int.MIN_VALUE,
    private var defaultMax: Int = Int.MAX_VALUE,
    private var defaultCurrent: Int = 0
) {

    private val points = ObjectMap<Any, Points>()
    private val listeners = ObjectMap<Any, (Points) -> Unit>()
    private var currentKey: Any? = null

    fun key(key: Any): PointsComponentBuilder {
        points.put(key, Points(defaultMin, defaultMax, defaultCurrent))
        currentKey = key
        return this
    }

    fun min(min: Int): PointsComponentBuilder {
        points[currentKey!!].min = min
        return this
    }

    fun max(max: Int): PointsComponentBuilder {
        points[currentKey!!].max = max
        return this
    }

    fun current(current: Int): PointsComponentBuilder {
        points[currentKey!!].set(current)
        return this
    }

    fun listener(listener: (Points) -> Unit): PointsComponentBuilder {
        listeners.put(currentKey!!, listener)
        return this
    }

    fun build() = PointsComponent(points, listeners)
}
