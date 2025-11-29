package com.mega.game.engine.common.shapes

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FloatArray
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.interfaces.IRectangle
import com.mega.game.engine.common.objects.*
import kotlin.math.*

open class GameRectangle() : IGameShape2D, IRectangle, IRotatableShape {

    companion object {

        private var OVERLAP_EXTENSION: ((GameRectangle, IGameShape2D) -> Boolean)? = null

        fun setOverlapExtension(overlapExtension: (GameRectangle, IGameShape2D) -> Boolean) {
            OVERLAP_EXTENSION = overlapExtension
        }

        fun calculateBoundsFromLines(lines: Array<GamePair<Vector2, Vector2>>, out: GameRectangle): GameRectangle {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY

            lines.forEach {
                val (point1, point2) = it
                minX = min(minX, min(point1.x, point2.x))
                minY = min(minY, min(point1.y, point2.y))
                maxX = max(maxX, max(point1.x, point2.x))
                maxY = max(maxY, max(point1.y, point2.y))
            }

            return out.set(minX, minY, abs(maxX - minX), abs(maxY - minY))
        }
    }

    override var drawingColor: Color = Color.RED
    override var drawingShapeType = ShapeType.Line

    internal val rectangle = Rectangle()

    private val linesArray = Array<GameLine>()
    private val linesPool = Pool<GameLine>(supplier = { GameLine() })

    private val pointsArray = Array<GamePair<Vector2, Vector2>>()
    private val pointsPool = Pool<GamePair<Vector2, Vector2>>(supplier = { GamePair(Vector2(), Vector2()) })

    private val polygonPool = Pool<GamePolygon>(supplier = { GamePolygon() })

    private val out1 = Vector2()
    private val out2 = Vector2()

