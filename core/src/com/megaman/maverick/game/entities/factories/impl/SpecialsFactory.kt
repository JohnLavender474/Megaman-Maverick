package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.sensors.FixtureTypeOverlapSpawn
import com.megaman.maverick.game.entities.special.*

class SpecialsFactory(private val game: MegamanMaverickGame) : EntityFactory() {

    companion object {
        const val WATER = "Water"
        const val POLYGON_WATER = "PolygonWater"
        const val LADDER = "Ladder"
        const val GRAVITY_CHANGE = "GravityChange"
        const val SPRING_BOUNCER = "SpringBouncer"
        const val DISAPPEARING_BLOCKS = "DisappearingBlocks"
        const val CART = "Cart"
        const val ROTATION_ANCHOR = "RotationAnchor"
        const val PORTAL_HOPPER = "PortalHopper"
        const val FIXTURE_TYPE_OVERLAP_SPAWN = "FixtureTypeOverlapSpawn"
        const val DARKNESS = "Darkness"
        const val FLIPPER_PLATFORM = "FlipperPlatform"
        const val RAIL_TRACK = "RailTrack"
        const val FORCE = "Force"
        const val TOGGLEE = "Togglee"
        const val QUICK_SAND = "QuickSand"
        const val CAPSULE_TELEPORTER = "CapsuleTeleporter"
    }

    override fun init() {
        pools.put(WATER, EntityPoolCreator.create { Water(game) })
        pools.put(POLYGON_WATER, EntityPoolCreator.create { PolygonWater(game) })
        pools.put(SPRING_BOUNCER, EntityPoolCreator.create { SpringBouncer(game) })
        pools.put(LADDER, EntityPoolCreator.create { Ladder(game) })
        pools.put(GRAVITY_CHANGE, EntityPoolCreator.create { GravityChange(game) })
        pools.put(DISAPPEARING_BLOCKS, EntityPoolCreator.create { DisappearingBlocks(game) })
        pools.put(CART, EntityPoolCreator.create { Cart(game) })
        pools.put(ROTATION_ANCHOR, EntityPoolCreator.create { RotationAnchor(game) })
        pools.put(PORTAL_HOPPER, EntityPoolCreator.create { PortalHopper(game) })
        pools.put(FIXTURE_TYPE_OVERLAP_SPAWN, EntityPoolCreator.create { FixtureTypeOverlapSpawn(game) })
        pools.put(DARKNESS, EntityPoolCreator.create { Darkness(game) })
        pools.put(FLIPPER_PLATFORM, EntityPoolCreator.create { FlipperPlatform(game) })
        pools.put(RAIL_TRACK, EntityPoolCreator.create { RailTrack(game) })
        pools.put(FORCE, EntityPoolCreator.create { Force(game) })
        pools.put(TOGGLEE, EntityPoolCreator.create { Togglee(game) })
        pools.put(QUICK_SAND, EntityPoolCreator.create { QuickSand(game) })
        pools.put(CAPSULE_TELEPORTER, EntityPoolCreator.create { CapsuleTeleporter(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
