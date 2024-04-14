package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.engine.common.interfaces.Resettable
import com.engine.common.interfaces.Updatable
import com.engine.common.time.Timer
import com.megaman.maverick.game.ConstVals

class CameraShaker(private val cam: Camera) : Updatable, Resettable {

    val isFinished: Boolean
        get() = durTimer.isFinished()
    val isJustFinished: Boolean
        get() = durTimer.isJustFinished()

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

    override fun update(delta: Float) {
        durTimer.update(delta)
        if (isFinished) return

        intervalTimer.update(delta)
        if (intervalTimer.isFinished()) {
            if (shakeLeft) {
                cam.position.x -= shakeX * ConstVals.PPM
                cam.position.y -= shakeY * ConstVals.PPM
            } else {
                cam.position.x += shakeX * ConstVals.PPM
                cam.position.y += shakeY * ConstVals.PPM
            }
            intervalTimer.reset()
            shakeLeft = !shakeLeft
        }
    }

    override fun reset() {
        durTimer.reset()
        intervalTimer.reset()
    }
}