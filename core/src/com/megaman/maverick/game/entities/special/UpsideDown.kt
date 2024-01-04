package com.megaman.maverick.game.entities.special

import com.engine.common.extensions.gdxArrayOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class UpsideDown(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity {

  private lateinit var upsideDownBounds: GameRectangle

  override fun init() {
    addComponent(defineBodyComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
    body.set(bounds)
    upsideDownBounds.set(bounds)

    addComponent(createCullablesComponent())
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)

    // upside-down fixture
    upsideDownBounds = GameRectangle()
    val upsideDownFixture = Fixture(upsideDownBounds, FixtureType.UPSIDE_DOWN)
    body.addFixture(upsideDownFixture)

    return BodyComponentCreator.create(this, body)
  }

  private fun createCullablesComponent(): CullablesComponent {
    val cullOnOutOfBounds = getGameCameraCullingLogic(this)
    return CullablesComponent(this, gdxArrayOf(cullOnOutOfBounds))
  }
}
