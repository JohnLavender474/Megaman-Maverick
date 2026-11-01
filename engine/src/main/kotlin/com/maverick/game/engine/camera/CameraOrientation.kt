package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera

class CameraOrientation(private val camera: Camera) {

    private var writtenAngle = 0f

    fun write(angle: Float) {
        if (angle == writtenAngle) return
        camera.up.set(0f, 1f, 0f).rotate(camera.direction, angle)
        writtenAngle = angle
    }
}
