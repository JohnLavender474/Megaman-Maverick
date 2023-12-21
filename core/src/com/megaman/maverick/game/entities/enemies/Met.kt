package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapeComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpriteComponent
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
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

/**
 * A met enemy.
 *
 * @param game The game instance.
 */
class Met(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  enum class MetBehavior {
    SHIELDING,
    POP_UP,
    RUNNING
  }

  companion object {
    const val RUN_ONLY = "RunOnly"
    const val RUNNING_ALLOWED = "RunningAllowed"

    private var atlas: TextureAtlas? = null

    private const val SHIELDING_DURATION = 1.15f
    private const val RUNNING_DURATION = .5f
    private const val POP_UP_DURATION = .5f
    private const val RUN_VELOCITY = 8f
    private const val RUN_IN_WATER_VELOCITY = 3f
    private const val GRAVITY_Y = .15f
    private const val BULLET_TRAJECTORY_X = 15f
    private const val BULLET_TRAJECTORY_Y = .25f
    private const val VELOCITY_CLAMP_X = 8f
    private const val VELOCITY_CLAMP_Y = 1.5f
  }

  private val metBehaviorTimers =
      objectMapOf(
          MetBehavior.SHIELDING to Timer(SHIELDING_DURATION),
          MetBehavior.POP_UP to Timer(POP_UP_DURATION),
          MetBehavior.RUNNING to Timer(RUNNING_DURATION))

  override val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 10,
          Fireball::class to ConstVals.MAX_HEALTH,
          ChargedShot::class to ConstVals.MAX_HEALTH,
          ChargedShotExplosion::class to ConstVals.MAX_HEALTH)

  override lateinit var facing: Facing

  private lateinit var type: String

  private var behavior: MetBehavior = MetBehavior.SHIELDING
    set(value) {
      field = value
      metBehaviorTimers.values().forEach { it.reset() }
    }

  private var runOnly = false
  private var runningAllowed = false

  override fun init() {
    super.init()
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    behavior = MetBehavior.SHIELDING
    val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
    body.setBottomCenterToPoint(bounds.getBottomCenterPoint())

    runningAllowed = spawnProps.getOrDefault(RUNNING_ALLOWED, true) as Boolean
    runOnly = spawnProps.getOrDefault(RUN_ONLY, false) as Boolean
    type = spawnProps.getOrDefault(ConstKeys.TYPE, "") as String

    val right = spawnProps.getOrDefault(ConstKeys.RIGHT, false) as Boolean
    facing = if (right) Facing.RIGHT else Facing.LEFT
  }

  private fun shoot() {
    val trajectory = Vector2(BULLET_TRAJECTORY_X, BULLET_TRAJECTORY_Y)
    if (facing == Facing.LEFT) trajectory.x *= -1

    val offset = ConstVals.PPM / 64f
    val spawn = body.getCenter().add(if (facing == Facing.LEFT) -offset else offset, offset)

    val spawnProps =
        props(
            ConstKeys.OWNER to this,
            ConstKeys.TRAJECTORY to trajectory,
            ConstKeys.POSITION to spawn)
    val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)
    game.gameEngine.spawn(bullet!!, spawnProps)
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)

    updatablesComponent.add {
      if (getMegamanMaverickGame().megaman.dead) return@add

      if (runOnly) behavior = MetBehavior.RUNNING

      when (behavior) {
        MetBehavior.SHIELDING -> {
          val shieldTimer = metBehaviorTimers.get(MetBehavior.SHIELDING)

          if (!isMegamanShootingAtMe()) shieldTimer.update(it)
          if (shieldTimer.isFinished()) behavior = MetBehavior.POP_UP
        }
        MetBehavior.POP_UP -> {
          facing =
              if (getMegamanMaverickGame().megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT

          val popUpTimer = metBehaviorTimers.get(MetBehavior.POP_UP)
          if (popUpTimer.isAtBeginning()) shoot()

          popUpTimer.update(it)

          if (popUpTimer.isFinished())
              behavior = if (runningAllowed) MetBehavior.RUNNING else MetBehavior.SHIELDING
        }
        MetBehavior.RUNNING -> {
          val runningTimer = metBehaviorTimers.get(MetBehavior.RUNNING)

          body.physics.velocity.x =
              ConstVals.PPM *
                  (if (body.isSensing(BodySense.IN_WATER)) RUN_IN_WATER_VELOCITY else RUN_VELOCITY)
          if (facing == Facing.LEFT) body.physics.velocity.x *= -1

          if (!runOnly) runningTimer.update(it)

          if (runningTimer.isFinished()) {
            if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.x = 0f
            behavior = MetBehavior.SHIELDING
          }
        }
      }
    }
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(0.75f * ConstVals.PPM)
    body.physics.velocityClamp.set(
        VELOCITY_CLAMP_X * ConstVals.PPM, VELOCITY_CLAMP_Y * ConstVals.PPM)

    val shapes = Array<() -> IDrawableShape>()

    // body fixture
    val bodyFixture = Fixture(GameRectangle().setSize(0.75f * ConstVals.PPM), FixtureType.BODY)
    body.addFixture(bodyFixture)
    shapes.add { bodyFixture.shape }

    // feet fixture
    val feetFixture = Fixture(GameRectangle().setSize(0.15f * ConstVals.PPM), FixtureType.FEET)
    feetFixture.offsetFromBodyCenter.y = -.375f * ConstVals.PPM
    body.addFixture(feetFixture)

    // shield fixture
    val shieldFixture =
        Fixture(
            GameRectangle().setSize(0.75f * ConstVals.PPM, 0.5f * ConstVals.PPM),
            FixtureType.SHIELD)
    shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
    body.addFixture(shieldFixture)

    // damageable fixture
    val damageableFixture =
        Fixture(GameRectangle().setSize(0.75f * ConstVals.PPM), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // damager fixture
    val damagerFixture =
        Fixture(GameRectangle().setSize(0.75f * ConstVals.PPM), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    // pre-process
    body.preProcess = Updatable {
      body.physics.gravity.y =
          if (body.isSensing(BodySense.FEET_ON_GROUND)) 0f else -GRAVITY_Y * ConstVals.PPM
      shieldFixture.active = behavior == MetBehavior.SHIELDING
      damageableFixture.active = behavior != MetBehavior.SHIELDING
    }

    addComponent(DrawableShapeComponent(this, shapes))

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
    sprite.setSize(1.5f * ConstVals.PPM)

    val spriteComponent = SpriteComponent(this, "met" to sprite)
    spriteComponent.putUpdateFunction("met") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
      _sprite.setFlip(facing == Facing.LEFT, false)
      // TODO: _sprite.hidden = damageBlink
    }

    return spriteComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier = {
      when (behavior) {
        MetBehavior.SHIELDING -> "LayDown"
        MetBehavior.POP_UP -> "PopUp"
        MetBehavior.RUNNING -> "Run"
      }
    }

    val animator =
        Animator(
            keySupplier,
            objectMapOf(
                "Run" to Animation(atlas!!.findRegion("Met/Run"), 1, 2, 0.125f, true),
                "PopUp" to Animation(atlas!!.findRegion("Met/PopUp"), false),
                "LayDown" to Animation(atlas!!.findRegion("Met/LayDown"), false),
                "SnowRun" to Animation(atlas!!.findRegion("SnowMet/Run"), 1, 2, 0.125f, true),
                "SnowMet" to Animation(atlas!!.findRegion("SnowMet/PopUp"), false),
                "SnowLayDown" to Animation(atlas!!.findRegion("SnowMet/LayDown"), false)))

    return AnimationsComponent(this, animator)
  }
}
