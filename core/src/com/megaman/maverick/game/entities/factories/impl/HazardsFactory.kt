package com.megaman.maverick.game.entities.factories.impl

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
        const val ACID_GOOP = "AcidGoop"
        const val ACID_GOOP_SUPPLIER = "AcidGoopSupplier"
        const val TUBE_BEAMER = "TubeBeamer"
        const val LAVA = "Lava"
        const val FLAME_THROWER = "FlameThrower"
        const val FIREBALL_BAR = "FireballBar"
        const val LAVA_DROP = "LavaDrop"
        const val LAVA_DROP_SUPPLIER = "LavaDropSupplier"
        const val ASTEROIDS_SPAWNER = "AsteroidsSpawner"
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
        pools.put(ACID_GOOP, EntityPoolCreator.create { AcidGoop(game) })
        pools.put(ACID_GOOP_SUPPLIER, EntityPoolCreator.create { AcidGoopSupplier(game) })
        pools.put(TUBE_BEAMER, EntityPoolCreator.create { TubeBeamer(game) })
        pools.put(LAVA, EntityPoolCreator.create { Lava(game) })
        pools.put(FLAME_THROWER, EntityPoolCreator.create { FlameThrower(game) })
        pools.put(FIREBALL_BAR, EntityPoolCreator.create { FireballBar(game) })
        pools.put(LAVA_DROP, EntityPoolCreator.create { LavaDrop(game) })
        pools.put(LAVA_DROP_SUPPLIER, EntityPoolCreator.create { LavaDropSupplier(game) })
        pools.put(ASTEROIDS_SPAWNER, EntityPoolCreator.create { AsteroidsSpawner(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
