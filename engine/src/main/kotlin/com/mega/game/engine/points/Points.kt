package com.mega.game.engine.points


class Points(min: Int, max: Int, current: Int) {

    var min: Int = min
        set(value) {
            field = value
            if (current < value) set(value)
        }

    var max: Int = max
        set(value) {
            field = value
            if (current > value) set(value)
        }

    var current: Int = current
        private set

    fun set(points: Int) {
        current = points
        if (current < min) current = min
        if (current > max) current = max
    }

    fun translate(delta: Int) = set(current + delta)

    fun setToMax() = set(max)

    fun setToMin() = set(min)

    fun isMin() = current == min

    fun isMax() = current == max
}
