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
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
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
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SwinginJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  private enum class SwinginJoeSetting {
    SWING_EYES_CLOSED,
    SWING_EYES_OPEN,
    THROWING
  }

  companion object {
    private var atlas: TextureAtlas? = null
    private const val BALL_SPEED = 9f
    private const val SETTING_DUR = .8f
  }

  override var facing = Facing.RIGHT

  override val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 2,
          ChargedShot::class to 4,
          ChargedShotExplosion::class to 1,
          Fireball::class to 4)

  private lateinit var setting: SwinginJoeSetting
  private val settingTimer = Timer(SETTING_DUR)
  private var type = ""

  override fun init() {
    super.init()
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    settingTimer.reset()
    setting = SwinginJoeSetting.SWING_EYES_CLOSED

    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
    body.positionOnPoint(spawn, Position.BOTTOM_CENTER)

    type =
        if (spawnProps.containsKey(ConstKeys.TYPE)) spawnProps.get(ConstKeys.TYPE, String::class)!!
        else ""
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)

    val shapes = Array<() -> IDrawableShape?>()

    // damager fixture
    val damagerFixture =
        Fixture(
            GameRectangle().setSize(0.75f * ConstVals.PPM, 1.15f * ConstVals.PPM),
            FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    damagerFixture.shape.color = Color.RED
    shapes.add { damagerFixture.shape }

    // damageable fixture
    val damageableFixture =
        Fixture(
            GameRectangle().setSize(0.8f * ConstVals.PPM, 1.35f * ConstVals.PPM),
            FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    damageableFixture.shape.color = Color.PURPLE
    shapes.add { damageableFixture.shape }

    // shield fixture
    val shieldFixture =
        Fixture(
            GameRectangle().setSize(0.4f * ConstVals.PPM, 0.9f * ConstVals.PPM), FixtureType.SHIELD)
    shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
    body.addFixture(shieldFixture)

    shieldFixture.shape.color = Color.BLUE
    shapes.add { shieldFixture.shape }

    // pre-process
    body.preProcess = Updatable {
      shieldFixture.active = setting == SwinginJoeSetting.SWING_EYES_CLOSED
      if (setting == SwinginJoeSetting.SWING_EYES_CLOSED) {
        damageableFixture.offsetFromBodyCenter.x = 0.25f * ConstVals.PPM * -facing.value
        shieldFixture.offsetFromBodyCenter.x = 0.35f * ConstVals.PPM * facing.value
      } else damageableFixture.offsetFromBodyCenter.x = 0f
    }

    addComponent(DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true))

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(2.25f * ConstVals.PPM)
    val SpritesComponent = SpritesComponent(this, "swingin_joe" to sprite)
    SpritesComponent.putUpdateFunction("swingin_joe") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
      _sprite.setFlip(facing == Facing.LEFT, false)
      if (facing == Facing.RIGHT) _sprite.translateX(-0.515f * ConstVals.PPM)
    }
    return SpritesComponent
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      val megaman = getMegamanMaverickGame().megaman
      facing = if (megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT

      settingTimer.update(it)
      if (settingTimer.isJustFinished()) {
        val index = (setting.ordinal + 1) % SwinginJoeSetting.values().size
        setting = SwinginJoeSetting.values()[index]
        if (setting == SwinginJoeSetting.THROWING) shoot()
        settingTimer.reset()
      }
    }
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String = {
      type +
          when (setting) {
            SwinginJoeSetting.SWING_EYES_CLOSED -> "SwingBall1"
            SwinginJoeSetting.SWING_EYES_OPEN -> "SwingBall2"
            SwinginJoeSetting.THROWING -> "ThrowBall"
          }
    }
    val animations =
        objectMapOf<String, IAnimation>(
            "SwingBall1" to
                Animation(atlas!!.findRegion("SwinginJoe/SwingBall1"), 1, 4, 0.1f, true),
            "SwingBall2" to
                Animation(atlas!!.findRegion("SwinginJoe/SwingBall2"), 1, 4, 0.1f, true),
            "ThrowBall" to Animation(atlas!!.findRegion("SwinginJoe/ThrowBall")),
            "SnowSwingBall1" to
                Animation(atlas!!.findRegion("SwinginJoe/SnowSwingBall1"), 1, 4, 0.1f, true),
            "SnowSwingBall2" to
                Animation(atlas!!.findRegion("SwinginJoe/SnowSwingBall2"), 1, 4, 0.1f, true),
            "SnowThrowBall" to Animation(atlas!!.findRegion("SwinginJoe/SnowThrowBall")),
        )
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }

  private fun shoot() {
    val spawn = body.getCenter().add(0.2f * facing.value * ConstVals.PPM, 0.15f * ConstVals.PPM)
    val props =
        props(
            ConstKeys.POSITION to spawn,
            ConstKeys.TYPE to type,
            ConstKeys.OWNER to this,
            ConstKeys.TRAJECTORY to Vector2().set(BALL_SPEED * ConstVals.PPM * facing.value, 0f),
            ConstKeys.MASK to objectSetOf<KClass<out IDamageable>>(Megaman::class))
    val joeBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.JOEBALL)!!
    game.gameEngine.spawn(joeBall, props)
  }
}
