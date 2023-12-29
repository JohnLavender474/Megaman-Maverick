package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapeComponent
import com.engine.drawables.shapes.IDrawableShape
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

class SuctionRoller(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  companion object {
    private var textureRegion: TextureRegion? = null
    private const val GRAVITY = -.15f
    private const val VEL_X = 2.5f
    private const val VEL_Y = 2.5f
  }

  override val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 5,
          Fireball::class to ConstVals.MAX_HEALTH,
          ChargedShot::class to ConstVals.MAX_HEALTH,
          ChargedShotExplosion::class to 15)

  override lateinit var facing: Facing

  private var onWall = false
  private var wasOnWall = false

  override fun init() {
    if (textureRegion == null)
        textureRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "SuctionRoller")
    super.init()
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    onWall = false
    wasOnWall = false
    facing = if (getMegamanMaverickGame().megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT
    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
    body.positionOnPoint(spawn, Position.BOTTOM_CENTER)
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      val megaman = getMegamanMaverickGame().megaman
      if (megaman.dead) return@add

      wasOnWall = onWall
      onWall =
          (facing == Facing.LEFT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
              (facing == Facing.RIGHT && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

      if (body.isSensing(BodySense.FEET_ON_GROUND)) {
        if (megaman.body.getBottomRightPoint().x < body.x) facing = Facing.LEFT
        else if (megaman.body.x > body.getBottomRightPoint().x) facing = Facing.RIGHT
      }
    }
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())

    val shapes = Array<() -> IDrawableShape>()

    // body fixture
    val bodyFixture =
        Fixture(
            GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
            FixtureType.BODY)
    body.addFixture(bodyFixture)

    bodyFixture.shape.color = Color.BLUE
    shapes.add { bodyFixture.shape }

    // feet fixture
    val feetFixture =
        Fixture(GameRectangle().setSize(ConstVals.PPM / 4f, ConstVals.PPM / 32f), FixtureType.FEET)
    feetFixture.offsetFromBodyCenter.y = -0.6f * ConstVals.PPM
    body.addFixture(feetFixture)

    feetFixture.shape.color = Color.GREEN
    shapes.add { feetFixture.shape }

    // left fixture
    val leftFixture =
        Fixture(
            GameRectangle().setSize(ConstVals.PPM / 32f, ConstVals.PPM.toFloat()), FixtureType.SIDE)
    leftFixture.offsetFromBodyCenter.x = -0.375f * ConstVals.PPM
    leftFixture.offsetFromBodyCenter.y = ConstVals.PPM / 5f
    leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
    body.addFixture(leftFixture)

    leftFixture.shape.color = Color.ORANGE
    shapes.add { leftFixture.shape }

    // right fixture
    val rightFixture =
        Fixture(
            GameRectangle().setSize(ConstVals.PPM / 32f, ConstVals.PPM.toFloat()), FixtureType.SIDE)
    rightFixture.offsetFromBodyCenter.x = 0.375f * ConstVals.PPM
    rightFixture.offsetFromBodyCenter.y = ConstVals.PPM / 5f
    rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
    body.addFixture(rightFixture)

    rightFixture.shape.color = Color.ORANGE
    shapes.add { rightFixture.shape }

    // damageable fixture
    val damageableFixture =
        Fixture(
            GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
            FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // damager fixture
    val damagerFixture =
        Fixture(
            GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
            FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    shapes.add { damagerFixture.shape }

    // pre-process
    body.preProcess = Updatable {
      body.physics.gravity.y =
          if (body.isSensing(BodySense.FEET_ON_GROUND)) 0f else GRAVITY * ConstVals.PPM

      if (onWall) {
        if (!wasOnWall) body.physics.velocity.x = 0f
        body.physics.velocity.y = VEL_Y * ConstVals.PPM
      } else {
        if (wasOnWall) body.y += ConstVals.PPM / 10f
        body.physics.velocity.x = VEL_X * ConstVals.PPM * facing.value
      }
    }

    addComponent(DrawableShapeComponent(this, debugShapeSuppliers = shapes, debug = true))

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpriteComponent(): SpriteComponent {
    val sprite = GameSprite()
    sprite.setSize(1.5f * ConstVals.PPM)
    sprite.setOriginCenter()

    val spriteComponent = SpriteComponent(this, "suctionRoller" to sprite)
    spriteComponent.putUpdateFunction("suctionRoller") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setFlip(facing == Facing.RIGHT, false)

      val position =
          if (onWall) {
            if (facing == Facing.LEFT) Position.CENTER_LEFT else Position.CENTER_RIGHT
          } else Position.BOTTOM_CENTER

      val bodyPosition =
          if (onWall) {
            if (position == Position.CENTER_LEFT) body.getCenterLeftPoint()
            else body.getCenterRightPoint()
          } else body.getBottomCenterPoint()

      _sprite.setPosition(bodyPosition, position)

      _sprite.rotation =
          if (onWall) {
            if (facing == Facing.LEFT) -90f else 90f
          } else 0f
    }
    return spriteComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val animation = Animation(textureRegion!!, 1, 5, 0.1f, true)
    val animator = Animator(animation)
    return AnimationsComponent(this, animator)
  }
}
