package com.maverick.game.engine.camera

interface ICameraLimitRule {

    fun apply(target: CameraLimits)
}
