package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraFollowMode
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.ICameraPart
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods.getOverlapPushDirection
import com.maverick.game.engine.common.UtilMethods.getSingleMostDirectionFromStartToTarget
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle

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

    var priorGameRoom: RectangleMapObject? = null
        private set
    var currentGameRoom: RectangleMapObject? = null
        private set

    val currentGameRoomKey: String?
        get() = currentGameRoom?.name

    private var reset = false

    private val outRect = Rectangle()

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

        val focus = follow.focus
        if (focus == null) {
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
            !currentGameRoom!!.rectangle.toGameRectangle().overlaps(focus.getFocusBounds())
        ) {
            priorGameRoom = currentGameRoom
            currentGameRoom = null
        }

        val currentRoomBounds = currentGameRoom?.rectangle?.toGameRectangle()

        follow.mode = when {
            currentRoomBounds?.overlaps(focus.getFocusBounds()) == true -> CameraFollowMode.X_AND_Y
            priorGameRoom != null -> CameraFollowMode.X_ONLY
            else -> CameraFollowMode.OFF
        }

        return false
    }

    fun transitionToRoom(camera: Camera, roomName: String, transDirection: Direction? = null): Boolean {
        if (currentGameRoom == null) throw IllegalStateException(
            "Cannot transition to room $roomName because the current game room is null"
        )

        val nextGameRoom = gameRooms?.first { it.name == roomName } ?: return false

        // Using room centers to calculate the transition direction is a very naive and unreliable way to do this.
        // Ideally, `transDirection` will always be supplied with a non-null value.
        val direction = transDirection ?: getSingleMostDirectionFromStartToTarget(
            currentGameRoom!!.rectangle.getCenter(), nextGameRoom.rectangle.getCenter()
        )

        GameLogger.debug(TAG, "transitionToRoom(): transition direction = $direction")

        transition.start(camera, nextGameRoom.rectangle, direction)

        priorGameRoom = currentGameRoom
        currentGameRoom = nextGameRoom

        return true
    }

    fun scanForNextRoom(camera: Camera) {
        val focus = follow.focus ?: return
        val currentRoomBounds = currentGameRoom?.rectangle?.toGameRectangle() ?: return

        for (room in gameRooms!!) {
            val roomBounds = room.rectangle.toGameRectangle()
            if (roomBounds.overlaps(focus.getFocusBounds()) && room.name != currentGameRoomKey) {
                val boundingBox = GameRectangle()
                    .setSize(transitionScannerDimensions.x, transitionScannerDimensions.y)
                    .setCenter(focus.getFocusBounds().getCenter())

                val direction = getOverlapPushDirection(boundingBox, currentRoomBounds, outRect)

                GameLogger.debug(TAG, "scanForNextRoom(): transition direction = $direction")

                priorGameRoom = currentGameRoom
                currentGameRoom = room

                transition.start(camera, room.rectangle, direction)

                break
            }
        }
    }

    fun nextGameRoom(): RectangleMapObject? {
        val focus = follow.focus
        if (focus == null || gameRooms == null) {
            GameLogger.debug(TAG, "nextGameRoom(): no focus, no game rooms, so no next room")
            return null
        }

        var nextGameRoom: RectangleMapObject? = null
        for (room in gameRooms!!)
            if (room.rectangle.contains(focus.getFocusBounds().getCenter())) {
                nextGameRoom = room
                break
            }
        nextGameRoom = nextGameRoom ?: currentGameRoom

        GameLogger.debug(TAG, "nextGameRoom(): nextGameRoom=${nextGameRoom?.name}")

        return nextGameRoom
    }
}
