package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.CameraRoomBounds
import com.badlogic.gdx.graphics.Camera
import com.maverick.game.engine.camera.ICameraPart
import com.megaman.maverick.game.world.wrap.isWrapRoom

class CameraRoomScan(private val roomBounds: CameraRoomBounds) : ICameraPart {

    override fun update(camera: Camera, delta: Float): Boolean {
        // TODO: a wrap room has no room transitions; revisit in item 111
        if (roomBounds.currentGameRoom?.isWrapRoom() == true) return false
        roomBounds.scanForNextRoom(camera)
        return false
    }
}
