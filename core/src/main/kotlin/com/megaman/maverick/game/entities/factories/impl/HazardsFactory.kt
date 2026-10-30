package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.enemies.Cactus
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.entities.hazards.*

class HazardsFactory(private val services: MegaServices) : EntityFactory() {

    companion object {
        const val SAW = "Saw"
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
        const val ICE_CUBE_MAKER = "IceCubeMaker"
        const val UNDERWATER_FAN = "UnderwaterFan"
        const val SEA_MINE = "SeaMine"
        const val CACTUS = "Cactus"
        const val MAGMA_FLAME = "MagmaFlame"
        const val DEADLY_LEAF = "DeadlyLeaf"
        const val INFERNO_OVEN = "InfernoOven"
        const val INFERNO_METEOR_SHOWER = "InfernoMeteorShower"
        const val RISING_LAVA_RIVER = "RisingLavaRiver"
    }

    override fun init(vararg params: Any) {
        pools.put(SAW, GameEntityPoolCreator.create { Saw(services) })
        pools.put(SWINGING_AXE, GameEntityPoolCreator.create { SwingingAxe(services) })
        pools.put(SPIKE, GameEntityPoolCreator.create { Spike(services) })
        pools.put(BOLT, GameEntityPoolCreator.create { Bolt(services) })
        pools.put(ELECTROCUTIE, GameEntityPoolCreator.create { Electrocutie(services) })
        pools.put(ELECTROCUTIE_CHILD, GameEntityPoolCreator.create { ElectrocutieChild(services) })
        pools.put(SPIKE_BALL, GameEntityPoolCreator.create { SpikeBall(services) })
        pools.put(WANAAN_LAUNCHER, GameEntityPoolCreator.create { WanaanLauncher(services) })
        pools.put(CEILING_CRUSHER, GameEntityPoolCreator.create { CeilingCrusher(services) })
        pools.put(ACID_GOOP, GameEntityPoolCreator.create { AcidGoop(services) })
        pools.put(ACID_GOOP_SUPPLIER, GameEntityPoolCreator.create { AcidGoopSupplier(services) })
        pools.put(TUBE_BEAMER, GameEntityPoolCreator.create { TubeBeamer(services) })
        pools.put(LAVA, GameEntityPoolCreator.create { Lava(services) })
        pools.put(FLAME_THROWER, GameEntityPoolCreator.create { FlameThrower(services) })
        pools.put(FIREBALL_BAR, GameEntityPoolCreator.create { FireballBar(services) })
        pools.put(LAVA_DROP, GameEntityPoolCreator.create { LavaDrop(services) })
        pools.put(LAVA_DROP_SUPPLIER, GameEntityPoolCreator.create { LavaDropSupplier(services) })
        pools.put(ASTEROIDS_SPAWNER, GameEntityPoolCreator.create { AsteroidsSpawner(services) })
        pools.put(LAVA_BEAM, GameEntityPoolCreator.create { LavaBeam(services) })
        pools.put(LAVA_BEAMER, GameEntityPoolCreator.create { LavaBeamer(services) })
        pools.put(ICE_CUBE_MAKER, GameEntityPoolCreator.create { IceCubeMaker(services) })
        pools.put(UNDERWATER_FAN, GameEntityPoolCreator.create { UnderwaterFan(services) })
        pools.put(SEA_MINE, GameEntityPoolCreator.create { SeaMine(services) })
        pools.put(CACTUS, GameEntityPoolCreator.create { Cactus(services) })
        pools.put(MAGMA_FLAME, GameEntityPoolCreator.create { MagmaFlame(services) })
        pools.put(DEADLY_LEAF, GameEntityPoolCreator.create { DeadlyLeaf(services) })
        pools.put(INFERNO_OVEN, GameEntityPoolCreator.create { InfernoOven(services) })
        pools.put(INFERNO_METEOR_SHOWER, GameEntityPoolCreator.create { InfernoMeteorShower(services) })
        pools.put(RISING_LAVA_RIVER, GameEntityPoolCreator.create { RisingLavaRiver(services) })
    }

    override fun fetch(key: Any?) = pools.get(key)?.fetch()
}
