package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.GameLogger
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.IProjectileEntity
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot

/** A factory that creates projectiles. */
class ProjectileFactory(game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val TAG = "ProjectileFactory"

    const val BULLET = "Bullet"
    const val PICKET = "Picket"
    const val JOEBALL = "Joeball"
    const val FIREBALL = "Fireball"
    const val SNOWBALL = "Snowball"
    const val CHARGED_SHOT = "ChargedShot"
    const val PRECIOUS_SHOT = "PreciousShot"
  }

  private val pools = ObjectMap<Any, Pool<IProjectileEntity>>()

  init {
    // bullet
    pools.put(BULLET, EntityPoolCreator.create(10) { Bullet(game) })
    // charged shot
    pools.put(CHARGED_SHOT, EntityPoolCreator.create(5) { ChargedShot(game) })
  }

  /**
   * Fetches a projectile from the pool.
   *
   * @param key The key of the projectile to fetch.
   * @return A projectile from the pool.
   */
  override fun fetch(key: Any): IProjectileEntity? {
    GameLogger.debug(TAG, "Spawning Projectile: key = $key")
    return pools.get(if (key == "") BULLET else key)?.fetch()
  }
}
