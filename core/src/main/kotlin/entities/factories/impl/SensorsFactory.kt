package com.megaman.maverick.game.entities.factories.impl


import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.entities.sensors.Death
import com.megaman.maverick.game.entities.sensors.Gate

class SensorsFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val TAG = "SensorsFactory"
        const val GATE = "Gate"
        const val DEATH = "Death"
    }

    override fun init() {
        pools.put(GATE, GameEntityPoolCreator.create { Gate(game) })
        pools.put(DEATH, GameEntityPoolCreator.create { Death(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
