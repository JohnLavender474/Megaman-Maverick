package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.UtilMethods.getSingleMostDirectionFromStartToTarget
import com.mega.game.engine.common.UtilMethods.interpolate
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.toVector2
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import kotlin.math.min

class CameraManagerForRooms(
    val camera: RotatableCamera,
    var distanceOnTransition: Float,
    var transitionScannerDimensions: Vector2,
    transDelay: Float,
    transDuration: Float,
    var beginTransition: (() -> Unit)? = null,
    var continueTransition: ((Float) -> Unit)? = null,
    var endTransition: (() -> Unit)? = null,
    var onSetToRoomNoTrans: (() -> Unit)? = null,
    var shouldInterpolate: () -> Boolean = { false },
    var interpolationValue: () -> Float = { 1f }
) : Updatable, Resettable {

    companion object {
        const val TAG = "CameraManagerForRooms"
    }

    private val delayTimer = Timer(transDelay)
    private val transTimer = Timer(transDuration)

    private val transitionStart = Vector2()
    private val transitionTarget = Vector2()

    private val focusStart = Vector2()
    private val focusTarget = Vector2()

    var gameRooms: Array<RectangleMapObject>? = null

    var priorGameRoom: RectangleMapObject? = null
        private set
    var currentGameRoom: RectangleMapObject? = null
        private set

    val currentGameRoomKey: String?
        get() = currentGameRoom?.name

    var focus: IFocusable? = null
        set(value) {
            GameLogger.debug(TAG, "set focus to $value")

            field = value
            reset = true

            if (value == null) return

            val pos = value.getFocusPosition()
            camera.position.x = pos.x
            camera.position.y = pos.y
        }

    private var transitionDirection: Direction? = null
    private var transitionState: ProcessState? = null

    val transitioning: Boolean
        get() = transitionState != null

    val transitionInterpolation: Vector2?
        get() = if (transitionState == null) null
        else {
            val startCopy = focusStart.cpy()
            val targetCopy = focusTarget.cpy()
            interpolate(startCopy, targetCopy, transitionTimerRatio, GameObjectPools.fetch(Vector2::class))
        }

    private val transitionTimerRatio: Float
        get() = transTimer.getRatio()

    private var reset = false

    private val outRect = Rectangle()

    override fun update(delta: Float) = when {
        reset -> {
            GameLogger.debug(TAG, "update(): reset")

            reset = false

            priorGameRoom = null
            currentGameRoom = null

            transitionDirection = null
            transitionState = null

            setCameraToFocusable()

            currentGameRoom = nextGameRoom()
        }

        transitioning -> onTransition(delta)
        else -> onNoTransition()
    }

    override fun reset() {
        reset = true
    }

    fun transitionToRoom(roomName: String): Boolean {
        if (currentGameRoom == null) throw IllegalStateException(
            "Cannot transition to room $roomName because the current game room is null"
        )

        val nextGameRoom = gameRooms?.first { it.name == roomName } ?: return false
        transitionDirection = getSingleMostDirectionFromStartToTarget(
            currentGameRoom!!.rectangle.getCenter(), nextGameRoom.rectangle.getCenter()
        )

        GameLogger.debug(TAG, "transitionToRoom(): transition direction = $transitionDirection")

        setTransitionValues(nextGameRoom.rectangle)

        priorGameRoom = currentGameRoom
        currentGameRoom = nextGameRoom

        return true
    }

    private fun setTransitionValues(next: Rectangle) {
        transitionState = ProcessState.BEGIN

        transitionStart.set(camera.position.toVector2(GameObjectPools.fetch(Vector2::class)))

        transitionTarget.set(transitionStart)

        focusStart.set(focus!!.getFocusBounds().getCenter())
        focusTarget.set(focusStart)

        when (transitionDirection) {
            Direction.LEFT -> {
                transitionTarget.x = (next.x + next.width) - min(next.width / 2f, camera.viewportWidth / 2f)
                focusTarget.x = (next.x + next.width) - distanceOnTransition
            }

            Direction.RIGHT -> {
                transitionTarget.x = next.x + min(next.width / 2f, camera.viewportWidth / 2f)
                focusTarget.x = next.x + distanceOnTransition
            }

            Direction.UP -> {
                transitionTarget.y = next.y + min(next.height / 2f, camera.viewportHeight / 2f)
                focusTarget.y = next.y + distanceOnTransition
            }

            Direction.DOWN -> {
                transitionTarget.y = (next.y + next.height) - min(next.height / 2f, camera.viewportHeight / 2f)
                focusTarget.y = (next.y + next.height) - distanceOnTransition
            }

            null -> {}
        }
    }

    private fun onNoTransition() {
        if (currentGameRoom == null) {
            val nextGameRoom = nextGameRoom()

            if (nextGameRoom != null) {
                priorGameRoom = currentGameRoom
                currentGameRoom = nextGameRoom
                onSetToRoomNoTrans?.invoke()
            }

            focus?.getFocusPosition()?.let { camera.position.x = it.x }

            return
        }

        if (focus == null) return

        if (currentGameRoom != null &&
            !currentGameRoom!!.rectangle.toGameRectangle().overlaps(focus!!.getFocusBounds())
        ) {
            priorGameRoom = currentGameRoom
            currentGameRoom = null
        }

        val currentRoomBounds = currentGameRoom?.rectangle?.toGameRectangle()

        when {
            currentRoomBounds?.overlaps(focus!!.getFocusBounds()) == true -> {
                setCameraToFocusable()
                camera.coerceIntoBounds(currentRoomBounds)
            }

            priorGameRoom != null -> {
                setCameraToFocusable(focusY = false)

                val priorRoomBounds = priorGameRoom!!.rectangle.toGameRectangle()
                camera.coerceIntoBounds(priorRoomBounds)
            }

            else -> return
        }

        if (currentRoomBounds != null) for (room in gameRooms!!) {
            val roomBounds = room.rectangle.toGameRectangle()
            if (roomBounds.overlaps(focus!!.getFocusBounds()) && room.name != currentGameRoomKey) {
                val boundingBox = GameRectangle()
                    .setSize(transitionScannerDimensions.x, transitionScannerDimensions.y)
                    .setCenter(focus!!.getFocusBounds().getCenter())

                transitionDirection = getOverlapPushDirection(boundingBox, currentRoomBounds, outRect)

                GameLogger.debug(TAG, "transitionToRoom(): transition direction = $transitionDirection")

                priorGameRoom = currentGameRoom
                currentGameRoom = room

                setTransitionValues(room.rectangle)

                break
            }
        }
    }

    private fun onTransition(delta: Float) {
        if (transitionState == null) return

        when (transitionState!!) {
            ProcessState.END -> {
                GameLogger.debug(TAG, "onTransition(): transition target = $transitionTarget")
                transitionDirection = null
                transitionState = null

                delayTimer.reset()
                transTimer.reset()

                transitionStart.setZero()
                transitionTarget.setZero()

                endTransition?.invoke()
            }

            ProcessState.BEGIN, ProcessState.CONTINUE -> {
                if (transitionState == ProcessState.BEGIN) {
                    beginTransition?.invoke()
                    GameLogger.debug(TAG, "onTransition(): transition start = $transitionStart")
                } else continueTransition?.invoke(delta)

                transitionState = ProcessState.CONTINUE

                delayTimer.update(delta)
                if (!delayTimer.isFinished()) return
                transTimer.update(delta)

                val pos = interpolate(
                    transitionStart,
                    transitionTarget,
                    transitionTimerRatio,
                    GameObjectPools.fetch(Vector2::class)
                )
                camera.position.x = pos.x
                camera.position.y = pos.y

                transitionState = if (transTimer.isFinished()) ProcessState.END else ProcessState.CONTINUE
            }
        }
    }

    private fun nextGameRoom(): RectangleMapObject? {
        if (focus == null || gameRooms == null) {
            GameLogger.debug(TAG, "nextGameRoom(): no focus, no game rooms, so no next room")
            return null
        }

        var nextGameRoom: RectangleMapObject? = null
        for (room in gameRooms!!)
            if (room.rectangle.contains(focus!!.getFocusBounds().getCenter())) {
                nextGameRoom = room
                break
            }
        nextGameRoom = nextGameRoom ?: currentGameRoom

        GameLogger.debug(TAG, "nextGameRoom(): next room = $nextGameRoom")

        return nextGameRoom
    }

    private fun setCameraToFocusable(focusY: Boolean = true) {
        focus?.let {
            val focusPos = it.getFocusPosition()

            if (shouldInterpolate.invoke()) {
                focusPos.x = interpolate(camera.position.x, focusPos.x, interpolationValue.invoke())
                focusPos.y = interpolate(camera.position.y, focusPos.y, interpolationValue.invoke())
            }

            camera.position.x = focusPos.x
            if (focusY) camera.position.y = focusPos.y
        }
    }
}

interface IFocusable {

    fun getFocusBounds(): GameRectangle

    fun getFocusPosition(): Vector2
}
