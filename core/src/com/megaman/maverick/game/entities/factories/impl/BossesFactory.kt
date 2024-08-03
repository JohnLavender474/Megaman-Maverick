package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.bosses.*
import com.megaman.maverick.game.entities.bosses.gutstank.GutsTank
import com.megaman.maverick.game.entities.bosses.sigmarat.SigmaRat
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class BossesFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val REACT_MAN = "ReactMan"
        const val BOSPIDER = "Bospider"
        const val GUTS_TANK = "GutsTank"
        const val SIGMA_RAT = "SigmaRat"
        const val PENGUIN_MINI_BOSS = "PenguinMiniBoss"
        const val REACTOR_MONKEY_MINI_BOSS = "ReactorMonkeyMiniBoss"
        const val MOON_FACE_MINI_BOSS = "MoonFaceMiniBoss"
    }

    override fun init() {
        pools.put(REACT_MAN, EntityPoolCreator.create { ReactMan(game) })
        pools.put(BOSPIDER, EntityPoolCreator.create { Bospider(game) })
        pools.put(GUTS_TANK, EntityPoolCreator.create { GutsTank(game) })
        pools.put(SIGMA_RAT, EntityPoolCreator.create { SigmaRat(game) })
        pools.put(PENGUIN_MINI_BOSS, EntityPoolCreator.create { PenguinMiniBoss(game) })
        pools.put(REACTOR_MONKEY_MINI_BOSS, EntityPoolCreator.create { ReactorMonkeyMiniBoss(game) })
        pools.put(MOON_FACE_MINI_BOSS, EntityPoolCreator.create { MoonHeadMiniBoss(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
