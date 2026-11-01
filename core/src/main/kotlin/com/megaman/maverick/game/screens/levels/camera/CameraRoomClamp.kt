package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.Camera
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.camera.ICameraPart

class CameraRoomClamp(private val view: CameraView, private val stage: CameraLimitStage) : ICameraPart {

    override fun update(camera: Camera, delta: Float): Boolean {
        if (!stage.active) return false

        camera.position.y = stage.limits.y.clamp(camera.position.y, view.blendedViewHeight)
        camera.position.x = stage.limits.x.clamp(camera.position.x, view.blendedViewWidth)

        return false
    }
}
