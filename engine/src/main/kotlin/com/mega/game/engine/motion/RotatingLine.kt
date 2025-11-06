package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.shapes.GameLine

class RotatingLine(
    origin: Vector2, radius: Float, var speed: Float, var degreesOnReset: Float = 0f
) : IMotion {

    val line = GameLine()
    var degrees = degreesOnReset

    var scaleX: Float
        get() = line.scaleX
        set(value) {
            line.scaleX = value
        }
    var scaleY: Float
        get() = line.scaleY
        set(value) {
            line.scaleY = value
        }

    private val out1 = Vector2()
    private val out2 = Vector2()

    init {
        val endPoint = origin.cpy().add(radius, 0f)
        set(origin.cpy(), endPoint)
        line.rotation = degrees
    }

    override fun getMotionValue(out: Vector2) = getEndPoint(out)

    override fun update(delta: Float) {
        degrees += speed * delta
        line.rotation = degrees
    }

    override fun reset() {
        degrees = degreesOnReset
        line.rotation = degrees
    }

    fun set(origin: Vector2, endPoint: Vector2) {
        setOrigin(origin)
        setStartPoint(origin)
        setEndPoint(endPoint)
    }

    fun getOrigin(out: Vector2): Vector2 = out.set(line.originX, line.originY)

    fun setOrigin(origin: Vector2) = setOrigin(origin.x, origin.y)

    fun setOrigin(x: Float, y: Float) {
        line.setOrigin(x, y)
    }

    fun getStartPoint(out: Vector2): Vector2 {
        line.calculateWorldPoints(out1, out2)
        return out.set(out1)
    }

    fun setStartPoint(startPoint: Vector2) = setStartPoint(startPoint.x, startPoint.y)

    fun setStartPoint(x: Float, y: Float) {
        line.setFirstLocalPoint(x, y)
    }

    fun getEndPoint(out: Vector2): Vector2 {
        line.calculateWorldPoints(out1, out2)
        return out.set(out2)
    }

    fun setEndPoint(endPoint: Vector2) = setEndPoint(endPoint.x, endPoint.y)

    fun setEndPoint(x: Float, y: Float) {
        line.setSecondLocalPoint(x, y)
    }

    fun getScaledPosition(scalar: Float, out: Vector2): Vector2 {
        val endPoint = getEndPoint(out1)
        val x = line.originX + (endPoint.x - line.originX) * scalar
        val y = line.originY + (endPoint.y - line.originY) * scalar
        return out.set(x, y)
    }

    fun translate(deltaX: Float, deltaY: Float) {
        val x = line.originX + deltaX
        val y = line.originY + deltaY
        setOrigin(x, y)
    }

    override fun toString() =
        "RotatingLine[line=$line, degrees=$degrees, origin=${getOrigin(out1)}, endPoint=${getEndPoint(out2)}, " +
            "scaleX=$scaleX, scaleY=$scaleY]"
}
