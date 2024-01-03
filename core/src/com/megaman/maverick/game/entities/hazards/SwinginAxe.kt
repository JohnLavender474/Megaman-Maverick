package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.motion.MotionComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class SwinginAxe(game: MegamanMaverickGame) : GameEntity(game), ISpriteEntity, IBodyEntity {

  companion object {
    const val TAG = "SwinginAxe"
    private var textureRegion: TextureRegion? = null
  }

  private var rotation = 0f

  override fun init() {
    if (textureRegion == null)
        textureRegion = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "SwingingAxe")
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(2f * ConstVals.PPM)

    // death circle
    val deathCircle = GameCircle()
    deathCircle.setRadius(ConstVals.PPM.toFloat())
    val deathFixture = Fixture(deathCircle, FixtureType.DEATH)
    body.addFixture(deathFixture)

    // shield fixture
    val shieldCircle = GameCircle()
    shieldCircle.setRadius(ConstVals.PPM.toFloat())
    val shieldFixture = Fixture(shieldCircle, FixtureType.SHIELD)
    shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
    body.addFixture(shieldFixture)

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(2f * ConstVals.PPM)
    val spritesComponent = SpritesComponent(this, "axe" to sprite)
    spritesComponent.putUpdateFunction("axe") { _, _sprite ->
      _sprite as GameSprite
      val origin = body.getBottomCenterPoint()
      _sprite.setOrigin(origin.x, origin.y)
      _sprite.setPosition(origin, Position.BOTTOM_CENTER)
      _sprite.rotation = rotation
    }
    return spritesComponent
  }

  private fun defineMotionComponent(): MotionComponent {
    TODO()
  }
}
