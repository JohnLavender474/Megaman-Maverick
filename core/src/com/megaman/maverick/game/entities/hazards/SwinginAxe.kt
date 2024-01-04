package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.engine.common.enums.Direction
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameLine
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.motion.MotionComponent
import com.engine.motion.Pendulum
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class SwinginAxe(game: MegamanMaverickGame) :
    GameEntity(game), ISpriteEntity, IBodyEntity, IMotionEntity {

  companion object {
    const val TAG = "SwinginAxe"
    private var textureRegion: TextureRegion? = null
    private const val DEBUG_SWING_ROTATION = false
    private const val LENGTH = 2.25f
    private const val PENDULUM_GRAVITY = 10f
    private const val DEBUG_SWING_ROTATION_SPEED = 1f
  }

  private lateinit var deathCircle: GameCircle
  private lateinit var shieldCircle: GameCircle

  private lateinit var pendulum: Pendulum

  private val debugSwingRotationTimer = Timer(DEBUG_SWING_ROTATION_SPEED)

  override fun init() {
    if (textureRegion == null)
        textureRegion =
            game.assMan.getTextureRegion(
                TextureAsset.HAZARDS_1.source, "SwingingAxe_HandleEndCentered")
    addComponent(DrawableShapesComponent(this, debug = true))
    addComponent(defineBodyComponent())
    addComponent(defineSpritesComponent())
    addComponent(MotionComponent(this))
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)
    clearMotions()
    val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
    body.setCenter(bounds.getCenter())
    setPendulum(bounds)
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(2f * ConstVals.PPM)

    val shapesComponent = getComponent(DrawableShapesComponent::class)!!

    // death circle
    deathCircle = GameCircle()
    deathCircle.setRadius(ConstVals.PPM.toFloat())
    val deathFixture = Fixture(deathCircle, FixtureType.DEATH)
    deathFixture.attachedToBody = false
    body.addFixture(deathFixture)
    shapesComponent.debugShapeSuppliers.add { deathCircle }

    // shield fixture
    shieldCircle = GameCircle()
    shieldCircle.setRadius(ConstVals.PPM.toFloat())
    val shieldFixture = Fixture(shieldCircle, FixtureType.SHIELD)
    shieldFixture.attachedToBody = false
    shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
    body.addFixture(shieldFixture)
    shapesComponent.debugShapeSuppliers.add { shieldCircle }

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpritesComponent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(8f * ConstVals.PPM)
    sprite.setRegion(textureRegion!!)

    val spritesComponent = SpritesComponent(this, "axe" to sprite)
    spritesComponent.putUpdateFunction("axe") { delta, _sprite ->
      _sprite as GameSprite
      val center = pendulum.anchor
      _sprite.setCenter(center.x, center.y)
      _sprite.setOriginCenter()
      _sprite.setFlip(flipX = false, flipY = true)
      if (DEBUG_SWING_ROTATION) {
        debugSwingRotationTimer.update(delta)
        if (debugSwingRotationTimer.isFinished()) {
          _sprite.rotation -= 1f
          debugSwingRotationTimer.reset()
        }
      } else _sprite.rotation = MathUtils.radiansToDegrees * pendulum.angle * -1
    }

    return spritesComponent
  }

  private fun setPendulum(bounds: GameRectangle) {
    pendulum =
        Pendulum(
            LENGTH * ConstVals.PPM, PENDULUM_GRAVITY * ConstVals.PPM, bounds.getCenter(), 1 / 60f)
    putMotion(
        ConstKeys.PENDULUM,
        MotionComponent.MotionDefinition(
            motion = pendulum,
            function = { value, _ ->
              deathCircle.setCenter(value)
              shieldCircle.setCenter(value)
            }))

    val shapesComponent = getComponent(DrawableShapesComponent::class)!!

    shapesComponent.debugShapeSuppliers.add {
      val line = GameLine(pendulum.anchor, pendulum.getMotionValue())
      line.color = Color.DARK_GRAY
      line.shapeType = ShapeRenderer.ShapeType.Line
      line.thickness = ConstVals.PPM / 8f
      line
    }

    val circle1 = GameCircle()
    circle1.setRadius(ConstVals.PPM / 4f)
    circle1.shapeType = ShapeRenderer.ShapeType.Filled
    circle1.color = Color.BROWN
    shapesComponent.debugShapeSuppliers.add { circle1.setCenter(pendulum.anchor) }

    val circle2 = GameCircle()
    circle2.setRadius(ConstVals.PPM / 4f)
    circle2.shapeType = ShapeRenderer.ShapeType.Line
    circle2.color = Color.DARK_GRAY
    shapesComponent.debugShapeSuppliers.add { circle2.setCenter(pendulum.getMotionValue()) }
  }
}
