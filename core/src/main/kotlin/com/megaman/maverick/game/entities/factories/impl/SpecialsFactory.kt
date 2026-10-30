package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.entities.sensors.FixtureTypeOverlapSpawn
import com.megaman.maverick.game.entities.special.*

class SpecialsFactory(private val services: MegaServices) : EntityFactory() {

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
        const val EVENT_TRIGGER = "EventTrigger"
        const val DECORATED_FORCE_GRAVITY = "DecoratedForceGravity"
        const val TOXIC_WATER = "ToxicWater"
        const val FLOOR_BUTTON = "FloorButton"
        const val GRAVITY_SWITCHAROO = "GravitySwitcharoo"
        const val GROUND_SNOW = "GroundSnow"
    }

    override fun init(vararg params: Any) {
        pools.put(WATER, GameEntityPoolCreator.create { Water(services) })
        pools.put(POLYGON_WATER, GameEntityPoolCreator.create { PolygonWater(services) })
        pools.put(SPRING_BOUNCER, GameEntityPoolCreator.create { SpringBouncer(services) })
        pools.put(LADDER, GameEntityPoolCreator.create { Ladder(services) })
        pools.put(GRAVITY_CHANGE, GameEntityPoolCreator.create { GravityChange(services) })
        pools.put(DISAPPEARING_BLOCKS, GameEntityPoolCreator.create { DisappearingBlocks(services) })
        pools.put(CART, GameEntityPoolCreator.create { Cart(services) })
        pools.put(ROTATION_ANCHOR, GameEntityPoolCreator.create { RotationAnchor(services) })
        pools.put(PORTAL_HOPPER, GameEntityPoolCreator.create { PortalHopper(services) })
        pools.put(FIXTURE_TYPE_OVERLAP_SPAWN, GameEntityPoolCreator.create { FixtureTypeOverlapSpawn(services) })
        pools.put(DARKNESS, GameEntityPoolCreator.create { /* Darkness(game) */ DarknessV2(services) })
        pools.put(FLIPPER_PLATFORM, GameEntityPoolCreator.create { FlipperPlatform(services) })
        pools.put(RAIL_TRACK, GameEntityPoolCreator.create { RailTrack(services) })
        pools.put(FORCE, GameEntityPoolCreator.create { Force(services) })
        pools.put(TOGGLEE, GameEntityPoolCreator.create { Togglee(services) })
        pools.put(QUICK_SAND, GameEntityPoolCreator.create { QuickSand(services) })
        pools.put(CAPSULE_TELEPORTER, GameEntityPoolCreator.create { CapsuleTeleporter(services) })
        pools.put(ROOM_SHAKER, GameEntityPoolCreator.create { RoomShaker(services) })
        pools.put(EVENT_TRIGGER, GameEntityPoolCreator.create { EventTrigger(services) })
        pools.put(DECORATED_FORCE_GRAVITY, GameEntityPoolCreator.create { DecoratedGravityForce(services) })
        pools.put(TOXIC_WATER, GameEntityPoolCreator.create { ToxicWater(services) })
        pools.put(FLOOR_BUTTON, GameEntityPoolCreator.create { FloorButton(services) })
        pools.put(GRAVITY_SWITCHAROO, GameEntityPoolCreator.create { GravitySwitcharoo(services) })
        pools.put(GROUND_SNOW, GameEntityPoolCreator.create { GroundSnow(services) })
    }

    override fun fetch(key: Any?) = pools.get(key)?.fetch()
}
