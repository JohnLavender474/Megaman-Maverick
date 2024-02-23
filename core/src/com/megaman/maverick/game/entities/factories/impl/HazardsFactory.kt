package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.hazards.*

class HazardsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

    companion object {
        const val SAW = "Saw"
        const val LASER_BEAMER = "LaserBeamer"
        const val SWINGIN_AXE = "SwinginAxe"
        const val SPIKE = "Spike"
        const val BOLT = "Bolt"
        const val ELECTROCUTIE = "Electrocutie"
        const val ELECTROCUTIE_CHILD = "ElectrocutieChild"
        const val SPIKE_BALL = "SpikeBall"
    }

    private val pools = ObjectMap<Any, Pool<IGameEntity>>()

    init {
        pools.put(SAW, EntityPoolCreator.create(2) { Saw(game) })
        pools.put(LASER_BEAMER, EntityPoolCreator.create(2) { LaserBeamer(game) })
        pools.put(SWINGIN_AXE, EntityPoolCreator.create(2) { SwinginAxe(game) })
        pools.put(SPIKE, EntityPoolCreator.create(10) { Spike(game) })
        pools.put(BOLT, EntityPoolCreator.create(2) { Bolt(game) })
        pools.put(ELECTROCUTIE, EntityPoolCreator.create(4) { Electrocutie(game) })
        pools.put(ELECTROCUTIE_CHILD, EntityPoolCreator.create(4) { ElectrocutieChild(game) })
        pools.put(SPIKE_BALL, EntityPoolCreator.create(2) { SpikeBall(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
