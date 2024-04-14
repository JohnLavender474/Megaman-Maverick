package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.bosses.Bospider
import com.megaman.maverick.game.entities.bosses.CrewMan
import com.megaman.maverick.game.entities.bosses.gutstank.GutsTank
import com.megaman.maverick.game.entities.bosses.sigmarat.SigmaRat
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class BossesFactory(game: MegamanMaverickGame) : IFactory<IGameEntity> {

    companion object {
        const val CREW_MAN = "CrewMan"
        const val BOSPIDER = "Bospider"
        const val GUTS_TANK = "GutsTank"
        const val SIGMA_RAT = "SigmaRat"
    }

    private val pools = ObjectMap<Any, Pool<IGameEntity>>()

    init {
        pools.put(CREW_MAN, EntityPoolCreator.create(1) { CrewMan(game) })
        pools.put(BOSPIDER, EntityPoolCreator.create(1) { Bospider(game) })
        pools.put(GUTS_TANK, EntityPoolCreator.create(1) { GutsTank(game) })
        pools.put(SIGMA_RAT, EntityPoolCreator.create(1) { SigmaRat(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
