package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.megaman.maverick.game.utils.toGameRectangle

// Thanks to https://stackoverflow.com/questions/16509244/set-camera-rotation-in-libgdx
class RotatableCamera(var onJustFinishedRotating: (() -> Unit)? = null) : OrthographicCamera(), Updatable, Resettable {

    companion object {
        const val TAG = "RotatableCamera"
        private val interpolation = Interpolation.smooth
    }

    private var directionRotation = Direction.UP

    private var startRot = 0f
    private var rotAmount = 0f
    private var totRot = 0f
    private var rotTime = 0f
    private var rotFinished = true
    private var accumulator = 0f

    fun getRotatedBounds() = toGameRectangle().getCardinallyRotatedShape(directionRotation)

    fun isFinishedRotating() = rotFinished

    fun getRotation() = totRot

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
        }
    }

    /**
     * Do not call this method directly, should use [startRotation] instead of this method
     */
    override fun rotate(degrees: Float) {
        super.rotate(degrees)
        totRot += degrees
    }

    fun startRotation(direction: Direction, time: Float) {
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

    override fun reset() {
        startRot = 0f
        rotAmount = 0f
        totRot = 0f
        rotTime = 0f
        rotFinished = true
        accumulator = 0f
        rotateTo(0f)
    }
}