package com.megaman.maverick.game.entities.factories

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.ObjectMap
import com.engine.IGame2D
import com.engine.common.objects.Pool
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.entities.blocks.Block

/** A factory that creates blocks. */
class BlockFactory(private val game: IGame2D) : IFactory<IGameEntity> {

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
    pools.put(
        STANDARD,
        EntityPoolCreator.create(
            10,
        ) {
          Block(game)
        })
  }

  override fun fetch(key: Any, props: Properties): Block? {
    Gdx.app.debug(TAG, "Spawning Block: key = $key, props = $props")
    return pools.get(if ((key) == "") STANDARD else key)?.fetch()
  }
}

/**
 * Returns a standard [Block].
 *
 * @return A standard [Block].
 */
fun BlockFactory.standard() = fetch(BlockFactory.STANDARD, props())

/**
 * Returns an [IceBlock].
 *
 * @return An [IceBlock].
 */
fun BlockFactory.iceBlock() = fetch(BlockFactory.ICE_BLOCK, props())

/**
 * Returns a [GearTrolley].
 *
 * @return A [GearTrolley].
 */
fun BlockFactory.gearTrolley() = fetch(BlockFactory.GEAR_TROLLEY, props())

/**
 * Returns a [ConveyorBelt].
 *
 * @return A [ConveyorBelt].
 */
fun BlockFactory.conveyorBelt() = fetch(BlockFactory.CONVEYOR_BELT, props())

/**
 * Returns a [RocketPlatform].
 *
 * @return A [RocketPlatform].
 */
fun BlockFactory.rocketPlatform() = fetch(BlockFactory.ROCKET_PLATFORM, props())
