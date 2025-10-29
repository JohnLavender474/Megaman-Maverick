package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.time.Timer
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

object MegaUtilMethods {

    fun delayRun(game: MegamanMaverickGame, delay: Float, action: () -> Unit) {
        if (delay <= 0f) throw IllegalArgumentException("Delay must be greater than 0: $delay")

        val id = UUID.randomUUID().toString()

        val timer = Timer(delay)

        game.updatables.put(id) { delta ->
            timer.update(delta)
            if (timer.isJustFinished()) {
                action()

                game.runQueue.addLast { game.updatables.remove(id) }
            }
        }
    }

    fun getSmallFontSize() = (ConstVals.PPM / 3f).roundToInt()

    fun getDefaultFontSize() = (ConstVals.PPM / 2f).roundToInt()

    fun getLargeFontSize() = (ConstVals.PPM / 1.5f).roundToInt()

    fun calculateJumpImpulse(
        source: Vector2,
        target: Vector2,
        verticalBaseImpulse: Float,
        horizontalScalar: Float = 1f,
        verticalScalar: Float = 1f,
        out: Vector2 = GameObjectPools.fetch(Vector2::class)
    ) = calculateJumpImpulse(
        source.x,
        source.y,
        target.x,
        target.y,
        horizontalScalar,
        verticalBaseImpulse,
        verticalScalar,
        out
    )

    fun calculateJumpImpulse(
        sourceX: Float, sourceY: Float,
        targetX: Float, targetY: Float,
        horizontalScalar: Float, verticalBaseImpulse: Float,
        verticalScalar: Float,
        out: Vector2 = GameObjectPools.fetch(Vector2::class)
    ): Vector2 {
        val horizontalDistance = targetX - sourceX
        val verticalDistance = targetY - sourceY

        val impulseX = horizontalDistance * horizontalScalar
        val impulseY = verticalBaseImpulse + (verticalDistance * verticalScalar)

        return out.set(impulseX, impulseY)
    }

    fun interpolate(start: Vector2, target: Vector2, delta: Float) =
        UtilMethods.interpolate(start, target, delta, GameObjectPools.fetch(Vector2::class))

    fun snapToNearest90(rotation: Float) = ((rotation / 90f).roundToInt() * 90f) % 360f

    fun snapToFloor90(rotation: Float) = (floor(rotation / 90f).toInt() * 90f) % 360f

    fun snapToCeil90(rotation: Float) = (ceil(rotation / 90f).toInt() * 90f) % 360f
}
