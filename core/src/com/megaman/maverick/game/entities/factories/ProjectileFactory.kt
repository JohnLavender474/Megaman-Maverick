package com.megaman.maverick.game.entities.factories

import com.engine.common.objects.Properties
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame

class ProjectileFactory(game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val BULLET = "Bullet"
    const val PICKET = "Picket"
    const val JOEBALL = "Joeball"
    const val FIREBALL = "Fireball"
    const val SNOWBALL = "Snowball"
    const val CHARGED_SHOT = "ChargedShot"
    const val PRECIOUS_SHOT = "PreciousShot"
  }

  init {}

  override fun fetch(key: Any, props: Properties): IGameEntity? {

    TODO("Not yet implemented")
  }
}
