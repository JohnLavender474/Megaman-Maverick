package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods.getOverlapPushDirection
import com.maverick.game.engine.common.UtilMethods.getSingleMostDirectionFromStartToTarget
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.toGameRectangle

class CameraRoomBounds(
    private val follow: CameraFollow,
    private val transition: CameraRoomTransition,
    var transitionScannerDimensions: Vector2,
    var onSetToRoomNoTrans: (() -> Unit)? = null,
    var onFirstUpdateAfterReset: (() -> Unit)? = null
) : ICameraPart, Resettable {

    companion object {
        const val TAG = "CameraRoomBounds"
    }

    var gameRooms: Array<RectangleMapObject>? = null

    var subject: IRoomSubject? = null

    var priorGameRoom: RectangleMapObject? = null
        private set
    var currentGameRoom: RectangleMapObject? = null
        private set

    val currentGameRoomKey: String?
        get() = currentGameRoom?.name

    private var reset = false

    private val outRect = Rectangle()
    private val tempRect1 = GameRectangle()
    private val tempRect2 = GameRectangle()
    private val tempVec1 = Vector2()
    private val tempVec2 = Vector2()

    override fun reset() {
        reset = true
    }

    override fun update(camera: Camera, delta: Float): Boolean {
        if (reset) {
            GameLogger.debug(TAG, "update(): reset")

            reset = false

            priorGameRoom = null
            currentGameRoom = null

            transition.cancel()

            follow.moveToFocus(camera)

            currentGameRoom = nextGameRoom()

            onFirstUpdateAfterReset?.invoke()

            return true
        }

        if (transition.transitioning) return false

        val subject = subject
        if (subject == null) {
            follow.mode = CameraFollowMode.OFF
            return false
        }

        if (currentGameRoom == null) {
            val nextGameRoom = nextGameRoom()

            if (nextGameRoom != null) {
                priorGameRoom = currentGameRoom
                currentGameRoom = nextGameRoom
                onSetToRoomNoTrans?.invoke()
            }

            follow.snapXToFocus(camera)
        }

        if (currentGameRoom != null &&
            !currentGameRoom!!.rectangle.toGameRectangle(tempRect1).overlaps(subject.getSubjectBounds())
        ) {
            priorGameRoom = currentGameRoom
            currentGameRoom = null
        }

        val currentRoomBounds = currentGameRoom?.rectangle?.toGameRectangle(tempRect1)

        follow.mode = when {
            currentRoomBounds?.overlaps(subject.getSubjectBounds()) == true -> CameraFollowMode.X_AND_Y
            priorGameRoom != null -> CameraFollowMode.X_ONLY
            else -> CameraFollowMode.OFF
        }

        return false
    }

    fun transitionToRoom(
        camera: Camera,
        roomName: String,
        transDirection: Direction? = null,
        axisOnly: Boolean = false
    ): Boolean {
        if (currentGameRoom == null) throw IllegalStateException(
            "Cannot transition to room $roomName because the current game room is null"
        )

        val nextGameRoom = gameRooms?.first { it.name == roomName } ?: return false

        // Using room centers to calculate the transition direction is a very naive and unreliable way to do this.
        // Ideally, `transDirection` will always be supplied with a non-null value.
        val direction = transDirection ?: getSingleMostDirectionFromStartToTarget(
            currentGameRoom!!.rectangle.getCenter(tempVec1), nextGameRoom.rectangle.getCenter(tempVec2)
        )

        GameLogger.debug(TAG, "transitionToRoom(): transition direction = $direction")

        if (axisOnly && direction != Direction.LEFT && direction != Direction.RIGHT) throw IllegalArgumentException(
            "The axis-only transition to room $roomName resolves to $direction" +
                (if (transDirection == null) " from the room centers" else "") +
                ": give it a LEFT or RIGHT transition direction, or place the rooms side by side"
        )

        transition.start(
            camera,
            nextGameRoom.rectangle,
            direction,
            subject!!.getSubjectBounds().getCenter(tempVec1),
            axisOnly
        )

        priorGameRoom = currentGameRoom
        currentGameRoom = nextGameRoom

        return true
    }

    fun scanForNextRoom(camera: Camera) {
        val subject = subject ?: return
        val currentRoomBounds = currentGameRoom?.rectangle?.toGameRectangle(tempRect1) ?: return

        for (room in gameRooms!!) {
            val roomBounds = room.rectangle.toGameRectangle(tempRect2)
            if (roomBounds.overlaps(subject.getSubjectBounds()) && room.name != currentGameRoomKey) {
                val boundingBox = GameRectangle()
                    .setSize(transitionScannerDimensions.x, transitionScannerDimensions.y)
                    .setCenter(subject.getSubjectBounds().getCenter(tempVec1))

                val direction = getOverlapPushDirection(boundingBox, currentRoomBounds, outRect)

                GameLogger.debug(TAG, "scanForNextRoom(): transition direction = $direction")

                priorGameRoom = currentGameRoom
                currentGameRoom = room

                transition.start(camera, room.rectangle, direction, subject.getSubjectBounds().getCenter(tempVec1))

                break
            }
        }
    }

    fun nextGameRoom(): RectangleMapObject? {
        val subject = subject
        if (subject == null || gameRooms == null) {
            GameLogger.debug(TAG, "nextGameRoom(): no room subject, no game rooms, so no next room")
            return null
        }

        var nextGameRoom: RectangleMapObject? = null
        for (room in gameRooms!!)
            if (room.rectangle.contains(subject.getSubjectBounds().getCenter(tempVec1))) {
                nextGameRoom = room
                break
            }
        nextGameRoom = nextGameRoom ?: currentGameRoom

        GameLogger.debug(TAG, "nextGameRoom(): nextGameRoom=${nextGameRoom?.name}")

        return nextGameRoom
    }
}
