package com.test.game.screens.levels.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.UtilMethods
import com.engine.common.enums.Direction
import com.engine.common.enums.ProcessState
import com.engine.common.extensions.toVector2
import com.engine.common.interfaces.PositionSupplier
import com.engine.common.interfaces.Resettable
import com.engine.common.interfaces.Updatable
import com.engine.common.time.Timer
import com.test.game.ConstVals
import kotlin.math.min

class CameraManagerForRooms(private val camera: Camera) : Updatable, Resettable {

  companion object {
    const val DELAY_DURATION = .35f
    const val TRANS_DURATION = 1f
    const val DISTANCE_ON_TRANSITION = 1.5f
  }

  private val delayTimer = Timer(DELAY_DURATION)
  private val transTimer = Timer(TRANS_DURATION)

  private val transitionStart = Vector2()
  private val transitionTarget = Vector2()

  private val focusStart = Vector2()
  private val focusTarget = Vector2()

  var gameRooms: Array<RectangleMapObject>? = null

  var focus: PositionSupplier? = null
    set(value) {
      field = value
      reset = true
      if (value == null) return
      val pos: Vector2 = value.getPosition()
      camera.position.x = pos.x
      camera.position.y = pos.y
    }

  var priorGameRoom: RectangleMapObject? = null
    private set

  var currentGameRoom: RectangleMapObject? = null
    private set

  var transitionDirection: Direction? = null
    private set

  var transitionState: ProcessState? = null
    private set

  val transitioning: Boolean
    get() = transitionState != null

  val transitionInterpolation: Vector2?
    get() =
        if (transitionState == null) null
        else {
          val startCopy = focusStart.cpy()
          val targetCopy = focusTarget.cpy()
          UtilMethods.interpolate(startCopy, targetCopy, transitionTimerRatio)
        }

  val delayFinished: Boolean
    get() = delayTimer.isFinished()

  val delayJustFinished: Boolean
    get() = delayTimer.isJustFinished()

  val transitionTimerRatio: Float
    get() = transTimer.getRatio()

  var beginTransition: (() -> Unit)? = null
  var continueTransition: ((Float) -> Unit)? = null
  var endTransition: (() -> Unit)? = null

  var reset = false

  override fun update(delta: Float) {
    if (reset) {
      reset = false
      priorGameRoom = null
      currentGameRoom = priorGameRoom
      transitionDirection = null
      transitionState = null
      setCameraToFocusable()
      currentGameRoom = nextGameRoom()
    } else if (transitioning) {
      onTransition(delta)
    } else {
      onNoTransition()
    }
  }

  override fun reset() {
    reset = true
  }

  private fun setTransitionValues(next: Rectangle) {
    transitionState = ProcessState.BEGIN

    transitionStart.set(camera.position.toVector2())
    transitionTarget.set(transitionStart)

    focusStart.set(focus!!.getPosition())
    focusTarget.set(focusStart)

    when (transitionDirection) {
      Direction.LEFT -> {
        transitionTarget.x =
            (next.x + next.width -
                    min((next.width / 2f).toDouble(), (camera.viewportWidth / 2f).toDouble()))
                .toFloat()
        focusTarget.x = next.x + next.width - DISTANCE_ON_TRANSITION * ConstVals.PPM
      }
      Direction.RIGHT -> {
        transitionTarget.x =
            (next.x + min((next.width / 2f).toDouble(), (camera.viewportWidth / 2f).toDouble()))
                .toFloat()
        focusTarget.x = next.x + DISTANCE_ON_TRANSITION * ConstVals.PPM
      }
      Direction.UP -> {
        transitionTarget.y =
            (next.y + min((next.height / 2f).toDouble(), (camera.viewportHeight / 2f).toDouble()))
                .toFloat()
        focusTarget.y = next.y + DISTANCE_ON_TRANSITION * ConstVals.PPM
      }
      Direction.DOWN -> {
        transitionTarget.y =
            (next.y + next.height -
                    min((next.height / 2f).toDouble(), (camera.viewportHeight / 2f).toDouble()))
                .toFloat()
        focusTarget.y = next.y + next.height - DISTANCE_ON_TRANSITION * ConstVals.PPM
      }
      null -> {}
    }
  }

