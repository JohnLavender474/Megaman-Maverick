package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Position
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class Hanabiran(game: MegamanMaverickGame) : AbstractEnemy(game) {

  enum class HanabiranState {
    SLEEPING,
    RISING,
    DROPPING,
    PETAL_4,
    PETAL_3,
    PETAL_2,
    PETAL_1,
    PETAL_0,
  }

  companion object {
    const val TAG = "Hanabiran"
    private var atlas: TextureAtlas? = null
    private const val SLEEP_DURATION = 1f
    private const val RISE_DROP_DURATION = 0.45f
    private const val PETAL_DURATION = 0.5f
    private const val ANIMATION_FRAME_DURATION = 0.15f
  }

  private val sleepTimer = Timer(SLEEP_DURATION)
  private val riseDropTimer = Timer(RISE_DROP_DURATION)
  private val petalTimer = Timer(PETAL_DURATION)

  private var petalCount = 4
  private lateinit var state: HanabiranState

  override val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 10,
          Fireball::class to ConstVals.MAX_HEALTH,
          ChargedShot::class to ConstVals.MAX_HEALTH,
          ChargedShotExplosion::class to ConstVals.MAX_HEALTH)

  override fun init() {
    super.init()
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
    body.setBottomCenterToPoint(bounds.getBottomCenterPoint())

    state = HanabiranState.SLEEPING
  }

  private fun shoot() {
    val start = body.getCenter()
    val target = megaman.body.getCenter()
    val trajectory = target.sub(start).nor()

    val petal = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.PETAL)!!
    GameLogger.debug(
        TAG,
        "Shooting petal. Start: $start. Target: $target. Trajectory: $trajectory. Petal: $petal")

    game.gameEngine.spawn(
        petal,
        props(
            ConstKeys.POSITION to start,
            ConstKeys.TRAJECTORY to trajectory,
            ConstKeys.OWNER to this))
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      when (state) {
        HanabiranState.SLEEPING -> {
          sleepTimer.update(it)
          if (sleepTimer.isJustFinished()) {
            sleepTimer.reset()
            state = HanabiranState.RISING
          }
        }
        HanabiranState.RISING -> {
          riseDropTimer.update(it)
          if (riseDropTimer.isJustFinished()) {
            riseDropTimer.reset()
            petalCount = 4
            state = HanabiranState.PETAL_4
          }
        }
        HanabiranState.PETAL_4,
        HanabiranState.PETAL_3,
        HanabiranState.PETAL_2,
        HanabiranState.PETAL_1,
        HanabiranState.PETAL_0 -> {
          petalTimer.update(it)
          if (petalTimer.isJustFinished()) {
            petalCount--
            state =
                if (petalCount < 0) {
                  HanabiranState.DROPPING
                } else {
                  shoot()
                  HanabiranState.valueOf("PETAL_$petalCount")
                }
            petalTimer.reset()
          }
        }
        HanabiranState.DROPPING -> {
          riseDropTimer.update(it)
          if (riseDropTimer.isJustFinished()) {
            riseDropTimer.reset()
            state = HanabiranState.SLEEPING
          }
        }
      }
    }
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())

    val debugShapes = Array<() -> IDrawableShape?>()

    val fixturesRectangle = GameRectangle()

    // body fixture
    val bodyFixture = Fixture(fixturesRectangle, FixtureType.BODY)
    bodyFixture.attachedToBody = false
    body.addFixture(bodyFixture)
    debugShapes.add { bodyFixture.shape }

    // damager fixture
    val damagerFixture = Fixture(fixturesRectangle, FixtureType.DAMAGER)
    damagerFixture.attachedToBody = false
    body.addFixture(damagerFixture)
    debugShapes.add { if (damagerFixture.active) damagerFixture.shape else null }

    // damageable fixture
    val damageableFixture = Fixture(fixturesRectangle, FixtureType.DAMAGEABLE)
    damageableFixture.attachedToBody = false
    body.addFixture(damageableFixture)
    debugShapes.add { if (damageableFixture.active) damageableFixture.shape else null }

    body.preProcess = Updatable {
      fixturesRectangle.setSize(
          (when (state) {
                HanabiranState.SLEEPING -> Vector2.Zero
                HanabiranState.RISING -> {
                  if (riseDropTimer.time >= 0.3f) Vector2(0.75f, 0.75f)
                  else if (riseDropTimer.time >= 0.15f) Vector2(0.75f, 0.5f)
                  else Vector2(0.75f, 0.25f)
                }
                HanabiranState.DROPPING -> {
                  if (riseDropTimer.time >= 0.3f) Vector2(0.75f, 0.25f)
                  else if (riseDropTimer.time >= 0.15f) Vector2(0.75f, 0.5f)
                  else Vector2(0.75f, 0.75f)
                }
                HanabiranState.PETAL_4,
                HanabiranState.PETAL_3,
                HanabiranState.PETAL_2,
                HanabiranState.PETAL_1,
                HanabiranState.PETAL_0 -> Vector2(0.75f, 0.85f)
              })
              .scl(ConstVals.PPM.toFloat()))

      fixturesRectangle.positionOnPoint(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)

      val fixturesOn =
          !state.equalsAny(HanabiranState.SLEEPING, HanabiranState.RISING, HanabiranState.DROPPING)
      bodyFixture.active = fixturesOn
      damagerFixture.active = fixturesOn
      damageableFixture.active = fixturesOn
    }

    addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(1.25f * ConstVals.PPM)

    val spritesComponent = SpritesComponent(this, "hanibiran" to sprite)
    spritesComponent.putUpdateFunction("hanibiran") { _, _sprite ->
      _sprite as GameSprite

      val position = body.getBottomCenterPoint()
      _sprite.setPosition(position, Position.BOTTOM_CENTER)

      _sprite.hidden = state == HanabiranState.SLEEPING
    }

    return spritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String? = {
      when (state) {
        HanabiranState.RISING -> "Rise"
        HanabiranState.DROPPING -> "Drop"
        HanabiranState.PETAL_4 -> "4PetalsSpin"
        HanabiranState.PETAL_3 -> "3PetalsSpin"
        HanabiranState.PETAL_2 -> "2PetalsSpin"
        HanabiranState.PETAL_1 -> "1PetalSpin"
        HanabiranState.PETAL_0 -> "NoPetalsSpin"
        HanabiranState.SLEEPING -> null
      }
    }
    val animations =
        objectMapOf<String, IAnimation>(
            "1PetalSpin" to
                Animation(atlas!!.findRegion("1PetalSpin"), 1, 4, ANIMATION_FRAME_DURATION, true),
            "2PetalsSpin" to
                Animation(atlas!!.findRegion("2PetalsSpin"), 1, 4, ANIMATION_FRAME_DURATION, true),
            "3PetalsSpin" to
                Animation(atlas!!.findRegion("3PetalsSpin"), 1, 4, ANIMATION_FRAME_DURATION, true),
            "4PetalsSpin" to
                Animation(atlas!!.findRegion("4PetalsSpin"), 1, 2, ANIMATION_FRAME_DURATION, true),
            "NoPetalsSpin" to
                Animation(atlas!!.findRegion("NoPetalsSpin"), 1, 2, ANIMATION_FRAME_DURATION, true),
            "Rise" to Animation(atlas!!.findRegion("Rise"), 1, 3, ANIMATION_FRAME_DURATION, false),
            "Drop" to Animation(atlas!!.findRegion("Drop"), 1, 3, ANIMATION_FRAME_DURATION, false),
        )
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }
}
