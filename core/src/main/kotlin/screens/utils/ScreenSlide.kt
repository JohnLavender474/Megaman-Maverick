package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.time.Timer

class ScreenSlide(
    private val camera: Camera,
    private var startPoint: Vector3,
    private var endPoint: Vector3,
    duration: Float,
    setToEnd: Boolean
) : Initializable, Updatable, Resettable {

    companion object {
        const val TAG = "ScreenSlide"
    }

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

        GameLogger.debug(TAG, "init(): camera.position=${camera.position}")
    }

    override fun update(delta: Float) {
        timer.update(delta)
        if (timer.isJustFinished()) {
            val position = if (reversed) startPoint else endPoint
            camera.position.set(position)

            GameLogger.debug(TAG, "update(): timer just finished: camera.position=${camera.position}")
        }

        if (timer.isFinished()) return

        val start: Vector3
        val end: Vector3
        if (reversed) {
            start = endPoint
            end = startPoint
        } else {
            start = startPoint
            end = endPoint
        }
        camera.position.x = UtilMethods.interpolate(start.x, end.x, timer.getRatio())
        camera.position.y = UtilMethods.interpolate(start.y, end.y, timer.getRatio())
    }

    override fun reset() {
        GameLogger.debug(TAG, "reset()")
        reversed = false
    }

    fun reverse() {
        reversed = !reversed
        GameLogger.debug(TAG, "reverse(): reversed=$reversed")
    }

    fun setToEnd() {
        GameLogger.debug(TAG, "setToEnd()")
        timer.setToEnd()
    }
}
