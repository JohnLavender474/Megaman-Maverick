package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.decorations.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.entities.hazards.DrippingToxicGoop
import com.megaman.maverick.game.entities.megaman.sprites.MegamanTrailSpriteV2

class DecorationsFactory(private val services: MegaServices) : EntityFactory() {

    companion object {
        const val SPLASH = "Splash"
        const val FORCE_DECORATION = "ForceDecoration"
        const val WINDY_GRASS = "WindyGrass"
        const val FALLING_LEAF = "FallingLeaf"
        const val FALLING_LEAVES = "FallingLeaves"
        const val TREE = "Tree"
        const val TREE_STUMP = "TreeStump"
        const val LAVA_FALL = "LavaFall"
        const val PIPI_EGG_SHATTER = "PipiEggShatter"
        const val MUZZLE_FLASH = "MuzzleFlash"
        const val UNDER_WATER_BUBBLE = "UnderWaterBubble"
        const val WHITE_ARROW = "WhiteArrow"
        const val WHITE_ARROW_POOL = "WhiteArrowPool"
        const val RAIN_DROP = "RainDrop"
        const val RAIN_FALL = "RainFall"
        const val DRIPPING_TOXIC_GOOP = "DrippingToxicGoop"
        const val TOXIC_WATERFALL = "ToxicWaterfall"
        const val LIGHT_SOURCE = "LightSource"
        const val LANTERN = "Lantern"
        const val SPACE_SATELLITE = "SpaceSatellite"
        const val MEGAMAN_TRAIL_SPRITE = "MegamanTrailSprite"
        const val MEGAMAN_TRAIL_SPRITE_V2 = "MegamanTrailSprite_v2"
        const val CHARGED_SHOT_RESIDUAL = "ChargedShotResidual"
        const val SNOW = "Snow"
        const val SNOW_FALL = "Snowfall"
        const val SNOW_FLUFF = "SnowFluff"
        const val AIR_CONDITIONER = "AirConditioner"
        const val CRYSTAL_CONVEYOR_BKG = "CrystalConveyorBKG"
        const val SMALL_GRASS = "SmallGrass"
        const val SMALL_GRASS_PATCH = "SmallGrassPatch"
        const val FURNACE = "Furnace"
        const val SMOKE = "Smoke"
    }

    override fun init(vararg params: Any) {
        pools.put(SPLASH, GameEntityPoolCreator.create { Splash(services) })
        pools.put(FORCE_DECORATION, GameEntityPoolCreator.create { ForceDecoration(services) })
        pools.put(WINDY_GRASS, GameEntityPoolCreator.create { WindyGrass(services) })
        pools.put(FALLING_LEAF, GameEntityPoolCreator.create { FallingLeaf(services) })
        pools.put(FALLING_LEAVES, GameEntityPoolCreator.create { FallingLeaves(services) })
        pools.put(TREE, GameEntityPoolCreator.create { Tree(services) })
        pools.put(TREE_STUMP, GameEntityPoolCreator.create { TreeStump(services) })
        pools.put(LAVA_FALL, GameEntityPoolCreator.create { LavaFall(services) })
        pools.put(PIPI_EGG_SHATTER, GameEntityPoolCreator.create { PipiEggShatter(services) })
        pools.put(MUZZLE_FLASH, GameEntityPoolCreator.create { MuzzleFlash(services) })
        pools.put(UNDER_WATER_BUBBLE, GameEntityPoolCreator.create { UnderWaterBubble(services) })
        pools.put(WHITE_ARROW, GameEntityPoolCreator.create { WhiteArrow(services) })
        pools.put(WHITE_ARROW_POOL, GameEntityPoolCreator.create { WhiteArrowPool(services) })
        pools.put(RAIN_DROP, GameEntityPoolCreator.create { RainDrop(services) })
        pools.put(RAIN_FALL, GameEntityPoolCreator.create { RainFall(services) })
        pools.put(DRIPPING_TOXIC_GOOP, GameEntityPoolCreator.create { DrippingToxicGoop(services) })
        pools.put(TOXIC_WATERFALL, GameEntityPoolCreator.create { ToxicWaterfall(services) })
        pools.put(LIGHT_SOURCE, GameEntityPoolCreator.create { LightSourceEntity(services) })
        pools.put(LANTERN, GameEntityPoolCreator.create { Lantern(services) })
        pools.put(SPACE_SATELLITE, GameEntityPoolCreator.create { SpaceSatellite(services) })
        // pools.put(MEGAMAN_TRAIL_SPRITE, GameEntityPoolCreator.create { MegamanTrailSprite(game) })
        pools.put(MEGAMAN_TRAIL_SPRITE_V2, GameEntityPoolCreator.create { MegamanTrailSpriteV2(services) })
        pools.put(CHARGED_SHOT_RESIDUAL, GameEntityPoolCreator.create { ChargedShotResidual(services) })
        pools.put(SNOW, GameEntityPoolCreator.create { Snow(services) })
        pools.put(SNOW_FALL, GameEntityPoolCreator.create { Snowfall(services) })
        pools.put(SNOW_FLUFF, GameEntityPoolCreator.create { SnowFluff(services) })
        pools.put(AIR_CONDITIONER, GameEntityPoolCreator.create { AirConditioner(services) })
        pools.put(CRYSTAL_CONVEYOR_BKG, GameEntityPoolCreator.create { CrystalConveyorBKG(services) })
        pools.put(SMALL_GRASS, GameEntityPoolCreator.create { SmallGrass(services) })
        pools.put(SMALL_GRASS_PATCH, GameEntityPoolCreator.create { SmallGrassPatch(services) })
        pools.put(FURNACE, GameEntityPoolCreator.create { Furnace(services) })
        pools.put(SMOKE, GameEntityPoolCreator.create { Smoke(services) })
    }

    override fun fetch(key: Any?) = pools.get(key)?.fetch()
}
