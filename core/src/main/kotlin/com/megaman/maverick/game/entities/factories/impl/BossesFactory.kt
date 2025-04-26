package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.bosses.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator

class BossesFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val REACTOR_MAN = "ReactorMan"
        const val GLACIER_MAN = "GlacierMan"
        const val DESERT_MAN = "DesertMan"
        const val INFERNO_MAN = "InfernoMan"
        const val MOON_MAN = "MoonMan"
        const val TIMBER_WOMAN = "TimberWoman"
        const val BOSPIDER = "Bospider"
        const val GUTS_TANK = "GutsTank"
        const val SIGMA_RAT = "SigmaRat"
        const val PENGUIN_MINI_BOSS = "PenguinMiniBoss"
        const val REACTOR_MONKEY_MINI_BOSS = "ReactorMonkeyMiniBoss"
        const val MOON_FACE_MINI_BOSS = "MoonFaceMiniBoss"
        const val MECHA_DRAGON_MINI_BOSS = "MechaDragonMiniBoss"
        const val SPHINX_MINI_BOSS = "SphinxMiniBoss"
    }

    override fun init() {
        pools.put(REACTOR_MAN, GameEntityPoolCreator.create { ReactorMan_OLD(game) })
        pools.put(GLACIER_MAN, GameEntityPoolCreator.create { GlacierMan(game) })
        pools.put(DESERT_MAN, GameEntityPoolCreator.create { DesertMan(game) })
        pools.put(INFERNO_MAN, GameEntityPoolCreator.create { InfernoMan(game) })
        pools.put(MOON_MAN, GameEntityPoolCreator.create { MoonMan(game) })
        pools.put(TIMBER_WOMAN, GameEntityPoolCreator.create { TimberWoman(game) })
        pools.put(BOSPIDER, GameEntityPoolCreator.create { Bospider(game) })
        pools.put(GUTS_TANK, GameEntityPoolCreator.create { GutsTank(game) })
        pools.put(SIGMA_RAT, GameEntityPoolCreator.create { SigmaRat(game) })
        pools.put(PENGUIN_MINI_BOSS, GameEntityPoolCreator.create { PenguinMiniBoss(game) })
        pools.put(REACTOR_MONKEY_MINI_BOSS, GameEntityPoolCreator.create { ReactorMonkey(game) })
        pools.put(MOON_FACE_MINI_BOSS, GameEntityPoolCreator.create { MoonHead(game) })
        pools.put(MECHA_DRAGON_MINI_BOSS, GameEntityPoolCreator.create { MechaDragon_OLD(game) })
        pools.put(SPHINX_MINI_BOSS, GameEntityPoolCreator.create { SphinxMiniBoss(game) })
    }

    override fun fetch(key: Any?) = pools.get(key)?.fetch()
}
