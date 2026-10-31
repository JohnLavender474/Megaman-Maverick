package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.ICameraPart

class CameraRoomScan(private val roomBounds: CameraRoomBounds) : ICameraPart<RotatableCamera> {

    override fun update(camera: RotatableCamera, delta: Float): Boolean {
        roomBounds.scanForNextRoom(camera)
        return false
    }
}
