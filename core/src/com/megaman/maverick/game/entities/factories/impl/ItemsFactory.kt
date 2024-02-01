package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.items.HealthBulb
import com.megaman.maverick.game.entities.items.HeartTank

class ItemsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

    companion object {
        const val HEALTH_BULB = "HealthBulb"
        const val HEART_TANK = "HeartTank"
        const val ARMOR_PIECE = "ArmorPiece"
        const val HEALTH_TANK = "HealthTank"
    }

    private val pools = ObjectMap<Any, Pool<IGameEntity>>()

    init {
        pools.put(HEALTH_BULB, EntityPoolCreator.create(5) { HealthBulb(game) })
        pools.put(HEART_TANK, EntityPoolCreator.create(1) { HeartTank(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
