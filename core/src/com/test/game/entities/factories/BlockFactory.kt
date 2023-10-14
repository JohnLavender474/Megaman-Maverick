package com.test.game.entities.factories

import com.badlogic.gdx.utils.ObjectMap
import com.engine.IGame2D
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.test.game.entities.blocks.Block

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
    pools.put(
        STANDARD,
        EntityPoolCreator.create(
            10,
        ) {
          Block(game)
        })
  }

  override fun fetch(key: Any): Block? = pools.get(key)?.fetch()
}
