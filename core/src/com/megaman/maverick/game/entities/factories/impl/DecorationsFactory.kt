package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.decorations.ForceDecoration
import com.megaman.maverick.game.entities.decorations.SmokePuff
import com.megaman.maverick.game.entities.decorations.Splash
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class DecorationsFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val SPLASH = "Splash"
        const val SMOKE_PUFF = "SmokePuff"
        const val FORCE = "ForceDecoration"
    }

    override fun init() {
        pools.put(SPLASH, EntityPoolCreator.create { Splash(game) })
        pools.put(SMOKE_PUFF, EntityPoolCreator.create { SmokePuff(game) })
        pools.put(FORCE, EntityPoolCreator.create { ForceDecoration(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
