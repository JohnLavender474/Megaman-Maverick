package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.decorations.SmokePuff
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class DecorationsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

    companion object {
        const val SPLASH = "Splash"
        const val SMOKE_PUFF = "SmokePuff"
    }

    private val pools = ObjectMap<Any, Pool<IGameEntity>>()

    init {
        pools.put(SPLASH, EntityPoolCreator.create(5) { Splash(game) })
        pools.put(SMOKE_PUFF, EntityPoolCreator.create(5) { SmokePuff(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
