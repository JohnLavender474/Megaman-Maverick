package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.decorations.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class DecorationsFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val SPLASH = "Splash"
        const val SMOKE_PUFF = "SmokePuff"
        const val FORCE = "ForceDecoration"
        const val WINDY_GRASS = "WindyGrass"
        const val TREE = "Tree"
    }

    override fun init() {
        pools.put(SPLASH, EntityPoolCreator.create { Splash(game) })
        pools.put(SMOKE_PUFF, EntityPoolCreator.create { SmokePuff(game) })
        pools.put(FORCE, EntityPoolCreator.create { ForceDecoration(game) })
        pools.put(WINDY_GRASS, EntityPoolCreator.create { WindyGrass(game) })
        pools.put(TREE, EntityPoolCreator.create { Tree(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
