package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.decorations.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator

class DecorationsFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val SPLASH = "Splash"
        const val FORCE = "ForceDecoration"
        const val WINDY_GRASS = "WindyGrass"
        const val FALLING_LEAF = "FallingLeaf"
        const val TREE = "Tree"
        const val TREE_STUMP = "TreeStump"
        const val LAVA_FALL = "LavaFall"
        const val PIPI_EGG_SHATTER = "PipiEggShatter"
        const val MUZZLE_FLASH = "MuzzleFlash"
    }

    override fun init() {
        pools.put(SPLASH, GameEntityPoolCreator.create { Splash(game) })
        pools.put(FORCE, GameEntityPoolCreator.create { ForceDecoration(game) })
        pools.put(WINDY_GRASS, GameEntityPoolCreator.create { WindyGrass(game) })
        pools.put(FALLING_LEAF, GameEntityPoolCreator.create { FallingLeaf(game) })
        pools.put(TREE, GameEntityPoolCreator.create { Tree(game) })
        pools.put(TREE_STUMP, GameEntityPoolCreator.create { TreeStump(game) })
        pools.put(LAVA_FALL, GameEntityPoolCreator.create { LavaFall(game) })
        pools.put(PIPI_EGG_SHATTER, GameEntityPoolCreator.create { PipiEggShatter(game) })
        pools.put(MUZZLE_FLASH, GameEntityPoolCreator.create { MuzzleFlash(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
