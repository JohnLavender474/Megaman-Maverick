package com.megaman.maverick.game.utils.misc

import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.world.body.Body
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

object HeadUtils {

    fun stopJumpingIfHitHead(body: Body) {
        if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) &&
            when (body.direction) {
                Direction.UP -> body.physics.velocity.y > 0f
                Direction.DOWN -> body.physics.velocity.y < 0f
                Direction.LEFT -> body.physics.velocity.x < 0f
                Direction.RIGHT -> body.physics.velocity.x > 0f
            }
        ) when (body.direction) {
            Direction.UP, Direction.DOWN -> body.physics.velocity.y = 0f
            Direction.LEFT, Direction.RIGHT -> body.physics.velocity.x = 0f
        }
    }
}
