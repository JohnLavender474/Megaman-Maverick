package com.megaman.maverick.game.entities.blocks

import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.world.Fixture
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.FixtureType

/** An ice block is a block that has ice physics. */
class IceBlock(game: MegamanMaverickGame) : Block(game) {

  private var ice: Fixture? = null

  override fun init() {
    super.init()
    ice = Fixture(GameRectangle(), FixtureType.ICE)
    body.addFixture(ice!!)
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    (ice!!.shape as GameRectangle).set(body)
  }
}
