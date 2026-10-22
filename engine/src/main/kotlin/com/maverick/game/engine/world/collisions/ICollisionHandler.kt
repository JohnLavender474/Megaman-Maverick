package com.maverick.game.engine.world.collisions

import com.maverick.game.engine.world.body.IBody

interface ICollisionHandler {

    fun handleCollision(body1: IBody, body2: IBody): Boolean
}
