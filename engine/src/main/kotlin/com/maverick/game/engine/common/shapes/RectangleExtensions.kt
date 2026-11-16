package com.maverick.game.engine.common.shapes

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.random.GameRandom
import com.maverick.game.engine.common.utils.BoundingBoxUtils

fun Rectangle.getRandomPositionInBounds(random: GameRandom, out: Vector2): Vector2 {
    val randX = random.nextFloat(x, x + width)
    val randY = random.nextFloat(y, y + height)
    return out.set(randX, randY)
}

fun Rectangle.toBoundingBox(out: BoundingBox): BoundingBox =
    out.inf().ext(x, y, 0f).ext(x + width, y + height, 0f)

fun Rectangle.isInCamera(camera: Camera, out: BoundingBox) = BoundingBoxUtils.isInCamera(toBoundingBox(out), camera)

fun Rectangle.toIntArray(out: Array<Int>) =
    out.addAll(x.toInt(), y.toInt(), (width + 1).toInt(), (height + 1).toInt())

fun Rectangle.toGameRectangle(out: GameRectangle) = out.set(this)
