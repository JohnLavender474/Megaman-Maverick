package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class FlyBoy(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  companion object {
    const val STAND_DUR = .5f
    const val FLY_DUR = 1.5f
    const val FLY_VEL = 10f
    const val GRAV = -.2f
    const val G_GRAV = -.015f
  }

  override val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 5,
          Fireball::class to 10,
          ChargedShot::class to 10,
          ChargedShotExplosion::class to 5)

  override var facing = Facing.RIGHT

  val flying: Boolean
    get() = !flyTimer.isFinished()

  val standing: Boolean
    get() = !standTimer.isFinished()

  private val flyTimer = Timer(FLY_DUR)
  private val standTimer = Timer(STAND_DUR)

  override fun init() {
    super.init()
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
    body.setBottomCenterToPoint(spawn)
    standTimer.reset()
    flyTimer.setToEnd()
  }

  override fun onDestroy() {
    super.onDestroy()

    if (hasDepletedHealth()) explode()
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(ConstVals.PPM.toFloat(), ConstVals.PPM * 2f)

    val shapes = Array<() -> IDrawableShape?>()

    // body fixture
    val bodyFixture = Fixture(GameRectangle().set(body), FixtureType.BODY)
    body.addFixture(bodyFixture)
    bodyFixture.shape.color = Color.BLUE
    shapes.add { bodyFixture.shape }

    // feet fixture
    val feetFixture = Fixture(GameRectangle().setSize(ConstVals.PPM * 0.5f), FixtureType.FEET)
    feetFixture.offsetFromBodyCenter.y = -ConstVals.PPM.toFloat()
    body.addFixture(feetFixture)
    feetFixture.shape.color = Color.GREEN
    shapes.add { feetFixture.shape }

    // head fixture
    val headFixture = Fixture(GameRectangle().setSize(ConstVals.PPM * 0.5f), FixtureType.HEAD)
    headFixture.offsetFromBodyCenter.y = ConstVals.PPM.toFloat()
    body.addFixture(headFixture)
    headFixture.shape.color = Color.ORANGE
    shapes.add { headFixture.shape }

    // damager fixture
    val damagerFixture =
        Fixture(
            GameRectangle().setSize(0.8f * ConstVals.PPM, 1.5f * ConstVals.PPM),
            FixtureType.DAMAGER)
    body.addFixture(damagerFixture)
    damagerFixture.shape.color = Color.RED
    shapes.add { damagerFixture.shape }

    // damageable fixture
    val damageableFixture = Fixture(GameRectangle().set(body), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)
    damageableFixture.shape.color = Color.PURPLE
    shapes.add { damageableFixture.shape }

    // pre-process
    body.preProcess = Updatable {
      body.physics.gravityOn = standing
      body.physics.gravity.y =
          (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV) * ConstVals.PPM
    }

    addComponent(DrawableShapesComponent(this, shapes))

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(ConstVals.PPM * 2.25f, ConstVals.PPM * 1.85f)
    val SpritesComponent = SpritesComponent(this, "flyboy" to sprite)
    SpritesComponent.putUpdateFunction("flyboy") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
      _sprite.setFlip(facing == Facing.LEFT, false)
    }
    return SpritesComponent
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      val megaman = getMegamanMaverickGame().megaman

      facing = if (body.x > megaman.body.x) Facing.LEFT else Facing.RIGHT

      if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.x = 0f

      if (standing && body.isSensing(BodySense.FEET_ON_GROUND)) {
        standTimer.update(it)
        if (standTimer.isJustFinished()) {
          flyTimer.reset()
          body.physics.velocity.y = FLY_VEL * ConstVals.PPM
        }
      }

      if (flying) {
        flyTimer.update(it)
        if (flyTimer.isJustFinished() || body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) {
          flyTimer.setToEnd()
          standTimer.reset()
          impulseToPlayer()
        }
      }
    }
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
    val animations =
        objectMapOf<String, IAnimation>(
            "fly" to Animation(atlas.findRegion("FlyBoy/Fly"), 1, 4, 0.05f),
            "stand" to Animation(atlas.findRegion("FlyBoy/Stand")))
    val keySupplier = { if (flying) "fly" else "stand" }
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }

  private fun impulseToPlayer() {
    val megaman = getMegamanMaverickGame().megaman
    body.physics.velocity.x = 1.85f * (megaman.body.x - body.x)
    body.physics.velocity.y = 0f
  }
}