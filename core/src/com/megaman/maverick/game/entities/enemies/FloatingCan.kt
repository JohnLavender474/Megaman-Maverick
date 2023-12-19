package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.pathfinding.PathfinderParams
import com.engine.pathfinding.PathfindingComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.pathfinding.StandardPathfinderResultConsumer
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class FloatingCan(game: MegamanMaverickGame) : AbstractEnemy(game) {

  companion object {
    private var textureRegion: TextureRegion? = null
    private var FLY_SPEED = 1.5f
    private var DEBUG_PATHFINDING = false
  }

  override fun init() {
    if (textureRegion == null)
        textureRegion =
            game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source).findRegion("FloatingCan")
    super.init()

    addComponent(defineBodyComponent())
    addComponent(defineSpriteComponent())
    addComponent(defineAnimationsComponent())
    addComponent(definePathfindingComponent())
  }

  override val damageNegotiations =
      objectMapOf(
          Bullet::class.hashCode() to 10,
          Fireball::class.hashCode() to ConstVals.MAX_HEALTH,
          ChargedShot::class.hashCode() to ConstVals.MAX_HEALTH,
          ChargedShotExplosion::class.hashCode() to 15)

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(.75f * ConstVals.PPM)

    // damageable fixture
    val damageableFixture =
        Fixture(GameRectangle().setSize(.75f * ConstVals.PPM), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // damager fixture
    val damagerFixture = Fixture(GameRectangle().setSize(.75f * ConstVals.PPM), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite()
    sprite.setSize(1.5f * ConstVals.PPM)

    val spriteComponent = SpriteComponent(this, "can" to sprite)
    spriteComponent.putUpdateFunction("can") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getCenter(), Position.CENTER)
      // TODO: set flip
    }

    return spriteComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val animation = Animation(textureRegion!!, 1, 4, 0.15f, true)
    val animator = Animator(animation)
    return AnimationsComponent(this, animator)
  }

  private fun definePathfindingComponent(): PathfindingComponent {
    val params =
        PathfinderParams(
            // start at this bat's body center
            startSupplier = { body.getCenter() },
            // target the top center point of Megaman
            targetSupplier = { getMegamanMaverickGame().megaman.body.getTopCenterPoint() },
            // don't travel diagonally
            allowDiagonal = { true },
            // try to avoid collision with blocks when pathfinding
            filter = { _, objs ->
              objs.none { it is Fixture && it.fixtureLabel == FixtureType.BLOCK }
            })

    val pathfindingComponent =
        PathfindingComponent(
            this,
            params,
            {
              StandardPathfinderResultConsumer.consume(
                  it,
                  body,
                  body.getCenter(),
                  FLY_SPEED,
                  if (DEBUG_PATHFINDING) getMegamanMaverickGame().getShapes() else null)
            },
            { true })

    pathfindingComponent.updateIntervalTimer = Timer(0.1f)

    return pathfindingComponent
  }
}
