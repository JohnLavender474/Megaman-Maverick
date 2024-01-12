package com.megaman.maverick.game.entities.factories.impl

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Pool
import com.engine.entities.IGameEntity
import com.engine.factories.IFactory
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.factories.EntityPoolCreator
import com.megaman.maverick.game.entities.hazards.LaserBeamer
import com.megaman.maverick.game.entities.hazards.Saw
import com.megaman.maverick.game.entities.hazards.Spike
import com.megaman.maverick.game.entities.hazards.SwinginAxe

class HazardsFactory(private val game: MegamanMaverickGame) : IFactory<IGameEntity> {

  companion object {
    const val SAW = "Saw"
    const val LASER_BEAMER = "LaserBeamer"
    const val SWINGIN_AXE = "SwinginAxe"
    const val SPIKE = "Spike"
  }

  private val pools = ObjectMap<Any, Pool<IGameEntity>>()

  init {
    pools.put(SAW, EntityPoolCreator.create(2) { Saw(game) })
    pools.put(LASER_BEAMER, EntityPoolCreator.create(2) { LaserBeamer(game) })
    pools.put(SWINGIN_AXE, EntityPoolCreator.create(2) { SwinginAxe(game) })
    pools.put(SPIKE, EntityPoolCreator.create(10) { Spike(game) })
  }

  override fun fetch(key: Any) = pools.get(key)?.fetch()
}
