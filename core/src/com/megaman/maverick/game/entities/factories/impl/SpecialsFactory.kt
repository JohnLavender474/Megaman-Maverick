package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.special.Water

class SpecialsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val WATER = "Water"
    const val LADDER = "Ladder"
    const val UPSIDE_DOWN = "UpsideDown"
    const val SPRING_BOUNCER = "SpringBouncer"
  }

  private val pools = ObjectMap<Any, Pool<IGameEntity>>()

  init {
    pools.put(WATER, EntityPoolCreator.create(3) { Water(game) })
    /*
    pools.put(LADDER, EntityPoolCreator.create(10) { Ladder(game) })
    pools.put(UPSIDE_DOWN, EntityPoolCreator.create(10) { UpsideDown(game) })
    pools.put(SPRING_BOUNCER, EntityPoolCreator.create(10) { SpringBouncer(game) })
     */
  }

  override fun fetch(key: Any) = pools.get(key)?.fetch()
}