  private fun onNoTransition() {
    // if the current room is null, then we need to find the current room
    if (currentGameRoom == null) {
      val nextGameRoom = nextGameRoom()

      if (nextGameRoom != null) {
        priorGameRoom = currentGameRoom
        currentGameRoom = nextGameRoom
      }

      // set the camera's x to the focusable's x
      focus?.getPosition()?.let { camera.position.x = it.x }

      // return and wait until next frame to do anything else
      return
    }

    // if focus is null, then nothing else should be done
    if (focus == null) return

    val currentRoomBounds = currentGameRoom?.rectangle ?: return

    // if the current room's bounds contains the focus, then set the camera
    // to the focus and then adjust to constrain the camera to the room
    if (currentRoomBounds.contains(focus!!.getPosition())) {
      setCameraToFocusable()

      if (camera.position.y >
          currentRoomBounds.y + currentRoomBounds.height - camera.viewportHeight / 2f) {
        camera.position.y =
            currentRoomBounds.y + currentRoomBounds.height - camera.viewportHeight / 2f
      }

      if (camera.position.y < currentRoomBounds.y + camera.viewportHeight / 2f) {
        camera.position.y = currentRoomBounds.y + camera.viewportHeight / 2f
      }

      if (camera.position.x >
          currentRoomBounds.x + currentRoomBounds.width - camera.viewportWidth / 2f) {
        camera.position.x =
            currentRoomBounds.x + currentRoomBounds.width - camera.viewportWidth / 2f
      }

      if (camera.position.x < currentRoomBounds.x + camera.viewportWidth / 2f) {
        camera.position.x = currentRoomBounds.x + camera.viewportWidth / 2f
      }

      // nothing more to be done, return
      return
    }

    // the current room doesn't contain the focus, so we need to get the next
    // game room if there is one
    val nextGameRoom = nextGameRoom()

    // if there is no game room containing the focus, then we need to set the
    // camera's x to the focus's x and return
    if (nextGameRoom == null) {
      camera.position.x = UtilMethods.roundedFloat(focus!!.getPosition().x, 3)
      return
    }

    // if there is a next game room, then we need to trigger the transition phase
    // and set the transition direction

    val overlap = Rectangle()

    val width = 5f * ConstVals.PPM
    val height = 5f * ConstVals.PPM

    val boundingBox = Rectangle().setSize(width, height).setCenter(focus!!.getPosition())

    transitionDirection =
        UtilMethods.getOverlapPushDirection(boundingBox, currentGameRoom!!.rectangle, overlap)

    priorGameRoom = currentGameRoom
    currentGameRoom = nextGameRoom

    if (transitionDirection == null) return

    setTransitionValues(nextGameRoom.rectangle)
  }

  private fun onTransition(delta: Float) {
    when (transitionState) {
      ProcessState.END -> {
        transitionDirection = null
        transitionState = null

        delayTimer.reset()
        transTimer.reset()

        transitionStart.setZero()
        transitionTarget.setZero()

        endTransition?.invoke()
      }
      ProcessState.BEGIN -> {
        transitionState = ProcessState.CONTINUE

        beginTransition?.invoke()
        continueTransition?.invoke(delta)

        delayTimer.update(delta)
        if (!delayTimer.isFinished()) return

        transTimer.update(delta)

        val pos: Vector2 =
            UtilMethods.interpolate(transitionStart, transitionTarget, transitionTimerRatio)
        camera.position.x = pos.x
        camera.position.y = pos.y

        transitionState = if (transTimer.isFinished()) ProcessState.END else ProcessState.CONTINUE
      }
      ProcessState.CONTINUE -> {
        continueTransition?.invoke(delta)

        delayTimer.update(delta)
        if (!delayTimer.isFinished()) return

        transTimer.update(delta)

        val pos: Vector2 =
            UtilMethods.interpolate(transitionStart, transitionTarget, transitionTimerRatio)
        camera.position.x = pos.x
        camera.position.y = pos.y

        transitionState = if (transTimer.isFinished()) ProcessState.END else ProcessState.CONTINUE
      }
      null -> {}
    }
  }

  private fun nextGameRoom(): RectangleMapObject? {
    if (focus == null || gameRooms == null) return null

    var nextGameRoom: RectangleMapObject? = null

    for (room in gameRooms!!.iterator()) {
      if (room.rectangle.contains(focus!!.getPosition())) {
        nextGameRoom = room
        break
      }
    }

    return nextGameRoom
  }

  private fun setCameraToFocusable() {
    focus?.let {
      val pos = it.getPosition()
      camera.position.x = pos.x
      camera.position.y = pos.y
    }
  }
}
