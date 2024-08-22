package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.bosses.*
import com.megaman.maverick.game.entities.bosses.gutstank.GutsTank
import com.megaman.maverick.game.entities.bosses.sigmarat.SigmaRat
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator

class BossesFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val REACT_MAN = "ReactMan"
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
        pools.put(REACT_MAN, GameEntityPoolCreator.create { ReactMan(game) })
        pools.put(BOSPIDER, GameEntityPoolCreator.create { Bospider(game) })
        pools.put(GUTS_TANK, GameEntityPoolCreator.create { GutsTank(game) })
        pools.put(SIGMA_RAT, GameEntityPoolCreator.create { SigmaRat(game) })
        pools.put(PENGUIN_MINI_BOSS, GameEntityPoolCreator.create { PenguinMiniBoss(game) })
        pools.put(REACTOR_MONKEY_MINI_BOSS, GameEntityPoolCreator.create { ReactorMonkeyMiniBoss(game) })
        pools.put(MOON_FACE_MINI_BOSS, GameEntityPoolCreator.create { MoonHeadMiniBoss(game) })
        pools.put(MECHA_DRAGON_MINI_BOSS, GameEntityPoolCreator.create { MechaDragonMiniBoss(game) })
        pools.put(SPHINX_MINI_BOSS, GameEntityPoolCreator.create { SphinxMiniBoss(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
