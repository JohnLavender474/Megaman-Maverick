package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Loop
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
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class Robbit(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  companion object {
    const val TAG = "Robbit"
    private var atlas: TextureAtlas? = null
    private const val STAND_DUR = 1f
    private const val CROUCH_DUR = 0.2f
    private const val JUMP_DUR = 0.25f
    private const val G_GRAV = -0.0015f
    private const val GRAV = -0.375f
    private const val JUMP_X = 6f
    private const val JUMP_Y = 12f
  }

  enum class RobbitState {
    STANDING,
    CROUCHING,
    JUMPING
  }

  override var facing = Facing.RIGHT

  override val damageNegotiations = objectMapOf<KClass<out IDamager>, Int>()

  private val robbitLoop = Loop(RobbitState.values().toGdxArray())
  private val robbitTimers =
      objectMapOf(
          RobbitState.STANDING to Timer(STAND_DUR),
          RobbitState.CROUCHING to Timer(CROUCH_DUR),
          RobbitState.JUMPING to Timer(JUMP_DUR))
  private val robbitTimer: Timer
    get() = robbitTimers[robbitLoop.getCurrent()]!!

  override fun init() {
    super.init()
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
    body.setBottomCenterToPoint(spawn)
    robbitLoop.reset()
    robbitTimer.reset()
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(1.5f * ConstVals.PPM)

    val debugShapes = Array<() -> IDrawableShape?>()

    // body fixture
    val bodyFixture = Fixture(GameRectangle().setSize(1.5f * ConstVals.PPM), FixtureType.BODY)
    body.addFixture(bodyFixture)
    bodyFixture.shape.color = Color.BLUE
    debugShapes.add { bodyFixture.shape }

    // feet fixture
    val feetFixture =
        Fixture(GameRectangle().setSize(ConstVals.PPM / 4f, 0.2f * ConstVals.PPM), FixtureType.FEET)
    feetFixture.offsetFromBodyCenter.y = -0.75f * ConstVals.PPM
    body.addFixture(feetFixture)
    feetFixture.shape.color = Color.GREEN
    debugShapes.add { feetFixture.shape }

    // damageable fixture
    val damageableFixture =
        Fixture(GameRectangle().setSize(1.5f * ConstVals.PPM), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // damager fixture
    val damagerFixture = Fixture(GameRectangle().setSize(1.5f * ConstVals.PPM), FixtureType.DAMAGER)
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
      if (robbitLoop.getCurrent() != RobbitState.JUMPING)
          facing = if (megaman.body.x >= body.x) Facing.RIGHT else Facing.LEFT

      robbitTimer.update(it)
      if (robbitTimer.isJustFinished()) {
        val currentState = robbitLoop.getCurrent()
        GameLogger.debug(TAG, "Current state: $currentState")

        val nextState = robbitLoop.next()
        GameLogger.debug(TAG, "Transitioning to state: $nextState")

        if (nextState == RobbitState.JUMPING) {
          body.physics.velocity.x = JUMP_X * ConstVals.PPM * facing.value
          body.physics.velocity.y = JUMP_Y * ConstVals.PPM
        }

        robbitTimer.reset()
      }
    }
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(4f * ConstVals.PPM, 3.5f * ConstVals.PPM)

    val spritesComponent = SpritesComponent(this, "robbit" to sprite)
    spritesComponent.putUpdateFunction("robbit") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
      _sprite.setFlip(isFacing(Facing.LEFT), false)
    }

    return spritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String? = {
      when (robbitLoop.getCurrent()) {
        RobbitState.STANDING -> "Stand"
        RobbitState.CROUCHING -> "Crouch"
        RobbitState.JUMPING -> "Jump"
      }
    }
    val animations =
        objectMapOf<String, IAnimation>(
            "Stand" to Animation(atlas!!.findRegion("Robbit/Stand")),
            "Crouch" to Animation(atlas!!.findRegion("Robbit/Crouch")),
            "Jump" to Animation(atlas!!.findRegion("Robbit/Jump")))
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }
}
