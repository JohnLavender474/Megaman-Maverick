package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.utils.toGameRectangle

// https://stackoverflow.com/questions/16509244/set-camera-rotation-in-libgdx
class RotatableCamera(var onJustFinishedRotating: (() -> Unit)? = null, var printDebug: Boolean = true) :
    OrthographicCamera(), Updatable, Resettable, IDirectionRotatable {

    companion object {
        const val TAG = "RotatableCamera"
        private val interpolation = Interpolation.smooth
    }

    /**
     * Should not set this value directly. Instead, call [startRotation].
     */
    override var directionRotation = Direction.UP

    private val reusableRect = GameRectangle()
    private var latestCoercingBounds = GameRectangle()

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
                    "cam_direction=$directionRotation, " +
                    "cam_pos=$position, " +
                    "coercing_bounds=${latestCoercingBounds}, " +
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

    override fun reset() {
        startRot = 0f
        rotAmount = 0f
        totRot = 0f
        rotTime = 0f
        rotFinished = true
        accumulator = 0f
        rotateTo(0f)
    }

    fun getRotatedBounds() = toGameRectangle(reusableRect).getCardinallyRotatedShape(directionRotation)

    fun coerceIntoBounds(bounds: GameRectangle) {
        latestCoercingBounds = bounds
        when (directionRotation) {
            Direction.UP, Direction.DOWN -> {
                if (position.y > bounds.getMaxY() - viewportHeight / 2f)
                    position.y = bounds.getMaxY() - viewportHeight / 2f
                if (position.y < bounds.y + viewportHeight / 2f)
                    position.y = bounds.y + viewportHeight / 2f
                if (position.x > bounds.getMaxX() - viewportWidth / 2f)
                    position.x = bounds.getMaxX() - viewportWidth / 2f
                if (position.x < bounds.x + viewportWidth / 2f)
                    position.x = bounds.x + viewportWidth / 2f
            }

            else -> {
                if (position.y > bounds.getMaxY() - viewportWidth / 2f)
                    position.y = bounds.getMaxY() - viewportWidth / 2f
                if (position.y < bounds.y + viewportWidth / 2f)
                    position.y = bounds.y + viewportWidth / 2f
                if (position.x > bounds.getMaxX() - viewportHeight / 2f)
                    position.x = bounds.getMaxX() - viewportHeight / 2f
                if (position.x < bounds.x + viewportHeight / 2f)
                    position.x = bounds.x + viewportHeight / 2f
            }
        }
    }

    fun startRotation(direction: Direction, time: Float) {
        if (printDebug) GameLogger.debug(
            TAG,
            "update(): START ROTATION: " +
                "cam_direction=$directionRotation, " +
                "cam_pos=$position, " +
                "coercing_bounds=${latestCoercingBounds}, " +
                "cam_rotated_bounds=${getRotatedBounds()}"
        )

        directionRotation = direction
        startRotation(direction.rotation, time)
    }

    fun immediateRotation(direction: Direction) {
        this.directionRotation = direction
        rotateTo(direction.rotation)
        rotFinished = true
        accumulator = 0f
    }

    private fun startRotation(degrees: Float, time: Float) {
        startRot = totRot
        rotAmount = degrees
        rotTime = time
        rotFinished = false
    }

    private fun rotateTo(degrees: Float) = rotate(degrees - totRot)
}
