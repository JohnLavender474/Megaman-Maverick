package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.ConstVals

class ShockWaveRow {

    companion object {
        const val FIRST_OFFSET = 1f
        const val SPACING = 1f
        const val GAP = 0.08f
    }

    private class Wave(var nextX: Float, var direction: Float, var timer: Float, var age: Float, val life: Float)

    private val waves = Array<Wave>()

    fun start(x: Float, direction: Float, life: Float) {
        waves.add(Wave(x + direction * FIRST_OFFSET * ConstVals.PPM, direction, 0f, 0f, life))
    }

    fun update(delta: Float, minX: Float, maxX: Float, spawn: (Float) -> Unit) {
        val iter = waves.iterator()
        while (iter.hasNext()) {
            val wave = iter.next()
            wave.timer -= delta
            wave.age += delta
            while (wave.timer <= 0f && wave.nextX in minX..maxX) {
                spawn(wave.nextX)
                wave.nextX += wave.direction * SPACING * ConstVals.PPM
                wave.timer += GAP
            }
            if (wave.age >= wave.life || wave.nextX !in minX..maxX) iter.remove()
        }
    }

    fun clear() = waves.clear()
}
