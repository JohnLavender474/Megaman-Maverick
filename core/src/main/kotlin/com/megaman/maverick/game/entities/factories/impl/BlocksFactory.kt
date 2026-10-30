package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.blocks.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.entities.special.RailTrackPlatform

class BlocksFactory(private val services: MegaServices) : EntityFactory() {

    companion object {
        const val TAG = "BlockFactory"
        const val STANDARD = "Standard"
        const val ANIMATED_BLOCK = "AnimatedBlock"
        const val ICE_BLOCK = "IceBlock"
        const val GEAR_TROLLEY = "GearTrolley"
        const val CONVEYOR_BELT = "ConveyorBelt"
        const val ROCKET_PLATFORM = "RocketPlatform"
        const val DROPPER_LIFT = "DropperLift"
        const val LIFT = "Lift"
        const val PROPELLER_PLATFORM = "PropellerPlatform"
        const val SWINGING_PLATFORM = "SwingingPlatform"
        const val RAIL_TRACK_PLATFORM = "RailTrackPlatform"
        const val BREAKABLE_ICE = "BreakableIce"
        const val FEET_RISE_SINK_BLOCK = "FeetRiseSinkBlock"
        const val LADDER_TOP = "LadderTop"
        const val DESTROYABLE_WOOD_PLANK = "DestroyableWoodPlank"
        const val PUSHABLE_BLOCK = "PushableBlock"
        const val SWITCH_GATE = "SwitchGate"
        const val GRAVITY_BLOCK = "GravityBlock"
        const val INFERNO_CHAIN_PLATFORM = "InfernoChainPlatform"
    }

    override fun init(vararg params: Any) {
        pools.put(STANDARD, GameEntityPoolCreator.create { Block(services) })
        pools.put(ANIMATED_BLOCK, GameEntityPoolCreator.create { AnimatedBlock(services) })
        pools.put(ICE_BLOCK, GameEntityPoolCreator.create { IceBlock(services) })
        pools.put(GEAR_TROLLEY, GameEntityPoolCreator.create { GearTrolley(services) })
        pools.put(CONVEYOR_BELT, GameEntityPoolCreator.create { ConveyorBelt(services) })
        pools.put(ROCKET_PLATFORM, GameEntityPoolCreator.create { RocketPlatform(services) })
        pools.put(DROPPER_LIFT, GameEntityPoolCreator.create { DropperLift(services) })
        pools.put(LIFT, GameEntityPoolCreator.create { Lift(services) })
        pools.put(PROPELLER_PLATFORM, GameEntityPoolCreator.create { PropellerPlatform(services) })
        pools.put(SWINGING_PLATFORM, GameEntityPoolCreator.create { SwingingPlatform(services) })
        pools.put(RAIL_TRACK_PLATFORM, GameEntityPoolCreator.create { RailTrackPlatform(services) })
        pools.put(BREAKABLE_ICE, GameEntityPoolCreator.create { BreakableIce(services) })
        pools.put(FEET_RISE_SINK_BLOCK, GameEntityPoolCreator.create { FeetRiseSinkBlock(services) })
        pools.put(LADDER_TOP, GameEntityPoolCreator.create { LadderTop(services) })
        pools.put(DESTROYABLE_WOOD_PLANK, GameEntityPoolCreator.create { WoodPlank(services) })
        pools.put(PUSHABLE_BLOCK, GameEntityPoolCreator.create { PushableBlock(services) })
        pools.put(SWITCH_GATE, GameEntityPoolCreator.create { SwitchGate(services) })
        pools.put(GRAVITY_BLOCK, GameEntityPoolCreator.create { GravityBlock(services) })
        pools.put(INFERNO_CHAIN_PLATFORM, GameEntityPoolCreator.create { InfernoChainPlatform(services) })
    }

    override fun fetch(key: Any?) =
        pools.get(if (key == null || key == "" || key.toString().lowercase() == "block") STANDARD else key)?.fetch()
}
