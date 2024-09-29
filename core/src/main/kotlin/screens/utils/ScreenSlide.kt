package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer

class ScreenSlide(
    private val camera: Camera,
    private val trajectory: Vector3,
    private var startPoint: Vector3,
    private var endPoint: Vector3,
    duration: Float,
    setToEnd: Boolean
) : Initializable, Updatable, Resettable {

    private val timer = Timer(duration)
    private var reversed = false

    val finished: Boolean
        get() = timer.isFinished()
    val justFinished: Boolean
        get() = timer.isJustFinished()

    init {
        if (setToEnd) setToEnd()
    }

    override fun init() {
        val position = if (reversed) endPoint else startPoint
        camera.position.set(position)
        timer.reset()
    }

    override fun update(delta: Float) {
        timer.update(delta)
        if (timer.isJustFinished()) {
            val position = if (reversed) startPoint else endPoint
            camera.position.set(position)
        }
        if (timer.isFinished()) return
        camera.position.x += trajectory.x * delta * (1f / timer.duration) * if (reversed) -1f else 1f
        camera.position.y += trajectory.y * delta * (1f / timer.duration) * if (reversed) -1f else 1f
    }

    override fun reset() {
        reversed = false
    }

    fun reverse() {
        reversed = !reversed
    }

    fun setToEnd() = timer.setToEnd()
}
