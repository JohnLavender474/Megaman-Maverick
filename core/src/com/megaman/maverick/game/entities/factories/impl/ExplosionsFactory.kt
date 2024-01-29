package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.GameLogger
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.explosions.*
import com.megaman.maverick.game.entities.factories.EntityPoolCreator

class ExplosionsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val TAG = "ExplosionFactory"

    const val EXPLOSION = "Explosion"
    const val EXPLOSION_ORB = "ExplosionOrb"
    const val DISINTEGRATION = "Disintegration"
    const val SNOWBALL_EXPLOSION = "SnowballExplosion"
    const val PRECIOUS_EXPLOSION = "PreciousExplosion"
    const val CHARGED_SHOT_EXPLOSION = "ChargedShotExplosion"
    const val CAVE_ROCK_EXPLOSION = "CaveRockExplosion"
  }

  private val pools = ObjectMap<Any, Pool<IGameEntity>>()

  init {
    pools.put(EXPLOSION, EntityPoolCreator.create(5) { Explosion(game) })
    pools.put(SNOWBALL_EXPLOSION, EntityPoolCreator.create(3) { SnowballExplosion(game) })
    pools.put(DISINTEGRATION, EntityPoolCreator.create(10) { Disintegration(game) })
    pools.put(CHARGED_SHOT_EXPLOSION, EntityPoolCreator.create(3) { ChargedShotExplosion(game) })
    pools.put(EXPLOSION_ORB, EntityPoolCreator.create(8) { ExplosionOrb(game) })
    pools.put(CAVE_ROCK_EXPLOSION, EntityPoolCreator.create(2) { CaveRockExplosion(game) })
  }

  override fun fetch(key: Any): IGameEntity? {
    GameLogger.debug(TAG, "Spawning Explosion: key = $key")
    return pools.get(if (key == "") EXPLOSION else key)?.fetch()
  }
}
