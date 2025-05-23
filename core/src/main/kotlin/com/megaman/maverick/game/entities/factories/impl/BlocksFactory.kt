package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.blocks.*
import com.megaman.maverick.game.entities.factories.EntityFactory
import com.megaman.maverick.game.entities.factories.GameEntityPoolCreator
import com.megaman.maverick.game.entities.special.RailTrackPlatform

class BlocksFactory(private val game: MegamanMaverickGame) : EntityFactory() {

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

    override fun init() {
        pools.put(STANDARD, GameEntityPoolCreator.create { Block(game) })
        pools.put(ANIMATED_BLOCK, GameEntityPoolCreator.create { AnimatedBlock(game) })
        pools.put(ICE_BLOCK, GameEntityPoolCreator.create { IceBlock(game) })
        pools.put(GEAR_TROLLEY, GameEntityPoolCreator.create { GearTrolley(game) })
        pools.put(CONVEYOR_BELT, GameEntityPoolCreator.create { ConveyorBelt(game) })
        pools.put(ROCKET_PLATFORM, GameEntityPoolCreator.create { RocketPlatform(game) })
        pools.put(DROPPER_LIFT, GameEntityPoolCreator.create { DropperLift(game) })
        pools.put(LIFT, GameEntityPoolCreator.create { Lift(game) })
        pools.put(PROPELLER_PLATFORM, GameEntityPoolCreator.create { PropellerPlatform(game) })
        pools.put(SWINGING_PLATFORM, GameEntityPoolCreator.create { SwingingPlatform(game) })
        pools.put(RAIL_TRACK_PLATFORM, GameEntityPoolCreator.create { RailTrackPlatform(game) })
        pools.put(BREAKABLE_ICE, GameEntityPoolCreator.create { BreakableIce(game) })
        pools.put(FEET_RISE_SINK_BLOCK, GameEntityPoolCreator.create { FeetRiseSinkBlock(game) })
        pools.put(LADDER_TOP, GameEntityPoolCreator.create { LadderTop(game) })
        pools.put(DESTROYABLE_WOOD_PLANK, GameEntityPoolCreator.create { WoodPlank(game) })
        pools.put(PUSHABLE_BLOCK, GameEntityPoolCreator.create { PushableBlock(game) })
        pools.put(SWITCH_GATE, GameEntityPoolCreator.create { SwitchGate(game) })
        pools.put(GRAVITY_BLOCK, GameEntityPoolCreator.create { GravityBlock(game) })
        pools.put(INFERNO_CHAIN_PLATFORM, GameEntityPoolCreator.create { InfernoChainPlatform(game) })
    }

    override fun fetch(key: Any?) =
        pools.get(if (key == null || key == "" || key.toString().lowercase() == "block") STANDARD else key)?.fetch()
}
