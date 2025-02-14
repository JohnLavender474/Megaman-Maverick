package com.mega.game.engine.common

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.*

object UtilMethods {

    private val gRect1 = GameRectangle()
    private val gRect2 = GameRectangle()
    private val rect1 = Rectangle()
    private val rect2 = Rectangle()
    private val rect3 = Rectangle()

    val random = Random(System.currentTimeMillis())

    fun allAreTrue(vararg conditions: Boolean) = conditions.all { it }

    fun anyIsTrue(vararg conditions: Boolean) = conditions.any { it }

    fun amountIsTrue(amount: Int, vararg conditions: Boolean) = conditions.count { it } == amount

    fun getRandomBool() = random.nextBoolean()

    fun getRandom(min: Int, max: Int) = random.nextInt(max + 1 - min) + min

    fun getRandom(min: Float, max: Float) = random.nextFloat() * (max - min) + min

    fun <T> getRandomValue(suppliers: Array<() -> T>): T {
        if (suppliers.isEmpty) throw IllegalArgumentException("No suppliers provided")
        return suppliers[random.nextInt(suppliers.size)]()
    }

    fun doIfRandomMatches(min: Int, max: Int, matches: Iterable<Int>, runOnMatch: Consumer<Int?>) {
        val r = getRandom(min, max)
        for (i in matches) if (r == i) {
            runOnMatch.accept(r)
            break
        }
    }

    fun <T> mask(o1: T, o2: T, p1: Predicate<T>, p2: Predicate<T>) =
        p1.test(o1) && p2.test(o2) || p2.test(o1) && p1.test(o2)

    fun <T> mask(o1: T, o2: T, p1: (T) -> Boolean, p2: (T) -> Boolean) =
        p1(o1) && p2(o2) || p2(o1) && p1(o2)

    fun getOverlapPushDirection(toBePushed: IGameShape2D, other: IGameShape2D, overlap: Rectangle = rect3): Direction? {
        val toBePushedBounds = toBePushed.getBoundingRectangle(gRect1).get(rect1)
        val otherBounds = other.getBoundingRectangle(gRect2).get(rect2)
        return if (Intersector.intersectRectangles(toBePushedBounds, otherBounds, overlap)) {
            if (overlap.width >= overlap.height) {
                if (toBePushedBounds.y > otherBounds.y) Direction.UP else Direction.DOWN
            } else if (toBePushedBounds.x > otherBounds.x) Direction.RIGHT else Direction.LEFT
        } else null
    }

    fun getSingleMostDirectionFromStartToTarget(start: Vector2, target: Vector2): Direction {
        val x = target.x - start.x
        val y = target.y - start.y
        return if (abs(x) > abs(y)) {
            if (x > 0) Direction.RIGHT else Direction.LEFT
        } else {
            if (y > 0) Direction.UP else Direction.DOWN
        }
    }

    fun getSlope(p1: Vector2, p2: Vector2) = (p1.y - p2.y) / (p1.x - p2.x)

    fun normalizedTrajectory(start: Vector2, end: Vector2, speed: Float, out: Vector2): Vector2 {
        var x = end.x - start.x
        var y = end.y - start.y
        val length = sqrt((x * x + y * y).toDouble()).toFloat()
        x /= length
        y /= length
        return out.set(x * speed, y * speed)
    }

    fun roundFloat(num: Float, decimals: Int): Float {
        val scale = 10.0.pow(decimals.toDouble()).toFloat()
        return (num * scale).roundToInt() / scale
    }

    fun roundVector2(vector2: Vector2, decimals: Int) {
        vector2.x = roundFloat(vector2.x, decimals)
        vector2.y = roundFloat(vector2.y, decimals)
    }

    fun interpolate(start: Vector2, target: Vector2, delta: Float, out: Vector2): Vector2 {
        out.x = interpolate(start.x, target.x, delta)
        out.y = interpolate(start.y, target.y, delta)
        return out
    }

    fun interpolate(start: Float, target: Float, delta: Float) = start - (start - target) * delta

    fun calculateAngleDegrees(origin: Vector2, target: Vector2): Float {
        val deltaX = target.x - origin.x
        val deltaY = target.y - origin.y
        val angleRadians = atan2(deltaY.toDouble(), deltaX.toDouble())
        return Math.toDegrees(angleRadians).toFloat()
    }
}
