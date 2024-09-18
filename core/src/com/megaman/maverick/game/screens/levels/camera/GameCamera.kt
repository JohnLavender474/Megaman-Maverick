package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.shapes.GamePolygon
import com.mega.game.engine.common.shapes.GameRectangle

// use [CameraRotator] for now
class GameCamera(var onJustFinishedRotating: (() -> Unit)? = null) : OrthographicCamera(), Updatable, Resettable {
    
    companion object {
        private val interpolation = Interpolation.smooth
    }

    private var startRot = 0f
    private var rotAmount = 0f
    private var totRot = 0f
    private var rotTime = 0f
    private var rotFinished = true
    private var accumulator = 0f

    fun toGamePolygon(): GamePolygon {
        val polygon = GameRectangle()
            .setSize(viewportWidth, viewportHeight)
            .setCenter(position.x, position.y)
            .setOrigin(position.x, position.y)
            .toPolygon()
        polygon.rotation = totRot
        return polygon
    }

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

    fun startRotation(degrees: Float, time: Float) {
        startRot = totRot
        rotAmount = degrees
        rotTime = time
        rotFinished = false
    }

    override fun rotate(degrees: Float) {
        super.rotate(degrees)
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