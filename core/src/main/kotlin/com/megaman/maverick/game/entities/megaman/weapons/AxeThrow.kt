package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

internal object AxeThrow {

    fun ratio(holdTime: Float) = MathUtils.clamp(
        (holdTime - MegamanValues.AXE_WINDUP_TIME) / (MegamanValues.AXE_HOLD_TIME - MegamanValues.AXE_WINDUP_TIME),
        0f,
        1f
    )

    fun localImpulse(holdTime: Float, out: Vector2): Vector2 {
        val ratio = ratio(holdTime)
        return out.set(
            MathUtils.lerp(MegamanValues.AXE_IMPULSE_X_MIN, MegamanValues.AXE_IMPULSE_X_MAX, ratio),
            MathUtils.lerp(MegamanValues.AXE_IMPULSE_Y_MIN, MegamanValues.AXE_IMPULSE_Y_MAX, ratio)
        )
    }

    fun worldImpulse(holdTime: Float, direction: Direction, facing: Facing, slowed: Boolean, out: Vector2): Vector2 {
        localImpulse(holdTime, out)
        val x = out.x
        val y = out.y
        when (direction) {
            Direction.UP -> out.set(x * facing.value, y)
            Direction.DOWN -> out.set(x * -facing.value, -y)
            Direction.LEFT -> out.set(-y, x * facing.value)
            Direction.RIGHT -> out.set(y, x * -facing.value)
        }
        out.scl(ConstVals.PPM.toFloat())
        if (slowed) out.scl(MegamanValues.AXE_SLOW_SCALAR)
        return out
    }

    fun spawnPoint(center: Vector2, direction: Direction, facing: Facing, out: Vector2): Vector2 {
        out.set(center)
        when (direction) {
            Direction.UP -> out.add(-0.1f * ConstVals.PPM * facing.value, 0.75f * ConstVals.PPM)
            Direction.DOWN -> out.add(0.1f * ConstVals.PPM * facing.value, -0.75f * ConstVals.PPM)
            Direction.LEFT -> out.add(-0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM * facing.value)
            Direction.RIGHT -> out.add(0.75f * ConstVals.PPM, -0.1f * ConstVals.PPM * facing.value)
        }
        return out
    }

    fun slowed(megaman: Megaman) = megaman.services.levelState.currentLevel!! == LevelDefinition.MOON_MAN ||
        megaman.body.isSensing(BodySense.IN_WATER)

    fun launch(megaman: Megaman, holdTime: Float, outPosition: Vector2, outImpulse: Vector2) {
        val direction = megaman.direction
        val facing = megaman.facing
        megaman.body.getCenter(outPosition)
        spawnPoint(outPosition, direction, facing, outPosition)
        worldImpulse(holdTime, direction, facing, slowed(megaman), outImpulse)
    }
}
