package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.extensions.toGameRectangle

// https://stackoverflow.com/questions/16509244/set-camera-rotation-in-libgdx
class RotatableCamera(var onJustFinishedRotating: (() -> Unit)? = null, var printDebug: Boolean = true) :
    OrthographicCamera(), Updatable, IDirectional {

    companion object {
        const val TAG = "RotatableCamera"
        private val interpolation = Interpolation.smooth
    }

    /**
     * Should not set this value directly. Instead, call [startRotation].
     */
    override var direction = Direction.UP

    private var accumulator = 0f
    private var startRot = 0f
    private var rotAmount = 0f
    private var totRot = 0f
    private var rotTime = 0f

    private var rotFinished = true

    override fun update(delta: Float) {
        if (rotFinished) return

        accumulator += delta
        var alpha = accumulator / rotTime
        alpha = MathUtils.clamp(alpha, 0f, 1f)

        rotateTo(interpolation.apply(startRot, rotAmount, alpha))

        if (MathUtils.isEqual(alpha, 1f, 0.0001f)) {
            accumulator = 0f
            rotFinished = true
            onJustFinishedRotating?.invoke()

            if (printDebug) GameLogger.debug(
                TAG,
                "update(): FINISH ROTATION: " +
                    "startRot=$startRot, " +
                    "totRot=$totRot, " +
                    "cam_direction=$direction, " +
                    "cam_pos=$position, " +
                    "cam_rotated_bounds=${getRotatedBounds()}"
            )
        }
    }

    /**
     * Do not call this method directly. Instead, use [startRotation].
     *
     * @param degrees the degrees to rotate
     */
    override fun rotate(degrees: Float) {
        super.rotate(degrees)
        totRot += degrees
    }

    fun getRotatedBounds(): GameRectangle {
        val rect = toGameRectangle()
        rect.rotate(direction.rotation, position.x, position.y)
        return rect
    }

    fun coerceIntoBounds(bounds: GameRectangle) {
        if (printDebug) GameLogger.debug(TAG, "coerceIntoBounds(): bounds=$bounds")

        val adjVpWidth: Float
        val adjVpHeight: Float

        if (direction.isVertical()) {
            adjVpWidth = viewportWidth
            adjVpHeight = viewportHeight
        } else {
            adjVpWidth = viewportHeight
            adjVpHeight = viewportWidth
        }

        if (position.y > bounds.getMaxY() - adjVpHeight / 2f)
            position.y = bounds.getMaxY() - adjVpHeight / 2f
        if (position.y < bounds.getY() + adjVpHeight / 2f)
            position.y = bounds.getY() + adjVpHeight / 2f
        if (position.x > bounds.getMaxX() - adjVpWidth / 2f)
            position.x = bounds.getMaxX() - adjVpWidth / 2f
        if (position.x < bounds.getX() + adjVpWidth / 2f)
            position.x = bounds.getX() + adjVpWidth / 2f
    }

    fun startRotation(direction: Direction, time: Float) {
        if (printDebug) GameLogger.debug(
            TAG,
            "update(): START ROTATION: " +
                "cam_direction=$direction, " +
                "cam_pos=$position, " +
                "cam_rotated_bounds=${getRotatedBounds()}"
        )

        this.direction = direction
        startRotation(direction.rotation, time)
    }

    fun immediateRotation(direction: Direction) {
        GameLogger.debug(TAG, "immediateRotation(): direction=$direction")
        this.direction = direction
        rotateTo(direction.rotation)
        rotFinished = true
        accumulator = 0f
    }

    private fun startRotation(degrees: Float, time: Float) {
        startRot = totRot
        rotAmount = startRot + calculateShortestRotation(startRot, degrees)
        rotTime = time
        rotFinished = false
        GameLogger.debug(
            TAG,
            "startRotation(): startRot=$startRot, rotAmount=$rotAmount, rotTime=$rotTime"
        )
    }

    private fun calculateShortestRotation(current: Float, target: Float): Float {
        var delta = (target - current) % 360
        if (delta < -180) delta += 360
        if (delta > 180) delta -= 360
        return delta
    }

    private fun rotateTo(degrees: Float) = rotate(degrees - totRot)
}
