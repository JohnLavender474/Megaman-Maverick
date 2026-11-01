package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.ICameraPart

class CameraRoomClamp(private val stage: CameraLimitStage) : ICameraPart<RotatableCamera> {

    override fun update(camera: RotatableCamera, delta: Float): Boolean {
        if (!stage.active) return false

        camera.position.y = stage.limits.y.clamp(camera.position.y, camera.blendedViewHeight)
        camera.position.x = stage.limits.x.clamp(camera.position.x, camera.blendedViewWidth)

        return false
    }
}
