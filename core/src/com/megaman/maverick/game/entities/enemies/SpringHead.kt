package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Rectangle
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
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
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
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.VelocityAlterationType
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getBody
import kotlin.reflect.KClass

class SpringHead(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  companion object {
    const val TAG = "SpringHead"

    private var textureAtlas: TextureAtlas? = null

    private const val SPEED_NORMAL = 2.5f
    private const val SPEED_SUPER = 7f

    private const val BOUNCE_DUR = 2f
    private const val TURN_DELAY = .25f

    private const val X_BOUNCE = 10f
    private const val Y_BOUNCE = 20f
  }

  override var facing = Facing.RIGHT

  override val damageNegotiations = objectMapOf<KClass<out IDamager>, Int>()

  val bouncing: Boolean
    get() = !bounceTimer.isFinished()

  private val turnTimer = Timer(TURN_DELAY)
  private val bounceTimer = Timer(BOUNCE_DUR)

  private val speedUpScanner =
      Rectangle().setSize(ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.PPM / 4f)

  private val facingWrongDirection: Boolean
    get() {
      val megamanBody = getMegamanMaverickGame().megaman.body
      return (body.x < megamanBody.x && facing == Facing.LEFT) ||
          (body.x > megamanBody.x && facing == Facing.RIGHT)
    }

  override fun init() {
    if (textureAtlas == null)
        textureAtlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
    super.init()
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    bounceTimer.setToEnd()
    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
    body.setBottomCenterToPoint(spawn)
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(ConstVals.PPM / 4f)

    // left fixture
    val leftFixture = Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM), FixtureType.SIDE)
    leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
    leftFixture.offsetFromBodyCenter.set(-0.4f * ConstVals.PPM, -ConstVals.PPM / 4f)
    body.addFixture(leftFixture)

    // right fixture
    val rightFixture = Fixture(GameRectangle().setSize(0.1f * ConstVals.PPM), FixtureType.SIDE)
    rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
    rightFixture.offsetFromBodyCenter.set(0.4f * ConstVals.PPM, -ConstVals.PPM / 4f)
    body.addFixture(rightFixture)

    val c1 = GameCircle().setRadius(0.5f * ConstVals.PPM)

    // damager fixture
    val damagerFixture = Fixture(c1.copy(), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    // damageable fixture
    val damageableFixture = Fixture(c1.copy(), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // val shieldFixture
    val shieldFixture =
        Fixture(
            GameRectangle().setSize(0.85f * ConstVals.PPM, 0.6f * ConstVals.PPM),
            FixtureType.SHIELD)
    shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
    shieldFixture.offsetFromBodyCenter.y = 0.1f * ConstVals.PPM
    body.addFixture(shieldFixture)

    // bouncer fixture
    val bouncerFixture = Fixture(GameCircle().setRadius(0.35f * ConstVals.PPM), FixtureType.BOUNCER)
    bouncerFixture.putProperty(
        ConstKeys.VELOCITY_ALTERATION,
        { bounceable: Fixture, _: Float -> velocityAlteration(bounceable) })
    body.addFixture(bouncerFixture)

    return BodyComponentCreator.create(this, body)
  }

  private fun velocityAlteration(bounceable: Fixture): VelocityAlteration {
    if (bouncing) return VelocityAlteration.addNone()

    val bounceableBody = bounceable.getBody()
    bounceTimer.reset()
    val x = (if (body.x > bounceableBody.x) -X_BOUNCE else X_BOUNCE) * ConstVals.PPM
    return VelocityAlteration(
        x, Y_BOUNCE * ConstVals.PPM, VelocityAlterationType.ADD, VelocityAlterationType.SET)
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      speedUpScanner.setCenter(body.getCenter())
      turnTimer.update(it)

      val megaman = getMegamanMaverickGame().megaman
      if (turnTimer.isJustFinished())
          facing = if (megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT

      if (turnTimer.isFinished() && facingWrongDirection) turnTimer.reset()

      bounceTimer.update(it)
      if (bouncing) {
        body.physics.velocity.x = 0f
        return@add
      }

      val vel =
          (if (megaman.body.overlaps(speedUpScanner)) SPEED_SUPER else SPEED_NORMAL) * ConstVals.PPM
      body.physics.velocity.x = if (facing == Facing.LEFT) -vel else vel
    }
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(1.5f * ConstVals.PPM)
    val SpritesComponent = SpritesComponent(this, "springHead" to sprite)
    SpritesComponent.putUpdateFunction("springHead") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
      _sprite.setFlip(facing == Facing.LEFT, false)
    }
    return SpritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String = { if (bouncing) "unleashed" else "compressed" }
    val animations =
        objectMapOf<String, IAnimation>(
            "unleashed" to
                Animation(textureAtlas!!.findRegion("SpringHead/Unleashed"), 1, 6, 0.1f, true),
            "compressed" to Animation(textureAtlas!!.findRegion("SpringHead/Compressed")))
    return AnimationsComponent(this, Animator(keySupplier, animations))
  }
}
