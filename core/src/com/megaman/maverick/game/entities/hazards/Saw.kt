package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameLine
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.motion.MotionComponent
import com.engine.motion.MotionComponent.MotionDefinition
import com.engine.motion.Pendulum
import com.engine.motion.RotatingLine
import com.engine.motion.Trajectory
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

class Saw(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpriteEntity, IMotionEntity {

  companion object {
    const val PENDULUM_TYPE = "P"
    const val ROTATION_TYPE = "R"
    const val TRAJECTORY_TYPE = "T"

    private var region: TextureRegion? = null

    private const val LENGTH = 3f
    private const val ROTATION_SPEED = 2f
    private const val PENDULUM_GRAVITY = 10f
  }

  override fun init() {
    if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "Saw")

    addComponent(defineBodyComponent())
    addComponent(defineSpritesCompoent())
    addComponent(defineAnimationsComponent())
    addComponent(MotionComponent(this))
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    getMotions().clear()

    val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
    val type = spawnProps.get(ConstKeys.TYPE) as String

    when (type) {
      PENDULUM_TYPE -> setToPendulum(bounds)
      ROTATION_TYPE -> setToRotation(bounds)
      TRAJECTORY_TYPE -> {
        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY) as String
        setToTrajectory(trajectory)
      }
    }
  }

  private fun setToPendulum(bounds: GameRectangle) {
    val pendulum =
        Pendulum(
            LENGTH * ConstVals.PPM, PENDULUM_GRAVITY * ConstVals.PPM, bounds.getCenter(), 1 / 60f)
    putMotion(ConstKeys.PENDULUM, MotionDefinition(pendulum) { body.setCenter(it) })

    val shapes = Array<() -> IDrawableShape>()

    shapes.add {
      val line = GameLine(pendulum.anchor, pendulum.getMotionValue())
      line.color = Color.DARK_GRAY
      line.shapeType = ShapeRenderer.ShapeType.Filled
      line.thickness = ConstVals.PPM / 8f
      line
    }

    val circle1 = GameCircle(pendulum.anchor.x, pendulum.anchor.y, ConstVals.PPM / 4f)
    circle1.shapeType = ShapeRenderer.ShapeType.Filled
    circle1.color = Color.DARK_GRAY
    shapes.add { circle1 }

    val circle2 = GameCircle()
    circle2.setRadius(ConstVals.PPM / 4f)
    circle2.shapeType = ShapeRenderer.ShapeType.Filled
    circle2.color = Color.DARK_GRAY
    shapes.add { circle2.setCenter(pendulum.getMotionValue()) }

    addComponent(DrawableShapesComponent(this, shapes))
  }

  private fun setToRotation(bounds: GameRectangle) {
    val rotation =
        RotatingLine(bounds.getCenter(), LENGTH * ConstVals.PPM, ROTATION_SPEED * ConstVals.PPM)
    putMotion(ConstKeys.ROTATION, MotionDefinition(rotation) { body.setCenter(it) })

    val shapes = Array<() -> IDrawableShape>()

    shapes.add {
      val line = GameLine(rotation.getOrigin(), rotation.getMotionValue())
      line.color = Color.DARK_GRAY
      line.shapeType = ShapeRenderer.ShapeType.Filled
      line.thickness = ConstVals.PPM / 8f
      line
    }

    val circle1 = GameCircle()
    circle1.setRadius(ConstVals.PPM / 4f)
    circle1.color = Color.DARK_GRAY
    circle1.shapeType = ShapeRenderer.ShapeType.Filled

    val circle2 = GameCircle()
    circle2.setRadius(ConstVals.PPM / 4f)
    circle2.color = Color.DARK_GRAY
    circle2.shapeType = ShapeRenderer.ShapeType.Filled

    shapes.add { circle1.setPosition(rotation.getOrigin()) }
    shapes.add { circle2.setCenter(rotation.getMotionValue()) }

    addComponent(DrawableShapesComponent(this, shapes))
  }

  private fun setToTrajectory(trajectoryDefinition: String) {
    val trajectory = Trajectory(trajectoryDefinition, ConstVals.PPM)
    putMotion(ConstKeys.TRAJECTORY, MotionDefinition(trajectory) { body.setCenter(it) })
  }

  private fun defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.ABSTRACT)
    body.setSize(2f * ConstVals.PPM)

    // death fixture
    val deathCircle = GameCircle()
    deathCircle.setRadius(ConstVals.PPM.toFloat())
    val deathFixture = Fixture(deathCircle, FixtureType.DEATH)
    body.addFixture(deathFixture)

    // shield fixture
    val shieldCircle = GameCircle()
    shieldCircle.setRadius(ConstVals.PPM.toFloat())
    val shieldFixture = Fixture(shieldCircle, FixtureType.SHIELD)
    shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
    body.addFixture(shieldFixture)

    return BodyComponentCreator.create(this, body)
  }

  private fun defineSpritesCompoent(): SpritesComponent {
    val sprite = GameSprite()
    sprite.setSize(2f * ConstVals.PPM)
    val SpritesComponent = SpritesComponent(this, "saw" to sprite)
    SpritesComponent.putUpdateFunction("saw") { _, _sprite ->
      _sprite as GameSprite
      _sprite.setPosition(body.getCenter(), Position.CENTER)
    }
    return SpritesComponent
  }

  private fun defineAnimationsComponent(): AnimationsComponent {
    val animation = Animation(region!!, 1, 2, 0.1f, true)
    val animator = Animator(animation)
    return AnimationsComponent(this, animator)
  }
}
