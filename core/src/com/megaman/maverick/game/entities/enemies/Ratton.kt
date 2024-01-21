package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.interfaces.isFacing
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
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class Ratton(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  companion object {
    private const val STAND_DUR = 0.75f
    private const val G_GRAV = -0.0015f
    private const val GRAV = -0.375f
    private const val JUMP_X = 15f
    private const val JUMP_Y = 18f
    private var atlas: TextureAtlas? = null
  }

  override var facing = Facing.RIGHT

  private val standTimer = Timer(STAND_DUR)

  override fun init() {
    super.init()
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
  }

  override val damageNegotiations = objectMapOf<KClass<out IDamager>, Int>()

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(ConstVals.PPM.toFloat())

    val debugShapes = Array<() -> IDrawableShape?>()

    // body fixture
    val bodyFixture = Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.BODY)
    body.addFixture(bodyFixture)
    bodyFixture.shape.color = Color.BLUE
    debugShapes.add { bodyFixture.shape }

    // feet fixture
    val feetFixture =
        Fixture(
            GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.2f * ConstVals.PPM),
            FixtureType.FEET)
    feetFixture.offsetFromBodyCenter.y = -0.5f * ConstVals.PPM
    body.addFixture(feetFixture)
    feetFixture.shape.color = Color.GREEN
    debugShapes.add { feetFixture.shape }

    // TODO: create head fixture, bounce megaman as reference to DK Country rat boss!

    // damageable fixture
    val damageableFixture =
        Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // damager fixture
    val damagerFixture =
        Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

    body.preProcess = Updatable {
      body.physics.gravity.y =
          ConstVals.PPM * (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV)
    }

    return BodyComponentCreator.create(this, body)
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      if (body.isSensing(BodySense.FEET_ON_GROUND)) {
        standTimer.update(it)
        facing = if (megaman.body.x > body.x) Facing.RIGHT else Facing.LEFT
      }

      if (standTimer.isFinished()) {
        standTimer.reset()
        body.physics.velocity.x += JUMP_X * facing.value * ConstVals.PPM
        body.physics.velocity.y += JUMP_Y * ConstVals.PPM
      }
    }
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(2f * ConstVals.PPM, 1.75f * ConstVals.PPM)

    val spritesComponent = SpritesComponent(this, "ratton" to sprite)
    spritesComponent.putUpdateFunction("ratton") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
      _sprite.setFlip(isFacing(Facing.LEFT), false)
    }

    return spritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String? = {
      if (body.isSensing(BodySense.FEET_ON_GROUND)) "Stand" else "Jump"
    }
    val animations =
        objectMapOf<String, IAnimation>(
            "Stand" to
                Animation(atlas!!.findRegion("Ratton/Stand"), 1, 2, gdxArrayOf(1.5f, 0.15f), true),
            "Jump" to Animation(atlas!!.findRegion("Ratton/Jump"), 1, 2, 0.1f, true))
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }
}
