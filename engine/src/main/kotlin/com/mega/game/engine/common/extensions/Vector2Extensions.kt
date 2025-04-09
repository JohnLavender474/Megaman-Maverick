package com.mega.game.engine.common.extensions

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.objects.IntPair

fun Vector2.rotateAroundOrigin(degrees: Float, originX: Float, originY: Float): Vector2 {
    val radians = degrees * MathUtils.degreesToRadians
    val newX = (x - originX) * MathUtils.cos(radians) - (y - originY) * MathUtils.sin(radians) + originX
    val newY = (x - originX) * MathUtils.sin(radians) + (y - originY) * MathUtils.cos(radians) + originY
    return set(newX, newY)
}

fun Vector2.toIntPair(out: IntPair): IntPair = out.set(x.toInt(), y.toInt())

fun Vector2.swapped(): Vector2 = set(y, x)

fun Vector2.flipped(): Vector2 = set(-x, -y)

fun Vector2.toVector3(out: Vector3, z: Float = 0f): Vector3 = out.set(x, y, z)

fun Vector2.set(value: Float): Vector2 = set(value, value)

fun Vector2.coerceX(min: Float, max: Float): Vector2 {
    x = x.coerceIn(min, max)
    return this
}

fun Vector2.coerceY(min: Float, max: Float): Vector2 {
    y = y.coerceIn(min, max)
    return this
}

fun Vector2.coerce(min: Float, max: Float): Vector2 {
    x = x.coerceIn(min, max)
    y = y.coerceIn(min, max)
    return this
}

fun Vector2.coerce(min: Vector2, max: Vector2): Vector2 {
    x = x.coerceIn(min.x, max.x)
    y = y.coerceIn(min.y, max.y)
    return this
}

fun Vector2.setX(x: Float): Vector2 = set(x, y)

fun Vector2.setY(y: Float): Vector2 = set(x, y)

fun Vector2.add(length: Float, direction: Direction): Vector2 {
    when (direction) {
        Direction.UP -> y += length
        Direction.DOWN -> y -= length
        Direction.LEFT -> x -= length
        Direction.RIGHT -> x += length
    }
    return this
}

fun Vector2.add(delta: Float): Vector2 = add(delta, delta)

fun randomVector2(min: Float, max: Float, out: Vector2) = out.set(getRandom(min, max), getRandom(min, max))

fun randomVector2(minX: Float, maxX: Float, minY: Float, maxY: Float, out: Vector2) =
    out.set(getRandom(minX, maxX), getRandom(minY, maxY))

fun Vector2.setToDirection(direction: Direction): Vector2 = when (direction) {
    Direction.UP -> set(0f, 1f)
    Direction.DOWN -> set(0f, -1f)
    Direction.LEFT -> set(-1f, 0f)
    Direction.RIGHT -> set(1f, 0f)
}
