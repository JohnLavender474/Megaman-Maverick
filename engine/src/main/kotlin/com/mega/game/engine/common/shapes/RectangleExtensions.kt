package com.mega.game.engine.common.shapes

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.utils.BoundingBoxUtils

fun Rectangle.getRandomPositionInBounds(out: Vector2): Vector2 {
    val randX = UtilMethods.getRandom(x, x + width)
    val randY = UtilMethods.getRandom(y, y + height)
    return out.set(randX, randY)
}

fun Rectangle.toBoundingBox(out: BoundingBox): BoundingBox =
    out.set(Vector3(x, y, 0f), Vector3(x + width, y + height, 0f))

fun Rectangle.isInCamera(camera: Camera, out: BoundingBox) = BoundingBoxUtils.isInCamera(toBoundingBox(out), camera)

fun Rectangle.toIntArray(out: Array<Int>) =
    out.addAll(x.toInt(), y.toInt(), (width + 1).toInt(), (height + 1).toInt())

fun Rectangle.toGameRectangle(out: GameRectangle) = out.set(this)
