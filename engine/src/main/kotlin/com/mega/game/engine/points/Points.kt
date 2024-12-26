package com.mega.game.engine.points


class Points(var min: Int, var max: Int, current: Int) {

    var current = current
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
