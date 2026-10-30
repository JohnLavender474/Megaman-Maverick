package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.ICameraPart

class CameraRotationStep : ICameraPart<RotatableCamera> {

    override fun update(camera: RotatableCamera, delta: Float): Boolean {
        camera.update(delta)
        return false
    }
}
