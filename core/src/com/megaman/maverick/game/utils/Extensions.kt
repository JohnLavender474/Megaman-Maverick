package com.megaman.maverick.game.utils

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
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.OrderedMap
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getMusic
import com.engine.common.extensions.getSound
import com.engine.common.objects.Matrix
import com.engine.common.objects.Properties
import com.engine.common.shapes.*
import com.engine.entities.IGameEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import kotlin.math.roundToInt

fun IGameEntity.getMegamanMaverickGame() = game as MegamanMaverickGame

fun MapObject.convertToProps(): Properties = when (this) {
    is RectangleMapObject -> toProps()
    is PolygonMapObject -> toProps()
    is CircleMapObject -> toProps()
    is PolylineMapObject -> toProps()
    else -> throw IllegalArgumentException("Unknown map object type: $this")
}

fun MapObject.getShape(): IGameShape2D = when (this) {
    is RectangleMapObject -> rectangle.toGameRectangle()
    is PolygonMapObject -> polygon.toGamePolygon()
    is CircleMapObject -> circle.toGameCircle()
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
    // TODO: props.put(ConstKeys.LINES, getShape())
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

fun Camera.toGameRectangle(): GameRectangle {
    val rectangle = GameRectangle()
    rectangle.setSize(viewportWidth, viewportHeight)
    rectangle.setCenter(position.x, position.y)
    return rectangle
}

fun getDefaultCameraPosition(): Vector3 {
    val v = Vector3()
    v.x = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
    v.y = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
    return v
}

fun Camera.setToDefaultPosition() {
    val v = getDefaultCameraPosition()
    position.set(v)
}

fun AssetManager.getSounds(): OrderedMap<SoundAsset, Sound> {
    val sounds = OrderedMap<SoundAsset, Sound>()
    for (ass in SoundAsset.values()) sounds.put(ass, getSound(ass.source))
    return sounds
}

fun AssetManager.getMusics(): OrderedMap<MusicAsset, Music> {
    val music = OrderedMap<MusicAsset, Music>()
    for (ass in MusicAsset.values()) music.put(ass, getMusic(ass.source))
    return music
}

fun GameRectangle.split(rectWidth: Float, rectHeight: Float): Matrix<GameRectangle> {
    val rows = (height / rectHeight).roundToInt()
    val columns = (width / rectWidth).roundToInt()
    val matrix = Matrix<GameRectangle>(rows, columns)
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val rectangle = GameRectangle(x + column * rectWidth, y + row * rectHeight, rectWidth, rectHeight)
            matrix[column, row] = rectangle
        }
    }
    return matrix
}

fun GamePolygon.splitIntoGameRectanglesBasedOnCenter(rectWidth: Float, rectHeight: Float): Matrix<GameRectangle> {
    val bounds = getBoundingRectangle()
    val x = bounds.x
    val y = bounds.y
    val width = bounds.width
    val height = bounds.height
    val rows = (height / rectHeight).roundToInt()
    val columns = (width / rectWidth).roundToInt()
    val matrix = Matrix<GameRectangle>(rows, columns)
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            val rectangle = GameRectangle(x + column * rectWidth, y + row * rectHeight, rectWidth, rectHeight)
            if (contains(rectangle.getCenter())) matrix[column, row] = rectangle
        }
    }
    return matrix
}

fun Direction.getOpposingPosition() = when (this) {
    Direction.UP -> Position.BOTTOM_CENTER
    Direction.DOWN -> Position.TOP_CENTER
    Direction.LEFT -> Position.CENTER_RIGHT
    Direction.RIGHT -> Position.CENTER_LEFT
}

