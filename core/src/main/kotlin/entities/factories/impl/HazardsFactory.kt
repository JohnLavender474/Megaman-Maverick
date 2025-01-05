package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.entities.hazards.*

class HazardsFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val SAW = "Saw"
        const val LASER_BEAMER = "LaserBeamer"
        const val SWINGING_AXE = "SwingingAxe"
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
        const val LAVA_BEAM = "LavaBeam"
        const val LAVA_BEAMER = "LavaBeamer"
        const val SMALL_ICE_CUBE = "SmallIceCube"
        const val ICE_CUBE_MAKER = "IceCubeMaker"
        const val UNDERWATER_FAN = "UnderwaterFan"
        const val SEA_MINE = "SeaMine"
        const val CACTUS = "Cactus"
        const val MAGMA_FLAME = "MagmaFlame"
        const val DEADLY_LEAF = "DeadlyLeaf"
        const val LAVA_RIVER = "LavaRiver"
        const val INFERNO_OVEN = "InfernoOven"
    }

    override fun init() {
        pools.put(SAW, GameEntityPoolCreator.create { Saw(game) })
        pools.put(LASER_BEAMER, GameEntityPoolCreator.create { LaserBeamer(game) })
        pools.put(SWINGING_AXE, GameEntityPoolCreator.create { SwingingAxe(game) })
        pools.put(SPIKE, GameEntityPoolCreator.create { Spike(game) })
        pools.put(BOLT, GameEntityPoolCreator.create { Bolt(game) })
        pools.put(ELECTROCUTIE, GameEntityPoolCreator.create { Electrocutie(game) })
        pools.put(ELECTROCUTIE_CHILD, GameEntityPoolCreator.create { ElectrocutieChild(game) })
        pools.put(SPIKE_BALL, GameEntityPoolCreator.create { SpikeBall(game) })
        pools.put(WANAAN_LAUNCHER, GameEntityPoolCreator.create { WanaanLauncher(game) })
        pools.put(CEILING_CRUSHER, GameEntityPoolCreator.create { CeilingCrusher(game) })
        pools.put(ACID_GOOP, GameEntityPoolCreator.create { AcidGoop(game) })
        pools.put(ACID_GOOP_SUPPLIER, GameEntityPoolCreator.create { AcidGoopSupplier(game) })
        pools.put(TUBE_BEAMER, GameEntityPoolCreator.create { TubeBeamer(game) })
        pools.put(LAVA, GameEntityPoolCreator.create { Lava(game) })
        pools.put(FLAME_THROWER, GameEntityPoolCreator.create { FlameThrower(game) })
        pools.put(FIREBALL_BAR, GameEntityPoolCreator.create { FireballBar(game) })
        pools.put(LAVA_DROP, GameEntityPoolCreator.create { LavaDrop(game) })
        pools.put(LAVA_DROP_SUPPLIER, GameEntityPoolCreator.create { LavaDropSupplier(game) })
        pools.put(ASTEROIDS_SPAWNER, GameEntityPoolCreator.create { AsteroidsSpawner(game) })
        pools.put(LAVA_BEAM, GameEntityPoolCreator.create { LavaBeam(game) })
        pools.put(LAVA_BEAMER, GameEntityPoolCreator.create { LavaBeamer(game) })
        pools.put(SMALL_ICE_CUBE, GameEntityPoolCreator.create { SmallIceCube(game) })
        pools.put(ICE_CUBE_MAKER, GameEntityPoolCreator.create { IceCubeMaker(game) })
        pools.put(UNDERWATER_FAN, GameEntityPoolCreator.create { UnderwaterFan(game) })
        pools.put(SEA_MINE, GameEntityPoolCreator.create { SeaMine(game) })
        pools.put(CACTUS, GameEntityPoolCreator.create { Cactus(game) })
        pools.put(MAGMA_FLAME, GameEntityPoolCreator.create { MagmaFlame(game) })
        pools.put(DEADLY_LEAF, GameEntityPoolCreator.create { DeadlyLeaf(game) })
        pools.put(LAVA_RIVER, GameEntityPoolCreator.create { LavaRiver(game) })
        pools.put(INFERNO_OVEN, GameEntityPoolCreator.create { InfernoOven(game) })
    }

    override fun fetch(key: Any?) = pools.get(key)?.fetch()
}
