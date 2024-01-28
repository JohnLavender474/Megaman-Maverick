package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
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
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class Elecn(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

  enum class ElecnState {
    MOVING,
    CHARGING,
    SHOCKING
  }

  companion object {
    const val TAG = "Elecn"
    private var atlas: TextureAtlas? = null
    private const val MOVING_DURATION = 0.5f
    private const val CHARGING_DURATION = 1f
    private const val SHOCKING_DURATION = 0.15f
    private const val ZIG_ZAG_DURATION = 0.5f
    private const val X_VEL = 2f
    private const val Y_VEL = 0.2f
    private const val SHOCK_VEL = 10f
  }

  override var facing = Facing.LEFT

  override val damageNegotiations = objectMapOf<KClass<out IDamager>, Int>()

  private val elecnLoop = Loop(ElecnState.values().toGdxArray())
  private val elecnTimers =
      objectMapOf(
          ElecnState.MOVING to Timer(MOVING_DURATION),
          ElecnState.CHARGING to Timer(CHARGING_DURATION),
          ElecnState.SHOCKING to Timer(SHOCKING_DURATION))
  private val elecnTimer: Timer
    get() = elecnTimers[elecnLoop.getCurrent()]!!

  private val zigzagTimer = Timer(ZIG_ZAG_DURATION)

  private var zigzagUp = false

  override fun init() {
    super.init()
    if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
    addComponent(defineAnimationsComponent())
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
    body.setCenter(spawn)
    elecnLoop.reset()
    elecnTimer.reset()
    facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, "LEFT", String::class))
  }

  override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    super.defineUpdatablesComponent(updatablesComponent)
    updatablesComponent.add {
      if (elecnLoop.getCurrent() != ElecnState.SHOCKING) {
        zigzagTimer.update(it)
        if (zigzagTimer.isFinished()) {
          zigzagUp = !zigzagUp
          zigzagTimer.reset()
        }
      }

      elecnTimer.update(it)
      if (elecnTimer.isFinished()) {
        val previous = elecnLoop.getCurrent()
        val state = elecnLoop.next()
        GameLogger.debug(TAG, "Setting state from $previous to $state")
        if (state == ElecnState.SHOCKING) shock()
        elecnTimer.reset()
      }
    }
  }

  override fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(0.85f * ConstVals.PPM)

    val debugShapes = Array<() -> IDrawableShape?>()
    debugShapes.add { body }

    // body fixture
    val bodyFixture = Fixture(GameRectangle().setSize(0.85f * ConstVals.PPM), FixtureType.BODY)
    body.addFixture(bodyFixture)

    // damager fixture
    val damagerFixture =
        Fixture(GameRectangle().setSize(0.85f * ConstVals.PPM), FixtureType.DAMAGER)
    body.addFixture(damagerFixture)

    // damageable fixture
    val damageableFixture =
        Fixture(GameRectangle().setSize(0.85f * ConstVals.PPM), FixtureType.DAMAGEABLE)
    body.addFixture(damageableFixture)

    // side fixture
    val sideFixture =
        Fixture(
            GameRectangle().setSize(0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM), FixtureType.SIDE)
    body.addFixture(sideFixture)

    addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

    body.preProcess = Updatable {
      if (isFacing(Facing.LEFT)) {
        sideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        sideFixture.offsetFromBodyCenter.x = -0.5f * ConstVals.PPM
      } else {
        sideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        sideFixture.offsetFromBodyCenter.x = 0.5f * ConstVals.PPM
      }

      body.physics.velocity =
          if (elecnLoop.getCurrent() == ElecnState.SHOCKING) Vector2()
          else {
            val x = X_VEL * ConstVals.PPM * facing.value
            val y = Y_VEL * ConstVals.PPM * (if (zigzagUp) 1 else -1)
            Vector2(x, y)
          }
    }

    body.postProcess = Updatable {
      if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT))
          facing = Facing.RIGHT
      else if (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
          facing = Facing.LEFT
    }

    return BodyComponentCreator.create(this, body)
  }

  override fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(2f * ConstVals.PPM)

    val spritesComponent = SpritesComponent(this, "elecn" to sprite)
    spritesComponent.putUpdateFunction("elecn") { _, _sprite ->
      _sprite as GameSprite
      val center = body.getCenter()
      _sprite.setCenter(center.x, center.y)
      _sprite.setFlip(isFacing(Facing.LEFT), false)
    }

    return spritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val keySupplier: () -> String? = {
      when (elecnLoop.getCurrent()) {
        ElecnState.MOVING -> "moving"
        ElecnState.CHARGING -> "charging"
        ElecnState.SHOCKING -> "shocking"
      }
    }
    val animations =
        objectMapOf<String, IAnimation>(
            "moving" to Animation(atlas!!.findRegion("Elecn/Elecn1")),
            "charging" to Animation(atlas!!.findRegion("Elecn/Elecn2"), 1, 2, 0.15f, true),
            "shocking" to Animation(atlas!!.findRegion("Elecn/Elecn3")))
    val animator = Animator(keySupplier, animations)
    return AnimationsComponent(this, animator)
  }

  private fun shock() {
    requestToPlaySound(SoundAsset.MM3_ELECTRIC_PULSE_SOUND, false)

    Position.values().forEach {
      if (it == Position.CENTER) return@forEach

      val xVel = ConstVals.PPM * (if (it.x == 1) 0f else if (it.x > 1) SHOCK_VEL else -SHOCK_VEL)
      val yVel = ConstVals.PPM * (if (it.y == 1) 0f else if (it.y > 1) SHOCK_VEL else -SHOCK_VEL)

      val shock = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ELECTRIC_BALL)!!
      game.gameEngine.spawn(
          shock,
          props(
              ConstKeys.POSITION to body.getTopCenterPoint(),
              ConstKeys.X to xVel,
              ConstKeys.Y to yVel))
    }
  }
}
