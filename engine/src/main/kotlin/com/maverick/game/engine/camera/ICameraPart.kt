package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera

interface ICameraPart {

    fun update(camera: Camera, delta: Float): Boolean
}
