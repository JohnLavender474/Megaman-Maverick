package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraFollowMode
import com.maverick.game.engine.camera.ICameraPart
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.utils.extensions.toGameRectangle

class CameraRoomClamp(
    private val follow: CameraFollow,
    private val roomBounds: CameraRoomBounds
) : ICameraPart<RotatableCamera> {

    override fun update(camera: RotatableCamera, delta: Float): Boolean {
        when (follow.mode) {
            CameraFollowMode.X_AND_Y -> {
                val room = roomBounds.currentGameRoom ?: return false
                clamp(camera, room.rectangle.toGameRectangle())
            }
            CameraFollowMode.X_ONLY -> clamp(camera, roomBounds.priorGameRoom!!.rectangle.toGameRectangle())
            CameraFollowMode.OFF -> return false
        }

        roomBounds.scanForNextRoom(camera)

        return false
    }

    private fun clamp(camera: RotatableCamera, bounds: GameRectangle) {
        val adjVpWidth: Float
        val adjVpHeight: Float

        if (camera.direction.isVertical()) {
            adjVpWidth = camera.viewportWidth
            adjVpHeight = camera.viewportHeight
        } else {
            adjVpWidth = camera.viewportHeight
            adjVpHeight = camera.viewportWidth
        }

        if (camera.position.y > bounds.getMaxY() - adjVpHeight / 2f)
            camera.position.y = bounds.getMaxY() - adjVpHeight / 2f
        if (camera.position.y < bounds.getY() + adjVpHeight / 2f)
            camera.position.y = bounds.getY() + adjVpHeight / 2f
        if (camera.position.x > bounds.getMaxX() - adjVpWidth / 2f)
            camera.position.x = bounds.getMaxX() - adjVpWidth / 2f
        if (camera.position.x < bounds.getX() + adjVpWidth / 2f)
            camera.position.x = bounds.getX() + adjVpWidth / 2f
    }
}
