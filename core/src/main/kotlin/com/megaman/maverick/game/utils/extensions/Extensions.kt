package com.megaman.maverick.game.utils.extensions

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.MapProperties
import com.badlogic.gdx.maps.objects.CircleMapObject
import com.badlogic.gdx.maps.objects.PolygonMapObject
import com.badlogic.gdx.maps.objects.PolylineMapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getMusic
import com.mega.game.engine.common.extensions.getSound
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GamePolygon
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.shapes.toGameLines
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.utils.GameObjectPools
import kotlin.math.abs
import kotlin.math.roundToInt

fun IntPair.isNeighborOf(coordinate: IntPair, allowDiagonal: Boolean = true): Boolean {
    val dx = abs(this.x - coordinate.x)
    val dy = abs(this.y - coordinate.y)
    return if (!allowDiagonal) (dx == 1 && dy == 0) || (dx == 0 && dy == 1)
    else (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0)
}

fun Vector2.toGridCoordinate(out: IntPair): IntPair {
    out.set(MathUtils.floor(x / ConstVals.PPM), MathUtils.floor(y / ConstVals.PPM))
    return out
}

fun Vector2.toGridCoordinate() = toGridCoordinate(GameObjectPools.fetch(IntPair::class))

fun IntPair.toWorldCoordinate(out: Vector2): Vector2 = out.set(x * ConstVals.PPM.toFloat(), y * ConstVals.PPM.toFloat())

fun IntPair.toWorldCoordinate() = toWorldCoordinate(GameObjectPools.fetch(Vector2::class))

fun MapObject.convertToProps(): Properties = when (this) {
    is RectangleMapObject -> toProps()
    is PolygonMapObject -> toProps()
    is CircleMapObject -> toProps()
    is PolylineMapObject -> toProps()
    else -> throw IllegalArgumentException("Unknown map object type: $this")
}

fun MapObject.getShape(reclaim: Boolean = false): IGameShape2D = when (this) {
    is RectangleMapObject -> rectangle.toGameRectangle(reclaim)
    is PolygonMapObject -> polygon.toGamePolygon(reclaim)
    is CircleMapObject -> circle.toGameCircle(reclaim)
    // TODO: support polyline map object
    else -> throw IllegalArgumentException("Unknown map object type: $this")
}

fun RectangleMapObject.toProps(): Properties {
    val props = Properties()
    props.put(ConstKeys.NAME, name)
    props.put(ConstKeys.BOUNDS, getShape())
    val objProps = properties.toProps()
    props.putAll(objProps)
    return props
}

fun PolygonMapObject.toProps(): Properties {
    val props = Properties()
    props.put(ConstKeys.NAME, name)
    props.put(ConstKeys.POLYGON, getShape())
    val objProps = properties.toProps()
    props.putAll(objProps)
    return props
}

fun CircleMapObject.toProps(): Properties {
    val props = Properties()
    props.put(ConstKeys.NAME, name)
    props.put(ConstKeys.CIRCLE, getShape())
    val objProps = properties.toProps()
    props.putAll(objProps)
    return props
}

fun PolylineMapObject.toProps(): Properties {
    val props = Properties()
    props.put(ConstKeys.NAME, name)
    props.put(ConstKeys.LINES, polyline.toGameLines())
    val objProps = properties.toProps()
    props.putAll(objProps)
    return props
}

fun MapProperties.toProps(): Properties {
    val props = Properties()
    keys.forEach { key -> props.put(key, get(key)) }
    return props
}

fun Camera.toGameRectangle(out: GameRectangle = GameObjectPools.fetch(GameRectangle::class)): GameRectangle {
    out.setSize(viewportWidth, viewportHeight)
    out.setCenter(position.x, position.y)
    return out
}

fun getDefaultCameraPosition(reclaim: Boolean = true) =
    getDefaultCameraPosition(GameObjectPools.fetch(Vector3::class, reclaim))

fun getDefaultCameraPosition(out: Vector3): Vector3 {
    out.x = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
    out.y = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
    return out
}

fun Camera.setToDefaultPosition() {
    val v = getDefaultCameraPosition()
    position.set(v)
}

fun AssetManager.getSounds(out: OrderedMap<SoundAsset, Sound>): OrderedMap<SoundAsset, Sound> {
    for (ass in SoundAsset.entries) out.put(ass, getSound(ass.source))
    return out
}

fun AssetManager.getMusics(out: OrderedMap<MusicAsset, Music>): OrderedMap<MusicAsset, Music> {
    for (ass in MusicAsset.entries) out.put(ass, getMusic(ass.source))
    return out
}

fun GamePolygon.splitIntoGameRectanglesBasedOnCenter(
    rectWidth: Float,
    rectHeight: Float,
    out: Matrix<GameRectangle>
): Matrix<GameRectangle> {
    val bounds = getBoundingRectangle()

    val x = bounds.getX()
    val y = bounds.getY()
    val width = bounds.getWidth()
    val height = bounds.getHeight()

    val rows = (height / rectHeight).roundToInt()
    val columns = (width / rectWidth).roundToInt()

    out.clear()
    out.rows = rows
    out.columns = columns

    for (row in 0 until rows) for (column in 0 until columns) {
        val rectangle = GameObjectPools.fetch(GameRectangle::class)
            .set(x + column * rectWidth, y + row * rectHeight, rectWidth, rectHeight)
        if (contains(rectangle.getCenter())) out[column, row] = rectangle
    }

    return out
}

fun Direction.getOpposingPosition() = when (this) {
    Direction.UP -> Position.BOTTOM_CENTER
    Direction.DOWN -> Position.TOP_CENTER
    Direction.LEFT -> Position.CENTER_RIGHT
    Direction.RIGHT -> Position.CENTER_LEFT
}

