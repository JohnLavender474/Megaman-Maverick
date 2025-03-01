package com.mega.game.engine.common.shapes

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.FloatArray
import com.mega.game.engine.common.interfaces.IRotatable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo

open class GamePolygon() : IGameShape2D, IRotatable, IRotatableShape, Resettable {

    companion object {
        private var OVERLAP_EXTENSION: ((GamePolygon, IGameShape2D) -> Boolean)? = null

        fun setOverlapExtension(overlapExtension: (GamePolygon, IGameShape2D) -> Boolean) {
            OVERLAP_EXTENSION = overlapExtension
        }
    }

    internal val libgdxPolygon = Polygon()

    override var originX: Float
        get() = libgdxPolygon.originX
        set(value) = libgdxPolygon.setOrigin(value, originY)

    override var originY: Float
        get() = libgdxPolygon.originY
        set(value) = libgdxPolygon.setOrigin(originX, value)

    override var rotation: Float
        get() = libgdxPolygon.rotation
        set(value) {
            libgdxPolygon.rotation = value
        }

    override var scaleX: Float
        get() = libgdxPolygon.scaleX
        set(value) {
            libgdxPolygon.setScale(value, libgdxPolygon.scaleY)
        }

    override var scaleY: Float
        get() = libgdxPolygon.scaleY
        set(value) {
            libgdxPolygon.setScale(libgdxPolygon.scaleX, value)
        }

    override var drawingColor: Color = Color.RED
    override var drawingShapeType = ShapeType.Line

    private var tempPolygon: GamePolygon? = null
    private val tempFloatArr = Array<Float>()

    constructor(vertices: kotlin.FloatArray) : this() {
        setLocalVertices(vertices)
    }

    constructor(polygon: GamePolygon) : this() {
        libgdxPolygon.vertices = polygon.libgdxPolygon.vertices.clone()
        setX(polygon.getX())
        setY(polygon.getY())
        scaleX = polygon.scaleX
        scaleY = polygon.scaleY
        originX = polygon.originX
        originY = polygon.originY
        rotation = polygon.rotation
    }

    constructor(polygon: Polygon) : this() {
        setLocalVertices(polygon.vertices.clone())
        setX(polygon.x)
        setY(polygon.y)
        scaleX = polygon.scaleX
        scaleY = polygon.scaleY
        originX = polygon.originX
        originY = polygon.originY
        rotation = polygon.rotation
    }

    override fun getProps(out: Properties): Properties {
        out.putAll(
            "local_vertices" pairTo libgdxPolygon.vertices.clone(),
            "x" pairTo getX(),
            "y" pairTo getY(),
            "scale_x" pairTo scaleX,
            "scale_y" pairTo scaleY,
            "origin_x" pairTo originX,
            "origin_y" pairTo originY,
            "rotation" pairTo rotation,
        )
        return out
    }

    override fun setWithProps(props: Properties): GamePolygon {
        if (props.containsKey("local_vertices")) setLocalVertices(props.get("local_vertices", FloatArray::class)!!)
        if (props.containsKey("x")) setX(props.get("x", Float::class)!!)
        if (props.containsKey("y")) setY(props.get("y", Float::class)!!)
        if (props.containsKey("scale_x")) scaleX = props.get("scale_x", Float::class)!!
        if (props.containsKey("scale_y")) scaleY = props.get("scale_y", Float::class)!!
        if (props.containsKey("origin_x")) originX = props.get("origin_x", Float::class)!!
        if (props.containsKey("origin_y")) originY = props.get("origin_y", Float::class)!!
        if (props.containsKey("rotation")) rotation = props.get("rotation", Float::class)!!
        return this
    }

    fun getTransformedVertices(out: Array<Float>): Array<Float> {
        libgdxPolygon.transformedVertices.forEach { out.add(it) }
        return out
    }

    fun getLocalVertices(out: Array<Float>): Array<Float> {
        libgdxPolygon.vertices.forEach { out.add(it) }
        return out
    }

    fun clearLocalVertices() {
        libgdxPolygon.vertices = kotlin.FloatArray(6)
        setDirty()
    }

    fun setLocalVertices(vertices: FloatArray) = setLocalVertices(vertices.toArray())

    fun setLocalVertices(vertices: kotlin.FloatArray) {
        libgdxPolygon.vertices = vertices
        setDirty()
    }

    fun getVertexCount() = libgdxPolygon.vertexCount

    fun set(polygon: GamePolygon) = set(polygon.libgdxPolygon)

