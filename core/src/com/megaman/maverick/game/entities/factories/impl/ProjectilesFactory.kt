package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.IProjectileEntity
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.JoeBall

class ProjectilesFactory(game: MegamanMaverickGame) : IFactory<IGameEntity> {

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
    pools.put(BULLET, EntityPoolCreator.create(100) { Bullet(game) })
    pools.put(CHARGED_SHOT, EntityPoolCreator.create(5) { ChargedShot(game) })
    pools.put(FIREBALL, EntityPoolCreator.create(3) { Fireball(game) })
    pools.put(JOEBALL, EntityPoolCreator.create(3) { JoeBall(game) })
  }

  override fun fetch(key: Any) = pools.get(if (key == "") BULLET else key)?.fetch()
}