    constructor(x: Number, y: Number, width: Number, height: Number) : this() {
        set(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
    }

    constructor(rect: Rectangle) : this() {
        set(rect)
    }

    constructor(rect: IRectangle) : this() {
        set(rect)
    }

    override fun getProps(out: Properties): Properties {
        out.putAll(
            "x" pairTo getX(),
            "y" pairTo getY(),
            "width" pairTo getWidth(),
            "height" pairTo getHeight(),
        )
        return out
    }

    override fun setWithProps(props: Properties): IGameShape2D {
        setX(props.getOrDefault("x", getX(), Float::class))
        setY(props.getOrDefault("y", getY(), Float::class))
        setWidth(props.getOrDefault("width", getWidth(), Float::class))
        setHeight(props.getOrDefault("height", getHeight(), Float::class))
        return this
    }

    fun get(out: Rectangle): Rectangle = out.set(rectangle)

    fun getAsLines(
        out: Array<GameLine>,
        topLine: GameLine,
        bottomLine: GameLine,
        leftLine: GameLine,
        rightLine: GameLine,
    ): Array<GameLine> {
        out.addAll(
            topLine.set(getTopLeftPoint(out1), getTopRightPoint(out2)),
            bottomLine.set(getBottomLeftPoint(out1), getBottomRightPoint(out2)),
            leftLine.set(getBottomLeftPoint(out1), getTopLeftPoint(out2)),
            rightLine.set(getBottomRightPoint(out1), getTopRightPoint(out2))
        )
        return out
    }

    fun getVertices(out: FloatArray): FloatArray {
        out.addAll(
            getX(),
            getY(),
            getX() + getWidth(),
            getY(),
            getX() + getWidth(),
            getY() + getHeight(),
            getX(),
            getY() + getHeight()
        )
        return out
    }

    override fun rotate(rotation: Float, originX: Float, originY: Float) {
        val line1 = linesPool.fetch()
        line1.reset()

        val line2 = linesPool.fetch()
        line2.reset()

        val line3 = linesPool.fetch()
        line3.reset()

        val line4 = linesPool.fetch()
        line4.reset()

        linesArray.clear()
        getAsLines(linesArray, line1, line2, line3, line4)

        pointsArray.forEach { pointsPool.free(it) }
        pointsArray.clear()

        val lineIter = linesArray.iterator()
        while (lineIter.hasNext()) {
            val line = lineIter.next()

            line.originX = originX
            line.originY = originY
            line.rotation = rotation

            val pair = pointsPool.fetch()
            line.calculateWorldPoints(pair.first, pair.second)
            pointsArray.add(pair)

            linesPool.free(line)
            lineIter.remove()
        }

        calculateBoundsFromLines(pointsArray, this)

        pointsArray.forEach { pointsPool.free(it) }
        pointsArray.clear()
    }

    override fun getSize(out: Vector2): Vector2 = out.set(getWidth(), getHeight())

    override fun setSize(size: Float): GameRectangle {
        rectangle.setSize(size)
        return this
    }

    override fun setSize(width: Float, height: Float): GameRectangle {
        rectangle.setSize(width, height)
        return this
    }

    override fun setTopLeftToPoint(topLeftPoint: Vector2): GameRectangle {
        super.setTopLeftToPoint(topLeftPoint)
        return this
    }

    override fun setTopCenterToPoint(topCenterPoint: Vector2): GameRectangle {
        super.setTopCenterToPoint(topCenterPoint)
        return this
    }

    override fun setTopRightToPoint(topRightPoint: Vector2): GameRectangle {
        super.setTopRightToPoint(topRightPoint)
        return this
    }

    override fun setCenterLeftToPoint(centerLeftPoint: Vector2): GameRectangle {
        super.setCenterLeftToPoint(centerLeftPoint)
        return this
    }

    override fun setCenterToPoint(centerPoint: Vector2): GameRectangle {
        super.setCenterToPoint(centerPoint)
        return this
    }

    override fun setCenterRightToPoint(centerRightPoint: Vector2): GameRectangle {
        super.setCenterRightToPoint(centerRightPoint)
        return this
    }

    override fun setBottomLeftToPoint(bottomLeftPoint: Vector2): GameRectangle {
        super.setBottomLeftToPoint(bottomLeftPoint)
        return this
    }

    override fun setBottomCenterToPoint(bottomCenterPoint: Vector2): GameRectangle {
        super.setBottomCenterToPoint(bottomCenterPoint)
        return this
    }

    override fun setBottomRightToPoint(bottomRightPoint: Vector2): GameRectangle {
        super.setBottomRightToPoint(bottomRightPoint)
        return this
    }

    override fun setSize(size: Vector2): GameRectangle {
        super.setSize(size)
        return this
    }

    override fun translateSize(width: Float, height: Float): GameRectangle {
        super.translateSize(width, height)
        return this
    }

    override fun set(x: Float, y: Float, width: Float, height: Float): GameRectangle {
        rectangle.set(x, y, width, height)
        return this
    }

    fun set(rect: Rectangle): GameRectangle {
        rectangle.set(rect)
        return this
    }

    fun set(rect: IRectangle): GameRectangle {
        setWidth(rect.getWidth())
        setHeight(rect.getHeight())
        setX(rect.getX())
        setY(rect.getY())
        return this
    }

    fun set(rect: GameRectangle): GameRectangle {
        set(rect.rectangle)
        return this
    }

    override fun setX(x: Float): GameRectangle {
        rectangle.x = x
        return this
    }

    override fun setY(y: Float): GameRectangle {
        rectangle.y = y
        return this
    }

    override fun getX() = rectangle.x

    override fun getY() = rectangle.y

    override fun setWidth(width: Float): GameRectangle {
        rectangle.width = width
        return this
    }

    override fun setHeight(height: Float): GameRectangle {
        rectangle.height = height
        return this
    }

    override fun getPosition(out: Vector2): Vector2 = out.set(getX(), getY())

    override fun setPosition(x: Float, y: Float): GameRectangle {
        rectangle.setPosition(x, y)
        return this
    }

    override fun setPosition(position: Vector2): GameRectangle {
        setPosition(position.x, position.y)
        return this
    }

    fun merge(rect: Rectangle): GameRectangle {
        rectangle.merge(rect)
        return this
    }

    fun merge(x: Float, y: Float): GameRectangle {
        rectangle.merge(x, y)
        return this
    }

    fun merge(vec: Vector2): GameRectangle {
        rectangle.merge(vec)
        return this
    }

    fun merge(vecs: kotlin.Array<Vector2>): GameRectangle {
        rectangle.merge(vecs)
        return this
    }

    fun fitOutside(rect: Rectangle): GameRectangle {
        rectangle.fitOutside(rect)
        return this
    }

    fun fitOutside(rect: GameRectangle) = fitOutside(rect.rectangle)

    fun fitInside(rect: Rectangle): GameRectangle {
        rectangle.fitInside(rect)
        return this
    }

    fun fitInside(rect: GameRectangle) = fitInside(rect.rectangle)

    fun fromString(v: String): GameRectangle {
        rectangle.fromString(v)
        return this
    }

    override fun setMaxX(x: Float) = setX(x - getWidth())

    override fun setMaxY(y: Float) = setY(y - getHeight())

    override fun getMaxX() = getX() + getWidth()

    override fun getMaxY() = getY() + getHeight()

    override fun overlaps(other: IGameShape2D) = when (other) {
        is GameRectangle -> Intersector.overlaps(rectangle, other.rectangle)
        is GameCircle -> Intersector.overlaps(other.libgdxCircle, rectangle)
        is GameLine -> {
            other.calculateWorldPoints(out1, out2)
            Intersector.intersectSegmentRectangle(out1, out2, rectangle)
        }

        is GamePolygon -> {
            val polygon = polygonPool.fetch()
            val overlaps = Intersector.overlapConvexPolygons(
                toPolygon(polygon).libgdxPolygon, other.libgdxPolygon
            )
            polygonPool.free(polygon)
            overlaps
        }

        else -> OVERLAP_EXTENSION?.invoke(this, other) == true
    }

    override fun getBoundingRectangle(out: GameRectangle) = out.set(this)

    override fun setCenter(x: Float, y: Float): GameRectangle {
        rectangle.setCenter(x, y)
        return this
    }

    override fun getCenter(out: Vector2): Vector2 = rectangle.getCenter(out)

    override fun setCenter(center: Vector2): GameRectangle = setCenter(center.x, center.y)

    fun setCenterX(centerX: Float): GameRectangle {
        setCenter(centerX, getCenter(out1).y)
        return this
    }

    fun setCenterY(centerY: Float): GameRectangle {
        setCenter(getCenter(out1).x, centerY)
        return this
    }

    override fun translate(x: Float, y: Float): GameRectangle {
        setX(getX() + x)
        setY(getY() + y)
        return this
    }

    override fun translate(delta: Vector2): GameRectangle {
        translate(delta.x, delta.y)
        return this
    }

    override fun getWidth() = rectangle.width

    override fun getHeight() = rectangle.height

    override fun positionOnPoint(point: Vector2, position: Position): GameRectangle {
        super.positionOnPoint(point, position)
        return this
    }

    override fun copy(): GameRectangle = GameRectangle(this)

    override fun draw(renderer: ShapeRenderer): GameRectangle {
        renderer.color = drawingColor
        renderer.set(drawingShapeType)
        renderer.rect(getX(), getY(), getWidth(), getHeight())
        return this
    }

    fun getSplitDimensions(size: Float, out: GamePair<Int, Int>) = getSplitDimensions(size, size, out)

    fun getSplitDimensions(rectWidth: Float, rectHeight: Float, out: GamePair<Int, Int>): GamePair<Int, Int> {
        val rows = (getHeight() / rectHeight).roundToInt()
        val columns = (getWidth() / rectWidth).roundToInt()
        return out.set(rows, columns)
    }

    fun splitIntoCells(rowsAndColumns: Int, out: Matrix<GameRectangle>) =
        splitIntoCells(rowsAndColumns, rowsAndColumns, out)

    fun splitIntoCells(rows: Int, columns: Int, out: Matrix<GameRectangle>): Matrix<GameRectangle> {
        val cellWidth = ceil(getWidth() / columns).toInt()
        val cellHeight = ceil(getHeight() / rows).toInt()

        out.clear()
        out.rows = rows
        out.columns = columns

        for (row in 0 until rows) for (column in 0 until columns) {
            val cell = GameRectangle(getX() + column * cellWidth, getY() + row * cellHeight, cellWidth, cellHeight)
            out[column, row] = cell
        }

        return out
    }

    fun splitByCellSize(size: Float, out: Matrix<GameRectangle>) = splitByCellSize(size, size, out)

    fun splitByCellSize(size: Vector2, out: Matrix<GameRectangle>) = splitByCellSize(size.x, size.y, out)

    fun splitByCellSize(rectWidth: Float, rectHeight: Float, out: Matrix<GameRectangle>): Matrix<GameRectangle> {
        val rows = ceil(getHeight() / rectHeight).roundToInt()
        val columns = ceil(getWidth() / rectWidth).roundToInt()
        return splitIntoCells(rows, columns, out)
    }

    override fun contains(point: Vector2) = rectangle.contains(point)

    override fun contains(x: Float, y: Float) = rectangle.contains(x, y)

    override fun equals(other: Any?) = other is GameRectangle && rectangle == other.rectangle

    override fun hashCode() = rectangle.hashCode()

    override fun toString() = rectangle.toString()

    fun toIntString(): String {
        val intX = rectangle.x.toInt()
        val intY = rectangle.y.toInt()
        val intWidth = rectangle.width.toInt()
        val intHeight = rectangle.height.toInt()
        return "[$intX, $intY, $intWidth, $intHeight]"
    }
}
