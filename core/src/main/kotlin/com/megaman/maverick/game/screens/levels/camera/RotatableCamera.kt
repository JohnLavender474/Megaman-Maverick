package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.collision.BoundingBox
import com.maverick.game.engine.camera.ICameraView
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IRectangle
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.extensions.getRotatedBounds

// https://stackoverflow.com/questions/16509244/set-camera-rotation-in-libgdx
class RotatableCamera(var onJustFinishedRotating: (() -> Unit)? = null, var printDebug: Boolean = true) :
    OrthographicCamera(), Updatable, IDirectional, ICameraView {

    companion object {
        const val TAG = "RotatableCamera"
        private val interpolation = Interpolation.smooth
    }

    /**
     * Should not set this value directly. Instead, call [startRotation].
     */
    override var direction = Direction.UP

    val viewWidth: Float
        get() = if (direction.isVertical()) viewportWidth else viewportHeight

    val viewHeight: Float
        get() = if (direction.isVertical()) viewportHeight else viewportWidth

    val blendedViewWidth: Float
        get() = blendViewSize(startViewWidth, viewWidth)

    val blendedViewHeight: Float
        get() = blendViewSize(startViewHeight, viewHeight)

    private var startViewWidth = 0f
    private var startViewHeight = 0f
    private var blendTime = 0f
    private var blendDuration = 0f

    private var accumulator = 0f
    private var startRot = 0f
    private var rotAmount = 0f
    private var totRot = 0f
    private var rotTime = 0f

    private var rotFinished = true

    override fun update(delta: Float) {
        if (rotFinished) return

        blendTime += delta
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

    override fun getRotatedBounds(out: GameRectangle): GameRectangle {
        toGameRectangle(out)
        out.rotate(direction.rotation, position.x, position.y)
        return out
    }

    override fun toGameRectangle(out: GameRectangle): GameRectangle {
        out.setSize(viewportWidth, viewportHeight)
        out.setCenter(position.x, position.y)
        return out
    }

    override fun overlaps(bounds: IRectangle, out: BoundingBox) = frustum.boundsInFrustum(bounds.toBoundingBox(out))

    fun startRotation(direction: Direction, time: Float) {
        if (printDebug) GameLogger.debug(
            TAG,
            "update(): START ROTATION: " +
                "cam_direction=$direction, " +
                "cam_pos=$position, " +
                "cam_rotated_bounds=${getRotatedBounds()}"
        )

        startViewWidth = blendedViewWidth
        startViewHeight = blendedViewHeight
        blendTime = 0f
        blendDuration = time - accumulator

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

    private fun blendViewSize(start: Float, end: Float) =
        if (rotFinished) end else interpolation.apply(start, end, MathUtils.clamp(blendTime / blendDuration, 0f, 1f))

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
