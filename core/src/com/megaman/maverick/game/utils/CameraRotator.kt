package com.megaman.maverick.game.utils

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable

// Thanks to https://stackoverflow.com/questions/16509244/set-camera-rotation-in-libgdx
class CameraRotator(private val cameras: Array<OrthographicCamera>, var onJustFinished: (() -> Unit)? = null) :
    Updatable, Resettable {

    companion object {
        private val interpolation = Interpolation.smooth
    }

    private var startRot = 0f
    private var rotAmount = 0f
    private var totRot = 0f
    private var rotTime = 0f
    private var rotFinished = true
    private var accumulator = 0f

    constructor(camera: OrthographicCamera, onJustFinished: (() -> Unit)? = null) : this(
        gdxArrayOf(camera),
        onJustFinished
    )

    fun isFinished() = rotFinished

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
            onJustFinished?.invoke()
        }
    }

    fun startRotation(degrees: Float, time: Float) {
        startRot = totRot
        rotAmount = degrees
        rotTime = time
        rotFinished = false
    }

    private fun rotate(degrees: Float) {
        cameras.forEach { it.rotate(degrees) }
        totRot += degrees
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