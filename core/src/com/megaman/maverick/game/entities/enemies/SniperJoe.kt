package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
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
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
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

class SniperJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable, IDirectionRotatable {

  companion object {
    const val TAG = "SniperJoe"
    const val SNOW_TYPE = "Snow"

    private val TIMES_TO_SHOOT = floatArrayOf(0.15f, 0.75f, 1.35f)

    private const val BULLET_SPEED = 7.5f
    private const val SNOWBALL_X = 10f
    private const val SNOWBALL_Y = 5f
    private const val SNOWBALL_GRAV = 0.15f

    private const val SHIELD_DUR = 1.75f
    private const val SHOOT_DUR = 1.5f

    private const val GRAVITY = 0.015f

    private var atlas: TextureAtlas? = null
  }

  override var facing = Facing.RIGHT

  override val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 5,
          Fireball::class to 15,
          ChargedShot::class to 15,
          ChargedShotExplosion::class to 15)

  override var directionRotation: Direction
    get() = body.cardinalRotation
    set(value) {
      body.cardinalRotation = value
    }

  private val shieldTimer = Timer(SHIELD_DUR)
  private val shootTimer = Timer(SHOOT_DUR)

  private var type = ""
  private var shielded = false
    set(value) {
      val timer = if (value) shieldTimer else shootTimer
      timer.reset()
      field = value
    }

  override fun init() {
    super.init()

    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)

    val shootRunnables = Array<TimeMarkedRunnable>()
    TIMES_TO_SHOOT.forEach { shootRunnables.add(TimeMarkedRunnable(it) { shoot() }) }
    shootTimer.setRunnables(shootRunnables)

    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
    body.positionOnPoint(spawn, Position.BOTTOM_CENTER)
    type = spawnProps.getOrDefault(ConstKeys.TYPE, "") as String
    shieldTimer.setToEnd()
    shootTimer.setToEnd()
    shielded = true
    directionRotation = Direction.UP
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)

    val shapes = Array<() -> IDrawableShape?>()

    // body fixture
    val bodyFixture = Fixture(GameRectangle().set(body), FixtureType.BODY)
    body.addFixture(bodyFixture)

    // feet fixture
    val feetFixture = Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM), FixtureType.FEET)
    feetFixture.offsetFromBodyCenter.y = -0.75f * ConstVals.PPM
    body.addFixture(feetFixture)
    feetFixture.shape.color = Color.GREEN
    shapes.add { feetFixture.shape }

    // damager fixture
    val damagerFixture =
        Fixture(
            GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM),
            FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    damagerFixture.shape.color = Color.RED
    shapes.add { damagerFixture.bodyRelativeShape }

    // damageable fixture
    val damageableFixture =
        Fixture(
            GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM),
            FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    damageableFixture.shape.color = Color.PURPLE
    shapes.add { damageableFixture.bodyRelativeShape }

    // shield fixture
    val shieldFixture =
        Fixture(
            GameRectangle().setSize(0.4f * ConstVals.PPM, 0.9f * ConstVals.PPM), FixtureType.SHIELD)
    body.addFixture(shieldFixture)

    shieldFixture.shape.color = Color.BLUE
    shapes.add { shieldFixture.bodyRelativeShape }

    // pre-process
    body.preProcess = Updatable {
      body.physics.gravity =
          if (body.isSensing(BodySense.FEET_ON_GROUND)) Vector2()
          else
              (when (directionRotation) {
                    Direction.UP -> Vector2(0f, -GRAVITY)
                    Direction.DOWN -> Vector2(0f, GRAVITY)
                    Direction.LEFT -> Vector2(GRAVITY, 0f)
                    Direction.RIGHT -> Vector2(-GRAVITY, 0f)
                  })
                  .scl(ConstVals.PPM.toFloat())

      shieldFixture.active = shielded
      shieldFixture.offsetFromBodyCenter.x =
          0.35f *
              ConstVals.PPM *
              if (isDirectionRotatedUp() || isDirectionRotatedLeft()) facing.value
              else -facing.value
      shieldFixture.putProperty(ConstKeys.DIRECTION, directionRotation)

      if (shielded)
          damageableFixture.offsetFromBodyCenter.x =
              0.25f *
                  ConstVals.PPM *
                  if (isDirectionRotatedUp() || isDirectionRotatedLeft()) -facing.value
                  else facing.value
      else damageableFixture.offsetFromBodyCenter.x = 0f
    }

    addComponent(DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true))

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(1.35f * ConstVals.PPM)

    val spritesComponent = SpritesComponent(this, "sniperjoe" to sprite)
    spritesComponent.putUpdateFunction("sniperjoe") { _, _sprite ->
      _sprite as GameSprite

      val flipX = facing == Facing.LEFT
      val flipY = directionRotation == Direction.DOWN
      _sprite.setFlip(flipX, flipY)

      val rotation =
          when (directionRotation) {
            Direction.UP,
            Direction.DOWN -> 0f
            Direction.LEFT -> 90f
            Direction.RIGHT -> 270f
          }
      sprite.setOriginCenter()
      _sprite.setRotation(rotation)

      val position =
          when (directionRotation) {
            Direction.UP -> Position.BOTTOM_CENTER
            Direction.DOWN -> Position.TOP_CENTER
            Direction.LEFT -> Position.CENTER_RIGHT
            Direction.RIGHT -> Position.CENTER_LEFT
          }
      val bodyPosition = body.getPositionPoint(position)
      _sprite.setPosition(bodyPosition, position)

      if (directionRotation == Direction.LEFT) _sprite.translateX(0.15f * ConstVals.PPM)
      else if (directionRotation == Direction.RIGHT) _sprite.translateX(-0.15f * ConstVals.PPM)
    }
    return spritesComponent
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      val megaman = getMegamanMaverickGame().megaman
      facing =
          when (directionRotation) {
            Direction.UP,
            Direction.DOWN -> if (megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT
            Direction.LEFT,
            Direction.RIGHT -> if (megaman.body.y > body.y) Facing.RIGHT else Facing.LEFT
          }

      val timer = if (shielded) shieldTimer else shootTimer
      timer.update(it)

      if (timer.isFinished()) shielded = !shielded
    }
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String = { type + if (shielded) "Shielded" else "Shooting" }
    val animations =
        objectMapOf<String, IAnimation>(
            "Shooting" to Animation(atlas!!.findRegion("SniperJoe/Shooting")),
            "Shielded" to Animation(atlas!!.findRegion("SniperJoe/Shielded")),
            "SnowShooting" to Animation(atlas!!.findRegion("SnowSniperJoe/Shooting")),
            "SnowShielded" to Animation(atlas!!.findRegion("SnowSniperJoe/Shielded")))
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }

  private fun shoot() {
    val spawn =
        (when (directionRotation) {
              Direction.UP -> Vector2(0.25f * facing.value, -0.15f)
              Direction.DOWN -> Vector2(0.25f * facing.value, 0.15f)
              Direction.LEFT -> Vector2(0.2f, 0.25f * facing.value)
              Direction.RIGHT -> Vector2(-0.2f, 0.25f * facing.value)
            })
            .scl(ConstVals.PPM.toFloat())
            .add(body.getCenter())

    val trajectory = Vector2()

    val props =
        props(
            ConstKeys.OWNER to this,
            ConstKeys.POSITION to spawn,
            ConstKeys.TRAJECTORY to trajectory,
            ConstKeys.DIRECTION to directionRotation)

    val entity: IGameEntity =
        if (type == SNOW_TYPE) {
          // TODO: fix trajectory to align with body rotation

          trajectory.x = SNOWBALL_X * ConstVals.PPM * facing.value
          trajectory.y = SNOWBALL_Y * ConstVals.PPM

          props.put(ConstKeys.GRAVITY_ON, true)
          props.put(ConstKeys.GRAVITY, Vector2(0f, -SNOWBALL_GRAV * ConstVals.PPM))

          requestToPlaySound(SoundAsset.CHILL_SHOOT, false)

          EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOWBALL)!!
        } else {
          if (isDirectionRotatedVertically())
              trajectory.set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)
          else trajectory.set(0f, BULLET_SPEED * ConstVals.PPM * facing.value)

          requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)

          EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        }

    game.gameEngine.spawn(entity, props)
  }
}
