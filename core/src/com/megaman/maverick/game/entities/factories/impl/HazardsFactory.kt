package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.hazards.*

class HazardsFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val SAW = "Saw"
        const val LASER_BEAMER = "LaserBeamer"
        const val SWINGIN_AXE = "SwinginAxe"
        const val SPIKE = "Spike"
        const val BOLT = "Bolt"
        const val ELECTROCUTIE = "Electrocutie"
        const val ELECTROCUTIE_CHILD = "ElectrocutieChild"
        const val SPIKE_BALL = "SpikeBall"
        const val WANAAN_LAUNCHER = "WanaanLauncher"
        const val CEILING_CRUSHER = "CeilingCrusher"
    }

    override fun init() {
        pools.put(SAW, EntityPoolCreator.create { Saw(game) })
        pools.put(LASER_BEAMER, EntityPoolCreator.create { LaserBeamer(game) })
        pools.put(SWINGIN_AXE, EntityPoolCreator.create { SwinginAxe(game) })
        pools.put(SPIKE, EntityPoolCreator.create { Spike(game) })
        pools.put(BOLT, EntityPoolCreator.create { Bolt(game) })
        pools.put(ELECTROCUTIE, EntityPoolCreator.create { Electrocutie(game) })
        pools.put(ELECTROCUTIE_CHILD, EntityPoolCreator.create { ElectrocutieChild(game) })
        pools.put(SPIKE_BALL, EntityPoolCreator.create { SpikeBall(game) })
        pools.put(WANAAN_LAUNCHER, EntityPoolCreator.create { WanaanLauncher(game) })
        pools.put(CEILING_CRUSHER, EntityPoolCreator.create { CeilingCrusher(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
