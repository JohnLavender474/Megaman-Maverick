package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.GameLogger
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.enemies.Bat
import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

/** A factory that creates enemies. */
class EnemiesFactory(game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val TAG = "EnemiesFactory"

    const val MET = "Met"
    const val BAT = "Bat"
    const val RATTON = "Ratton"
    const val MAG_FLY = "MagFly"
    const val FLY_BOY = "FlyBoy"
    const val PENGUIN = "Penguin"
    const val SCREWIE = "Screwie"
    const val PICKET_JOE = "PicketJoe"
    const val SNIPER_JOE = "SniperJoe"
    const val PRECIOUS_JOE = "PreciousJoe"
    const val DRAGON_FLY = "Dragonfly"
    const val MATASABURO = "Matasaburo"
    const val SPRING_HEAD = "SpringHead"
    const val SWINGIN_JOE = "SwinginJoe"
    const val GAPING_FISH = "GapingFish"
    const val FLOATING_CAN = "FloatingCan"
    const val SUCTION_ROLLER = "SuctionRoller"
    const val SHIELD_ATTACKER = "ShieldAttacker"
  }

  private val pools = ObjectMap<Any, Pool<AbstractEnemy>>()

  init {
    // bat
    pools.put(BAT, EntityPoolCreator.create(5) { Bat(game) })
    // met
    pools.put(MET, EntityPoolCreator.create(5) { Met(game) })
  }

  /**
   * Fetches an enemy from the pool.
   *
   * @param key The key of the enemy to fetch.
   * @return An enemy from the pool.
   */
  override fun fetch(key: Any): AbstractEnemy? {
    GameLogger.debug(ExplosionsFactory.TAG, "Spawning Explosion: key = $key")
    return pools.get(key)?.fetch()
  }
}
