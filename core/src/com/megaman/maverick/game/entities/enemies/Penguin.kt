package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
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
import kotlin.math.abs
import kotlin.reflect.KClass

class Penguin(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  companion object {
    private var atlas: TextureAtlas? = null
    private const val STAND_DUR = 1f
    private const val SLIDE_DUR = 0.25f
    private const val G_GRAV = -.0015f
    private const val GRAV = -0.375f
    private const val JUMP_X = 5f
    private const val JUMP_Y = 20f
    private const val SLIDE_X = 8f
  }

  override var facing = Facing.RIGHT

  override val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 5,
          Fireball::class to ConstVals.MAX_HEALTH,
          ChargedShot::class to 10,
          ChargedShotExplosion::class to 5)

  val sliding: Boolean
    get() = !slideTimer.isFinished() && body.isSensing(BodySense.FEET_ON_GROUND)

  val jumping: Boolean
    get() = !slideTimer.isFinished() && !body.isSensing(BodySense.FEET_ON_GROUND)

  val standing: Boolean
    get() = slideTimer.isFinished()

  private val standTimer = Timer(STAND_DUR)
  private val slideTimer = Timer(SLIDE_DUR)

  override fun init() {
    super.init()
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
    body.setBottomCenterToPoint(spawn)
    slideTimer.setToEnd()
    standTimer.reset()
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)

    // body fixture
    val bodyFixture = Fixture(GameRectangle(), FixtureType.BODY)
    body.addFixture(bodyFixture)

    // feet fixture
    val feetFixture = Fixture(GameRectangle().setHeight(0.1f * ConstVals.PPM), FixtureType.FEET)
    body.addFixture(feetFixture)

    // damageable fixture
    val damageableFixture = Fixture(GameRectangle(), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // damager fixture
    val damagerFixture = Fixture(GameRectangle(), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    body.preProcess = Updatable {
      val feetBounds = feetFixture.shape as GameRectangle
      if (standing || jumping) {
        body.setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())
        feetBounds.width = 0.65f * ConstVals.PPM
        feetFixture.offsetFromBodyCenter.y = -0.5f * ConstVals.PPM
      } else {
        body.setSize(ConstVals.PPM.toFloat(), 0.5f * ConstVals.PPM)
        feetBounds.width = 0.9f * ConstVals.PPM.toFloat()
        feetFixture.offsetFromBodyCenter.y = -0.25f * ConstVals.PPM
      }

      (damageableFixture.shape as GameRectangle).set(body)
      (damagerFixture.shape as GameRectangle).set(body)

      body.physics.gravity.y =
          (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV) * ConstVals.PPM

      if (sliding) body.physics.velocity.x = SLIDE_X * ConstVals.PPM * facing.value
    }

    return BodyComponentCreator.create(this, body)
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add { if (standing) stand(it) else if (sliding) slide(it) }
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(1.5f * ConstVals.PPM)
    val SpritesComponent = SpritesComponent(this, "penguin" to sprite)
    SpritesComponent.putUpdateFunction("penguin") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setFlip(facing == Facing.LEFT, false)
      _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
      if (sliding) sprite.translateY(-0.25f * ConstVals.PPM)
    }
    return SpritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String = {
      if (standing) if (abs(body.physics.velocity.x) > ConstVals.PPM / 4f) "slippin" else "stand"
      else if (jumping) "jump" else "slide"
    }
    val animations =
        objectMapOf<String, IAnimation>(
            "slippin" to Animation(atlas!!.findRegion("Penguin/Slippin")),
            "stand" to Animation(atlas!!.findRegion("Penguin/Stand"), 1, 2, 0.1f, true),
            "jump" to Animation(atlas!!.findRegion("Penguin/Jump")),
            "slide" to Animation(atlas!!.findRegion("Penguin/Slide")))
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }

  private fun stand(delta: Float) {
    val megaman = getMegamanMaverickGame().megaman
    facing = if (megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT
    standTimer.update(delta)
    if (body.isSensing(BodySense.FEET_ON_GROUND) && standTimer.isFinished()) jump()
  }

  private fun jump() {
    standTimer.setToEnd()
    slideTimer.reset()

    val impulse = Vector2()
    impulse.x = JUMP_X * ConstVals.PPM * facing.value
    impulse.y = JUMP_Y * ConstVals.PPM
    body.physics.velocity.add(impulse)
  }

  private fun slide(delta: Float) {
    slideTimer.update(delta)
    if (slideTimer.isFinished()) standTimer.reset()
  }
}
