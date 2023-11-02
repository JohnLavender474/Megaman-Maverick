package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.GameLogger
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

/** A factory that creates blocks. */
class BlockFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val TAG = "BlockFactory"

    const val STANDARD = "Standard"
    const val ICE_BLOCK = "IceBlock"
    const val GEAR_TROLLEY = "GearTrolley"
    const val CONVEYOR_BELT = "ConveyorBelt"
    const val ROCKET_PLATFORM = "RocketPlatform"
  }

  private val pools = ObjectMap<Any, Pool<Block>>()

  init {
    // standard blocks
    pools.put(STANDARD, EntityPoolCreator.create(10) { Block(game) })
  }

  /**
   * Fetches a block from the pool.
   *
   * @param key The key of the block to fetch.
   * @return A block from the pool.
   */
  override fun fetch(key: Any): IGameEntity? {
    GameLogger.debug(TAG, "Spawning Block: key = $key")
    return pools.get(if (key == "") STANDARD else key)?.fetch()
  }
}