    fun set(polygon: Polygon): GamePolygon {
        setLocalVertices(polygon.vertices)
        setX(polygon.x)
        setY(polygon.y)
        scaleX = polygon.scaleX
        scaleY = polygon.scaleY
        originX = polygon.originX
        originY = polygon.originY
        rotation = polygon.rotation
        return this
    }

    override fun overlaps(other: IGameShape2D) = when (other) {
        is GamePolygon -> Intersector.overlapConvexPolygons(libgdxPolygon, other.libgdxPolygon)
        is GameRectangle -> {
            if (tempPolygon == null) tempPolygon = GamePolygon()
            Intersector.overlapConvexPolygons(libgdxPolygon, other.toPolygon(tempPolygon!!).libgdxPolygon)
        }

        is GameLine -> {
            tempFloatArr.clear()
            val arr = other.getTransformedVertices(tempFloatArr)

            if (tempPolygon == null) tempPolygon = GamePolygon()

            tempPolygon!!.reset()
            tempPolygon!!.setLocalVertices(arr.toArray().toFloatArray())

            Intersector.overlapConvexPolygons(libgdxPolygon, tempPolygon!!.libgdxPolygon)
        }

        is GameCircle -> ShapeUtils.overlapCircleAndPolygon(other.libgdxCircle, libgdxPolygon)
        else -> OVERLAP_EXTENSION?.invoke(this, other) == true
    }

    override fun getBoundingRectangle(out: GameRectangle) = out.set(libgdxPolygon.boundingRectangle)

    override fun setPosition(x: Float, y: Float): GamePolygon {
        libgdxPolygon.setPosition(x, y)
        return this
    }

    override fun setX(x: Float): GamePolygon {
        libgdxPolygon.setPosition(x, libgdxPolygon.y)
        return this
    }

    override fun setY(y: Float): GamePolygon {
        libgdxPolygon.setPosition(libgdxPolygon.x, y)
        return this
    }

    override fun getX() = libgdxPolygon.x

    override fun getY() = libgdxPolygon.y

    override fun getCenter(out: Vector2): Vector2 = libgdxPolygon.boundingRectangle.getCenter(out)

    override fun setCenter(center: Vector2) = setCenter(center.x, center.y)

    override fun setCenter(centerX: Float, centerY: Float): GamePolygon {
        val bounds = libgdxPolygon.boundingRectangle
        libgdxPolygon.setPosition(centerX - bounds.width / 2, centerY - bounds.height / 2)
        return this
    }

    override fun getMaxX(): Float {
        val bounds = libgdxPolygon.boundingRectangle
        return bounds.x + bounds.width
    }

    override fun getMaxY(): Float {
        val bounds = libgdxPolygon.boundingRectangle
        return bounds.y + bounds.height
    }

    override fun translate(translateX: Float, translateY: Float): GamePolygon {
        libgdxPolygon.translate(translateX, translateY)
        return this
    }

    override fun contains(p0: Vector2) = libgdxPolygon.contains(p0)

    override fun contains(p0: Float, p1: Float) = libgdxPolygon.contains(p0, p1)

    override fun copy() = GamePolygon(this)

    override fun rotate(rotation: Float, originX: Float, originY: Float) {
        this.rotation = rotation
        this.originX = originX
        this.originY = originY
    }

    fun setVertex(vertexNum: Int, x: Float, y: Float) = libgdxPolygon.setVertex(vertexNum, x, y)

    fun rotate(rotation: Float) = libgdxPolygon.rotate(rotation)

    fun scale(scale: Float) = libgdxPolygon.scale(scale)

    fun setDirty() = libgdxPolygon.dirty()

    fun area() = libgdxPolygon.area()

    fun getVertex(vertexIndex: Int, pos: Vector2) = libgdxPolygon.getVertex(vertexIndex, pos)

    fun getCentroid(centroid: Vector2): Vector2 = libgdxPolygon.getCentroid(centroid)

    override fun draw(renderer: ShapeRenderer): GamePolygon {
        renderer.color = drawingColor
        renderer.set(drawingShapeType)
        renderer.polygon(libgdxPolygon.transformedVertices)
        return this
    }

    override fun toString() = "GamePolygon(vertices=${getTransformedVertices(tempFloatArr)})"

    override fun reset() {
        originX = 0f
        originY = 0f
        scaleX = 1f
        scaleY = 1f
        rotation = 0f
        setPosition(0f, 0f)
        clearLocalVertices()
    }
}
