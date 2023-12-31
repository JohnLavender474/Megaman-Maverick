package com.megaman.maverick.game.screens.utils

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.engine.common.interfaces.Initializable
import com.engine.common.interfaces.Updatable
import com.engine.common.time.Timer

class ScreenSlide(
    private val camera: Camera,
    private val trajectory: Vector3,
    private var startPoint: Vector3,
    private var endPoint: Vector3,
    duration: Float,
    setToEnd: Boolean
) : Initializable, Updatable {

  private val timer = Timer(duration)

  val finished: Boolean
    get() = timer.isFinished()

  val justFinished: Boolean
    get() = timer.isJustFinished()

  init {
    if (setToEnd) setToEnd()
  }

  override fun init() {
    camera.position.scl(startPoint)
    timer.reset()
  }

  override fun update(delta: Float) {
    timer.update(delta)
    if (timer.isJustFinished()) camera.position.set(endPoint)
    if (timer.isFinished()) return
    camera.position.x += trajectory.x * delta * (1f / timer.duration)
    camera.position.y += trajectory.y * delta * (1f / timer.duration)
  }

  fun setToEnd() = timer.setToEnd()

  fun reverse() {
    val temp = endPoint
    endPoint = startPoint
    startPoint = temp
    trajectory.scl(-1f)
  }
}
