package com.mega.game.engine.motion

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

class SineWave(var position: Vector2, var speed: Float, var amplitude: Float, var frequency: Float) : IMotion {

    private var elapsedTime = 0f

    override fun getMotionValue(out: Vector2): Vector2 = out.set(position)

    override fun update(delta: Float) {
        elapsedTime += delta
        position.x += speed * delta
        position.y += amplitude * MathUtils.sin(frequency * elapsedTime)
    }

    override fun reset() {
        elapsedTime = 0f
    }
}