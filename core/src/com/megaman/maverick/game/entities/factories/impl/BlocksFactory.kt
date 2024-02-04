package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.blocks.*
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class BlocksFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

    companion object {
        const val TAG = "BlockFactory"

        const val STANDARD = "Standard"
        const val ANIMATED_BLOCK = "AnimatedBlock"
        const val ICE_BLOCK = "IceBlock"
        const val GEAR_TROLLEY = "GearTrolley"
        const val CONVEYOR_BELT = "ConveyorBelt"
        const val ROCKET_PLATFORM = "RocketPlatform"
        const val DROPPER_LIFT = "DropperLift"
    }

    private val pools = ObjectMap<Any, Pool<Block>>()

    init {
        pools.put(STANDARD, EntityPoolCreator.create(10) { Block(game) })
        pools.put(ANIMATED_BLOCK, EntityPoolCreator.create(10) { AnimatedBlock(game) })
        pools.put(ICE_BLOCK, EntityPoolCreator.create(10) { IceBlock(game) })
        pools.put(GEAR_TROLLEY, EntityPoolCreator.create(5) { GearTrolley(game) })
        pools.put(CONVEYOR_BELT, EntityPoolCreator.create(5) { ConveyorBelt(game) })
        pools.put(ROCKET_PLATFORM, EntityPoolCreator.create(5) { RocketPlatform(game) })
        pools.put(DROPPER_LIFT, EntityPoolCreator.create(2) { DropperLift(game) })
    }

    override fun fetch(key: Any) = pools.get(if (key == "") STANDARD else key)?.fetch()
}
