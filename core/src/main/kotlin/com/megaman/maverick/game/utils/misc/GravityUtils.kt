package com.megaman.maverick.game.utils.misc

import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.world.body.Body

object GravityUtils {

    fun setGravity(body: Body, value: Float) = body.physics.gravity.let { gravity ->
        when (body.direction) {
            Direction.UP -> gravity.set(0f, -value)
            Direction.DOWN -> gravity.set(0f, value)
            Direction.LEFT -> gravity.set(value, 0f)
            Direction.RIGHT -> gravity.set(-value, 0f)
        }
    }
}
