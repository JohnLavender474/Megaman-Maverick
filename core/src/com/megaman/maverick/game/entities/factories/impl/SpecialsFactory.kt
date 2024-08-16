package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
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
        const val ROOM_SHAKER = "RoomShaker"
    }

    override fun init() {
        pools.put(WATER, GameEntityPoolCreator.create { Water(game) })
        pools.put(POLYGON_WATER, GameEntityPoolCreator.create { PolygonWater(game) })
        pools.put(SPRING_BOUNCER, GameEntityPoolCreator.create { SpringBouncer(game) })
        pools.put(LADDER, GameEntityPoolCreator.create { Ladder(game) })
        pools.put(GRAVITY_CHANGE, GameEntityPoolCreator.create { GravityChange(game) })
        pools.put(DISAPPEARING_BLOCKS, GameEntityPoolCreator.create { DisappearingBlocks(game) })
        pools.put(CART, GameEntityPoolCreator.create { Cart(game) })
        pools.put(ROTATION_ANCHOR, GameEntityPoolCreator.create { RotationAnchor(game) })
        pools.put(PORTAL_HOPPER, GameEntityPoolCreator.create { PortalHopper(game) })
        pools.put(FIXTURE_TYPE_OVERLAP_SPAWN, GameEntityPoolCreator.create { FixtureTypeOverlapSpawn(game) })
        pools.put(DARKNESS, GameEntityPoolCreator.create { Darkness(game) })
        pools.put(FLIPPER_PLATFORM, GameEntityPoolCreator.create { FlipperPlatform(game) })
        pools.put(RAIL_TRACK, GameEntityPoolCreator.create { RailTrack(game) })
        pools.put(FORCE, GameEntityPoolCreator.create { Force(game) })
        pools.put(TOGGLEE, GameEntityPoolCreator.create { Togglee(game) })
        pools.put(QUICK_SAND, GameEntityPoolCreator.create { QuickSand(game) })
        pools.put(CAPSULE_TELEPORTER, GameEntityPoolCreator.create { CapsuleTeleporter(game) })
        pools.put(ROOM_SHAKER, GameEntityPoolCreator.create { RoomShaker(game) })
    }

    override fun fetch(key: Any) = pools.get(key)?.fetch()
}
