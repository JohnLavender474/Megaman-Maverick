package com.mega.game.engine.common.shapes

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.interfaces.IRotatable
import com.mega.game.engine.common.interfaces.IScalable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class GameLine : IGameShape2D, IScalable, IRotatable, IRotatableShape, Resettable {

    companion object {

        private var OVERLAP_EXTENSION: ((GameLine, IGameShape2D) -> Boolean)? = null

        fun setOverlapExtension(overlapExtension: (GameLine, IGameShape2D) -> Boolean) {
            OVERLAP_EXTENSION = overlapExtension
        }
    }

    private val position = Vector2()
    private val localPoint1 = Vector2()
    private val localPoint2 = Vector2()
    private val worldPoint1 = Vector2()
    private val worldPoint2 = Vector2()

    override var rotation = 0f
        set(value) {
            field = value
            dirty = true
        }

    override var originX = 0f
        set(value) {
            field = value
            dirty = true
        }

    override var originY = 0f
        set(value) {
            field = value
            dirty = true
        }

    override var scaleX = 1f
        set(value) {
            field = value
            dirty = true
            calculateScaledLength = true
        }

    override var scaleY = 1f
        set(value) {
            field = value
            dirty = true
            calculateScaledLength = true
        }

    override var drawingColor: Color = Color.RED
    override var drawingShapeType = ShapeType.Line

    private var dirty = true

    private var length = 0f
    private var calculateLength = true

    private var scaledLength = 0f
    private var calculateScaledLength = true

    private val reusableVec1 = Vector2()
    private val reusableVec2 = Vector2()
    private val reusableVec3 = Vector2()
    private val reusableVec4 = Vector2()
    private val reusableRect = Rectangle()

    constructor(x1: Float, y1: Float, x2: Float, y2: Float) {
        localPoint1.set(x1, y1)
        localPoint2.set(x2, y2)
    }

    constructor(line: GameLine) {
        localPoint1.set(line.localPoint1)
        localPoint2.set(line.localPoint2)
        scaleX = line.scaleX
        scaleY = line.scaleY
        rotation = line.rotation
        originX = line.originX
        originY = line.originY
    }

    constructor(point1: Vector2, point2: Vector2) : this(point1.x, point1.y, point2.x, point2.y)

    constructor() : this(0f, 0f, 0f, 0f)

    override fun getProps(out: Properties): Properties {
        out.putAll(
            "local_point_1_x" pairTo localPoint1.x,
            "local_point_1_y" pairTo localPoint1.y,
            "local_point_2_x" pairTo localPoint2.x,
            "local_point_2_y" pairTo localPoint2.y,
            "scale_x" pairTo scaleX,
            "scale_y" pairTo scaleY,
            "rotation" pairTo rotation,
            "origin_x" pairTo originX,
            "origin_y" pairTo originY
        )
        return out
    }

    override fun setWithProps(props: Properties): IGameShape2D {
        localPoint1.x = props.getOrDefault("local_point_1_x", localPoint1.x, Float::class)
        localPoint1.y = props.getOrDefault("local_point_1_y", localPoint1.y, Float::class)
        localPoint2.x = props.getOrDefault("local_point_2_x", localPoint2.x, Float::class)
        localPoint2.y = props.getOrDefault("local_point_2_y", localPoint2.y, Float::class)
        scaleX = props.getOrDefault("scale_x", scaleX, Float::class)
        scaleY = props.getOrDefault("scale_y", scaleX, Float::class)
        rotation = props.getOrDefault("rotation", rotation, Float::class)
        originX = props.getOrDefault("origin_x", originX, Float::class)
        originY = props.getOrDefault("origin_y", originY, Float::class)
        return this
    }

    fun getRawVertices(out: Array<Float>): Array<Float> {
        out.clear()
        out.addAll(localPoint1.x, localPoint1.y, localPoint2.x, localPoint2.y)
        return out
    }

    fun getTransformedVertices(out: Array<Float>): Array<Float> {
        calculateWorldPoints(reusableVec1, reusableVec2)
        out.clear()
        out.addAll(reusableVec1.x, reusableVec1.y, reusableVec2.x, reusableVec2.y)
        return out
    }

    fun set(line: GameLine): GameLine {
        setPosition(line.position)
        setOrigin(line.originX, line.originY)
        scaleX = line.scaleX
        scaleY = line.scaleY
        rotation = line.rotation
        return set(line.localPoint1, line.localPoint2)
    }

    fun set(point1: Vector2, point2: Vector2) = set(point1.x, point1.y, point2.x, point2.y)

    fun set(x1: Float, y1: Float, x2: Float, y2: Float): GameLine {
        localPoint1.x = x1
        localPoint1.y = y1
        localPoint2.x = x2
        localPoint2.y = y2
        dirty = true
        calculateLength = true
        return this
    }

    fun setOriginCenter(): GameLine {
        val center = getCenter(reusableVec1)
        originX = center.x
        originY = center.y
        return this
    }

    fun setToDirty(): GameLine {
        dirty = true
        return this
    }

    fun setToRecalculateLength(): GameLine {
        calculateLength = true
        return this
    }

    override fun rotate(rotation: Float, originX: Float, originY: Float) {
        setLocalPoints(localPoint1, localPoint2)
        setPosition(position)
        this.rotation = rotation
        this.originX = originX
        this.originY = originX
    }

    fun getLength(): Float {
        if (calculateLength) {
            calculateLength = false
            calculateWorldPoints(reusableVec1, reusableVec2)
            val x = worldPoint1.x - worldPoint2.x
            val y = worldPoint1.y - worldPoint2.y
            length = sqrt((x * x + y * y).toDouble()).toFloat()
        }
        return length
    }

    fun setFirstLocalPoint(point: Vector2) = setFirstLocalPoint(point.x, point.y)

    fun setFirstLocalPoint(x1: Float, y1: Float): GameLine {
        setLocalPoints(x1, y1, localPoint2.x, localPoint2.y)
        return this
    }

    fun setSecondLocalPoint(point: Vector2) = setSecondLocalPoint(point.x, point.y)

    fun setSecondLocalPoint(x2: Float, y2: Float): GameLine {
        setLocalPoints(localPoint1.x, localPoint1.y, x2, y2)
        return this
    }

    fun setLocalPoints(x1: Float, y1: Float, x2: Float, y2: Float): GameLine {
        localPoint1.set(x1, y1)
        localPoint2.set(x2, y2)
        dirty = true
        calculateLength = true
        return this
    }

    fun setLocalPoints(point1: Vector2, point2: Vector2) = setLocalPoints(point1.x, point1.y, point2.x, point2.y)

    fun getFirstLocalPoint(out: Vector2): Vector2 = out.set(localPoint1)

    fun getSecondLocalPoint(out: Vector2): Vector2 = out.set(localPoint2)

    fun calculateWorldPoints(out1: Vector2, out2: Vector2): GameLine {
        if (dirty) {
            dirty = false

            val cos = MathUtils.cosDeg(rotation)
            val sin = MathUtils.sinDeg(rotation)

            var first = true
            forEachLocalPoint {
                var x = it.x - originX
                var y = it.y - originY

                x *= scaleX
                y *= scaleY

                if (rotation != 0f) {
                    val oldX = x
                    x = cos * x - sin * y
                    y = sin * oldX + cos * y
                }

                val worldPoint = if (first) worldPoint1 else worldPoint2
                first = false

                worldPoint.x = position.x + x + originX
                worldPoint.y = position.y + y + originY
            }
        }

        out1.set(worldPoint1)
        out2.set(worldPoint2)

        return this
    }

    fun forEachLocalPoint(action: (Vector2) -> Unit): GameLine {
        action.invoke(localPoint1)
        action.invoke(localPoint2)
        return this
    }

    fun worldDistanceFromPoint(point: Vector2, segment: Boolean = true): Float {
        calculateWorldPoints(reusableVec1, reusableVec2)
        return if (segment) Intersector.distanceSegmentPoint(reusableVec1, reusableVec2, point)
        else Intersector.distanceLinePoint(
            reusableVec1.x,
            reusableVec1.y,
            reusableVec2.x,
            reusableVec2.y,
            point.x,
            point.y
        )
    }

    fun intersectionPoint(line: GameLine, out: Vector2): Vector2? {
        calculateWorldPoints(reusableVec1, reusableVec2)
        line.calculateWorldPoints(reusableVec3, reusableVec4)
        val intersection = reusableVec1
        return if (Intersector.intersectLines(
                reusableVec1,
                reusableVec2,
                reusableVec3,
                reusableVec4,
                intersection
            )
        ) out.set(intersection) else null
    }

    override fun contains(point: Vector2): Boolean {
        calculateWorldPoints(reusableVec1, reusableVec2)
        return Intersector.pointLineSide(reusableVec1, reusableVec2, point) == 0 &&
                point.x <= getMaxX() && point.x >= getX()
    }

    override fun contains(x: Float, y: Float) = contains(reusableVec3.set(x, y))

    override fun setCenter(centerX: Float, centerY: Float): GameLine {
        val currentCenter = getCenter(reusableVec1)
        val centerDeltaX = centerX - currentCenter.x
        val centerDeltaY = centerY - currentCenter.y

        if (centerDeltaX == 0f && centerDeltaY == 0f) return this

        position.x += centerDeltaX
        position.y += centerDeltaY

        dirty = true
        calculateLength = true

        return this
    }

    override fun setCenter(center: Vector2) = setCenter(center.x, center.y)

    override fun getCenter(out: Vector2): Vector2 {
        calculateWorldPoints(reusableVec1, reusableVec2)
        return out.set((reusableVec1.x + reusableVec2.x) / 2f, (reusableVec1.y + reusableVec2.y) / 2f)
    }

    fun getLocalCenter(out: Vector2): Vector2 =
        out.set((localPoint1.x + localPoint2.x) / 2f, (localPoint1.y + localPoint2.y) / 2f)

    override fun setX(x: Float): GameLine {
        if (position.x != x) {
            position.x = x
            dirty = true
        }
        return this
    }

    override fun setY(y: Float): GameLine {
        if (position.y != y) {
            position.y = y
            dirty = true
        }
        return this
    }

    override fun getX() = position.x

    override fun getY() = position.y

    override fun getMaxX(): Float {
        calculateWorldPoints(reusableVec1, reusableVec2)
        return max(reusableVec1.x, reusableVec2.x)
    }

    override fun getMaxY(): Float {
        calculateWorldPoints(reusableVec1, reusableVec2)
        return max(reusableVec1.y, reusableVec2.y)
    }

    override fun translate(translateX: Float, translateY: Float): GameLine {
        position.x += translateX
        position.y += translateY
        dirty = true
        return this
    }

    fun setOrigin(origin: Vector2) = setOrigin(origin.x, origin.y)

    fun setOrigin(originX: Float, originY: Float): GameLine {
        this.originX = originX
        this.originY = originY
        return this
    }

    override fun copy(): GameLine = GameLine(this)

    override fun overlaps(other: IGameShape2D): Boolean {
        calculateWorldPoints(reusableVec1, reusableVec2)

        return when (other) {
            is GameRectangle -> Intersector.intersectSegmentRectangle(
                reusableVec1,
                reusableVec2,
                other.get(reusableRect)
            )

            is GameCircle -> Intersector.intersectSegmentCircle(
                reusableVec1, reusableVec2, other.getCenter(reusableVec3), other.getRadius() * other.getRadius()
            )

            is GameLine -> {
                other.calculateWorldPoints(reusableVec3, reusableVec4)
                Intersector.intersectSegments(reusableVec1, reusableVec2, reusableVec3, reusableVec4, null)
            }

            is GamePolygon -> Intersector.intersectLinePolygon(reusableVec1, reusableVec2, other.libgdxPolygon)
            else -> OVERLAP_EXTENSION?.invoke(this, other) == true
        }
    }

    fun getMinsAndMaxes(min: Vector2, max: Vector2) {
        calculateWorldPoints(reusableVec1, reusableVec2)
        min.x = min(reusableVec1.x, reusableVec2.x)
        max.x = max(reusableVec1.x, reusableVec2.x)
        min.y = min(reusableVec1.y, reusableVec2.y)
        max.y = max(reusableVec1.y, reusableVec2.y)
    }

    fun getBoundingRectangle(out: Rectangle): Rectangle {
        getMinsAndMaxes(reusableVec1, reusableVec2)
        val minX = reusableVec1.x
        val minY = reusableVec1.y
        val maxX = reusableVec2.x
        val maxY = reusableVec2.y
        return out.set(minX, minY, maxX - minX, maxY - minY)
    }

    override fun getBoundingRectangle(out: GameRectangle): GameRectangle {
        getMinsAndMaxes(reusableVec1, reusableVec2)
        val minX = reusableVec1.x
        val minY = reusableVec1.y
        val maxX = reusableVec2.x
        val maxY = reusableVec2.y
        return out.set(minX, minY, maxX - minX, maxY - minY)
    }

    override fun draw(renderer: ShapeRenderer): GameLine {
        renderer.color = drawingColor
        renderer.set(drawingShapeType)
        calculateWorldPoints(reusableVec1, reusableVec2)
        renderer.line(reusableVec1, reusableVec2)
        return this
    }

    override fun toString(): String {
        calculateWorldPoints(reusableVec1, reusableVec2)
        return "GameLine[$reusableVec1, $reusableVec2]"
    }

    override fun reset() {
        position.setZero()
        localPoint1.setZero()
        localPoint2.setZero()
        originX = 0f
        originY = 0f
        scaleX = 1f
        scaleY = 1f
        rotation = 0f
    }
}
