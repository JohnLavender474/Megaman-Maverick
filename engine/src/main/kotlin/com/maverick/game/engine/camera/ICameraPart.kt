package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera

interface ICameraPart<in C : Camera> {

    fun update(camera: C, delta: Float): Boolean
}
