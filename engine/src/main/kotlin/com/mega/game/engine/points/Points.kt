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

    fun set(points: Int): Int {
        current = points
        if (current < min) current = min
        if (current > max) current = max
        return current
    }

    fun translate(delta: Int): Boolean {
        val old = current
        val new = set(current + delta)
        return old != new
    }

    fun setToMax() = set(max)

    fun setToMin() = set(min)

    fun isMin() = current == min

    fun isMax() = current == max

    override fun toString() = "Points{current=$current, min=$min, max=$max}"
}
