package com.maverick.game.engine.world

import com.maverick.game.engine.world.body.IBody

interface IWorldBodies {

    val bodyCount: Int

    fun getBody(index: Int): IBody
}
