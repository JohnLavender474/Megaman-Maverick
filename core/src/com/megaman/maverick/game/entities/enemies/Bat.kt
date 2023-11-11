package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.pathfinding.PathfinderParams
import com.engine.pathfinding.PathfindingComponent
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
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
import com.megaman.maverick.game.world.*

/**
 * A bat enemy.
 *
 * @param game The game instance.
 */
class Bat(game: MegamanMaverickGame) : AbstractEnemy(game) {

  enum class BatStatus(val region: String) {
    HANGING("Hang"),
    OPEN_EYES("OpenEyes"),
    OPEN_WINGS("OpenWings"),
    FLYING_TO_ATTACK("Fly"),
    FLYING_TO_RETREAT("Fly")
  }

  companion object {
    private var atlas: TextureAtlas? = null

    private const val HANG_DURATION = 1.75f
    private const val RELEASE_FROM_PERCH_DURATION = .25f
    private const val FLY_TO_ATTACK_SPEED = 3f
    private const val FLY_TO_RETREAT_SPEED = 8f
  }

  override val damageNegotiations =
      objectMapOf(
          Bullet::class.hashCode() to 10,
          Fireball::class.hashCode() to ConstVals.MAX_HEALTH,
          ChargedShot::class.hashCode() to ConstVals.MAX_HEALTH,
          ChargedShotExplosion::class.hashCode() to ConstVals.MAX_HEALTH)

  private val hangTimer = Timer(HANG_DURATION)
  private val releasePerchTimer = Timer(RELEASE_FROM_PERCH_DURATION)

  private lateinit var type: String
  private lateinit var status: BatStatus

  override fun init() {
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
    super.init()

    addComponent(defineAnimationsComponent())
    addComponent(definePathfindingComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    hangTimer.reset()
    releasePerchTimer.reset()
    status = BatStatus.HANGING

    val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
    body.setTopCenterToPoint(bounds.getTopCenterPoint())

    type =
        if (spawnProps.containsKey(ConstKeys.TYPE)) spawnProps.get(ConstKeys.TYPE) as String else ""
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)

    updatablesComponent.add {
      when (status) {
        BatStatus.HANGING -> {
          hangTimer.update(it)
          if (hangTimer.isFinished()) {
            status = BatStatus.OPEN_EYES
            hangTimer.reset()
          }
        }
        BatStatus.OPEN_EYES,
        BatStatus.OPEN_WINGS -> {
          releasePerchTimer.update(it)
          if (releasePerchTimer.isFinished()) {
            if (status == BatStatus.OPEN_EYES) {
              status = BatStatus.OPEN_WINGS
              releasePerchTimer.reset()
            } else status = BatStatus.FLYING_TO_ATTACK
          }
        }
        BatStatus.FLYING_TO_RETREAT -> {
          if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) status = BatStatus.HANGING
        }
        else -> {}
      }
    }
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(.5f * ConstVals.PPM, .25f * ConstVals.PPM)

    // head fixture
    val headFixture =
        Fixture(
            GameRectangle().setSize(.5f * ConstVals.PPM, .175f * ConstVals.PPM), FixtureType.HEAD)
    headFixture.offsetFromBodyCenter.y = 0.375f * ConstVals.PPM
    body.addFixture(headFixture)

    // model for following fixtures
    val model = GameRectangle().setSize(0.75f * ConstVals.PPM)

    // damageable fixture
    val damageableFixture = Fixture(model.copy(), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // damager fixture
    val damagerFixture = Fixture(model.copy(), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    // shield fixture
    val shieldFixture = Fixture(model.copy(), FixtureType.SHIELD)
    shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
    body.addFixture(shieldFixture)

    // scanner fixture
    val scannerFixture = Fixture(model.copy(), FixtureType.CONSUMER)
    val consumer: (Fixture) -> Unit = {
      if (it.fixtureLabel == FixtureType.DAMAGEABLE &&
          it.getEntity() == getMegamanMaverickGame().megaman)
          status = BatStatus.FLYING_TO_RETREAT
    }
    scannerFixture.putProperty(ConstKeys.CONSUMER, consumer)
    body.addFixture(scannerFixture)

    body.preProcess = Updatable {
      shieldFixture.active = status == BatStatus.HANGING
      damageableFixture.active = status != BatStatus.HANGING

      if (status == BatStatus.FLYING_TO_RETREAT)
          body.physics.velocity.set(0f, FLY_TO_RETREAT_SPEED * ConstVals.PPM)
      else if (status != BatStatus.FLYING_TO_ATTACK) body.physics.velocity.setZero()
    }

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite()
    sprite.setSize(1.5f * ConstVals.PPM)

    val spriteComponent = SpriteComponent(this, "bat" to sprite)
    spriteComponent.putUpdateFunction("bat") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getCenter(), Position.CENTER)
      _sprite.hidden = damageBlink
    }
    return spriteComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier = { type + status.region }
    val animator =
        Animator(
            keySupplier,
            objectMapOf(
                "Hang" to Animation(atlas!!.findRegion("Bat/Hang"), true),
                "Fly" to Animation(atlas!!.findRegion("Bat/Fly"), 1, 2, 0.1f, true),
                "OpenEyes" to Animation(atlas!!.findRegion("Bat/OpenEyes"), true),
                "OpenWings" to Animation(atlas!!.findRegion("Bat/OpenWings"), true),
                "SnowHang" to Animation(atlas!!.findRegion("SnowBat/Hang"), true),
                "SnowFly" to Animation(atlas!!.findRegion("SnowBat/Fly"), 1, 2, 0.1f, true),
                "SnowOpenEyes" to Animation(atlas!!.findRegion("SnowBat/OpenEyes"), true),
                "SnowOpenWings" to Animation(atlas!!.findRegion("SnowBat/OpenWings"), true)))
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
            allowDiagonal = { false },
            // try to avoid collision with blocks or other bats when pathfinding
            filter = { _, objs ->
              objs.none {
                it is Fixture && (it.fixtureLabel == FixtureType.BLOCK || it.getEntity() is Bat)
              }
            })

    return PathfindingComponent(
        this,
        params,
        {
          StandardPathfinderResultConsumer.consume(
              it, body, body.getTopCenterPoint(), FLY_TO_ATTACK_SPEED)
        },
        { status == BatStatus.FLYING_TO_ATTACK })
  }
}
