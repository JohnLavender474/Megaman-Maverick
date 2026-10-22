package com.megaman.maverick.game.utils.misc

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.world.body.Body

object GravityUtils {

    fun setGravity(body: Body, value: Float): Vector2 = body.physics.gravity.let { gravity ->
        when (body.direction) {
            Direction.UP -> gravity.set(0f, -value)
            Direction.DOWN -> gravity.set(0f, value)
            Direction.LEFT -> gravity.set(value, 0f)
            Direction.RIGHT -> gravity.set(-value, 0f)
        }
    }
}
