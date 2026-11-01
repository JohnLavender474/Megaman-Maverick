package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.Camera
import com.maverick.game.engine.camera.ICameraPart

class CameraRoomScan(private val roomBounds: CameraRoomBounds) : ICameraPart {

    override fun update(camera: Camera, delta: Float): Boolean {
        roomBounds.scanForNextRoom(camera)
        return false
    }
}
