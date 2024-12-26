package com.mega.game.engine.common.objects

import com.badlogic.gdx.math.Vector2

data class IntPair(var x: Int, var y: Int) {

    fun set(x: Int, y: Int): IntPair {
        this.x = x
        this.y = y
        return this
    }

    operator fun plus(value: Int) = IntPair(x + value, y + value)

    operator fun minus(value: Int) = IntPair(x - value, y - value)

    operator fun times(value: Int) = IntPair(x * value, y * value)

    operator fun div(value: Int) = IntPair(x / value, y / value)

    operator fun plus(other: IntPair): IntPair = IntPair(x + other.x, y + other.y)

    operator fun minus(other: IntPair): IntPair = IntPair(x - other.x, y - other.y)

    operator fun times(other: IntPair): IntPair = IntPair(x * other.x, y * other.y)

    operator fun div(other: IntPair): IntPair = IntPair(x / other.x, y / other.y)

    fun toVector2(out: Vector2): Vector2 = out.set(x.toFloat(), y.toFloat())
}

infix fun Int.pairTo(that: Int): IntPair = IntPair(this, that)
