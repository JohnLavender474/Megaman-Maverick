package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.sensors.Death
import com.megaman.maverick.game.entities.sensors.Gate

class SensorsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

    companion object {
        const val TAG = "SensorsFactory"
        const val GATE = "Gate"
        const val DEATH = "Death"
    }

    private val pools = ObjectMap<Any, Pool<IGameEntity>>()

    init {
        pools.put(GATE, EntityPoolCreator.create(10) { Gate(game) })
        pools.put(DEATH, EntityPoolCreator.create(10) { Death(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
