package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.utils.SineUtils

class SineWave(var position: Vector2, var speed: Float, var amplitude: Float, var frequency: Float) : IMotion {

    private var elapsedTime = 0f
    private val origin = Vector2(position)

    override fun getMotionValue(out: Vector2): Vector2 = out.set(position)

    override fun update(delta: Float) {
        elapsedTime += delta
        position.x = origin.x + speed * elapsedTime
        position.y = origin.y + SineUtils.y(amplitude, frequency, elapsedTime)
    }

    override fun reset() {
        elapsedTime = 0f
        position.set(origin)
    }
}
