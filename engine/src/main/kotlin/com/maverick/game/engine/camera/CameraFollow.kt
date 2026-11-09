package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.maverick.game.engine.common.UtilMethods.interpolate
import com.maverick.game.engine.common.extensions.epsilonEquals

enum class CameraFollowMode { X_AND_Y, X_ONLY, OFF }

class CameraFollow(
    private val smoothStopDistance: Float,
    var interpolationValue: () -> Float,
    var minX: Float = 0f,
    var maxX: Float = Float.POSITIVE_INFINITY
) : ICameraPart {

    var focus: IFocusable? = null
        private set
    var mode = CameraFollowMode.X_AND_Y
    var smooth = false
        private set

    var followedX = Float.NaN
        private set
    var followedY = Float.NaN
        private set

    val stopRule = object : ICameraPart {
        override fun update(camera: Camera, delta: Float): Boolean {
            val focus = focus
            if (!smooth || focus == null) return false
            val position = focus.getFocusPosition()
            if (position.x.epsilonEquals(camera.position.x, smoothStopDistance) &&
                position.y.epsilonEquals(camera.position.y, smoothStopDistance)
            ) smooth = false
            return false
        }
    }

    fun setFocus(camera: Camera, focus: IFocusable?) {
        this.focus = focus
        if (focus == null) return
        val position = focus.getFocusPosition()
        camera.position.x = position.x.coerceIn(minX, maxX)
        camera.position.y = position.y
    }

    fun startSmoothFollow() {
        smooth = true
    }

    fun stopSmoothFollow() {
        smooth = false
    }

    fun clearFollowed() {
        followedX = Float.NaN
        followedY = Float.NaN
    }

    fun moveToFocus(camera: Camera, followY: Boolean = true) = moveToFocus(camera, followY, record = false)

    private fun moveToFocus(camera: Camera, followY: Boolean, record: Boolean) {
        val focus = focus ?: return
        val position = focus.getFocusPosition()

        if (record && !smooth) {
            followedX = position.x
            if (followY) followedY = position.y
        }

        if (smooth) {
            position.x = interpolate(camera.position.x, position.x, interpolationValue())
            position.y = interpolate(camera.position.y, position.y, interpolationValue())
        }

        camera.position.x = position.x.coerceIn(minX, maxX)
        if (followY) camera.position.y = position.y
    }

    fun snapXToFocus(camera: Camera) {
        val focus = focus ?: return
        camera.position.x = focus.getFocusPosition().x.coerceIn(minX, maxX)
    }

    override fun update(camera: Camera, delta: Float): Boolean {
        when (mode) {
            CameraFollowMode.X_AND_Y -> moveToFocus(camera, followY = true, record = true)
            CameraFollowMode.X_ONLY -> moveToFocus(camera, followY = false, record = true)
            CameraFollowMode.OFF -> {}
        }
        return false
    }
}
