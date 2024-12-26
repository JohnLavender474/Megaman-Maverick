package com.mega.game.engine.world.collisions

import com.mega.game.engine.world.body.IBody

interface ICollisionHandler {

    fun handleCollision(body1: IBody, body2: IBody): Boolean
}
