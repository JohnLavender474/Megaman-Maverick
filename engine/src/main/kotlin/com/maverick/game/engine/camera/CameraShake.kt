package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.maverick.game.engine.common.time.Timer

class CameraShake(private val unit: Float) : ICameraPart<Camera> {

    val isFinished: Boolean
        get() = durTimer.isFinished()

    private val durTimer = Timer().setToEnd()
    private val intervalTimer = Timer().setToEnd()

    private var shakeX = 0f
    private var shakeY = 0f
    private var shakeLeft = false

    fun startShake(duration: Float, interval: Float, shakeX: Float, shakeY: Float) {
        this.shakeX = shakeX
        this.shakeY = shakeY
        shakeLeft = false
        durTimer.resetDuration(duration)
        intervalTimer.resetDuration(interval)
    }

    fun stop() {
        durTimer.setToEnd()
        intervalTimer.setToEnd()
    }

    override fun update(camera: Camera, delta: Float): Boolean {
        if (isFinished) return false

        durTimer.update(delta)
        if (isFinished) return false

        intervalTimer.update(delta)
        if (intervalTimer.isFinished()) {
            if (shakeLeft) {
                camera.position.x -= shakeX * unit
                camera.position.y -= shakeY * unit
            } else {
                camera.position.x += shakeX * unit
                camera.position.y += shakeY * unit
            }
            intervalTimer.reset()
            shakeLeft = !shakeLeft
        }

        return false
    }
}
