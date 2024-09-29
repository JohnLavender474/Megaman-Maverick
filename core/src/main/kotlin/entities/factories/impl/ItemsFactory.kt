package com.megaman.maverick.game.entities.factories.impl

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.entities.items.HealthBulb
import com.megaman.maverick.game.entities.items.HeartTank

class ItemsFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val HEALTH_BULB = "HealthBulb"
        const val HEART_TANK = "HeartTank"
        const val ARMOR_PIECE = "ArmorPiece"
        const val HEALTH_TANK = "HealthTank"
    }

    override fun init() {
        pools.put(HEALTH_BULB, GameEntityPoolCreator.create { HealthBulb(game) })
        pools.put(HEART_TANK, GameEntityPoolCreator.create { HeartTank(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
