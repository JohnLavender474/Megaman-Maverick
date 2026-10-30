package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.bosses.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator

class BossesFactory(private val services: MegaServices) : EntityFactory() {

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

    override fun init(vararg params: Any) {
        // pools.put(REACTOR_MAN, GameEntityPoolCreator.create { ReactorMan_OLD(game) })
        pools.put(GLACIER_MAN, GameEntityPoolCreator.create { GlacierMan(services) })
        pools.put(DESERT_MAN, GameEntityPoolCreator.create { DesertMan(services) })
        pools.put(INFERNO_MAN, GameEntityPoolCreator.create { InfernoMan(services) })
        pools.put(MOON_MAN, GameEntityPoolCreator.create { MoonMan(services) })
        pools.put(TIMBER_WOMAN, GameEntityPoolCreator.create { TimberWoman(services) })
        pools.put(BOSPIDER, GameEntityPoolCreator.create { Bospider(services) })
        pools.put(GUTS_TANK, GameEntityPoolCreator.create { GutsTank(services) })
        pools.put(SIGMA_RAT, GameEntityPoolCreator.create { SigmaRat(services) })
        pools.put(PENGUIN_MINI_BOSS, GameEntityPoolCreator.create { PenguinMiniBoss(services) })
        pools.put(REACTOR_MONKEY_MINI_BOSS, GameEntityPoolCreator.create { ReactorMonkey(services) })
        pools.put(MOON_FACE_MINI_BOSS, GameEntityPoolCreator.create { MoonHead(services) })
        // pools.put(MECHA_DRAGON_MINI_BOSS, GameEntityPoolCreator.create { MechaDragon_OLD(game) })
        // pools.put(SPHINX_MINI_BOSS, GameEntityPoolCreator.create { SphinxMiniBoss(game) })
    }

    override fun fetch(key: Any?) = pools.get(key)?.fetch()
}
