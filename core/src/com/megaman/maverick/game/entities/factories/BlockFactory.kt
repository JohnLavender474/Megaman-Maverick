package com.megaman.maverick.game.entities.factories

import com.badlogic.gdx.utils.ObjectMap
import com.engine.IGame2D
import com.engine.common.objects.Pool
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.entities.blocks.Block

class BlockFactory(private val game: IGame2D) : IFactory<IGameEntity> {

  companion object {
    const val STANDARD = "Standard"
    const val ICE_BLOCK = "IceBlock"
    const val GEAR_TROLLEY = "GearTrolley"
    const val CONVEYOR_BELT = "ConveyorBelt"
    const val ROCKET_PLATFORM = "RocketPlatform"
  }

  private val pools = ObjectMap<Any, Pool<Block>>()

  init {
    // standard blocks
    pools.put(
        STANDARD,
        EntityPoolCreator.create(
            10,
        ) {
          Block(game)
        })
  }

  override fun fetch(key: Any, props: Properties): Block? = pools.get(key)?.fetch()
}

fun BlockFactory.standard() = fetch(BlockFactory.STANDARD, props())

fun BlockFactory.iceBlock() = fetch(BlockFactory.ICE_BLOCK, props())

fun BlockFactory.gearTrolley() = fetch(BlockFactory.GEAR_TROLLEY, props())

fun BlockFactory.conveyorBelt() = fetch(BlockFactory.CONVEYOR_BELT, props())

fun BlockFactory.rocketPlatform() = fetch(BlockFactory.ROCKET_PLATFORM, props())
