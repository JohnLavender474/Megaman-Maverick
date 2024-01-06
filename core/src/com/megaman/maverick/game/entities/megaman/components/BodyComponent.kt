package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.enums.Direction
import com.engine.common.interfaces.Updatable
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.world.*

internal fun Megaman.defineBodyComponent(): BodyComponent {
  val body = Body(BodyType.DYNAMIC)
  body.width = .75f * ConstVals.PPM
  body.physics.takeFrictionFromOthers = true

  // drawable shapes
  val shapes = Array<() -> IDrawableShape?>()

  // player fixture
  val playerFixture = Fixture(GameRectangle().setWidth(0.8f * ConstVals.PPM), FixtureType.PLAYER)
  body.fixtures.add(FixtureType.PLAYER to playerFixture)
  playerFixture.shape.color = Color.WHITE
  // shapes.add { playerFixture.shape }

  // body fixture
  val bodyFixture = Fixture(GameRectangle().setWidth(.8f * ConstVals.PPM), FixtureType.BODY)
  body.fixtures.add(FixtureType.BODY to bodyFixture)
  bodyFixture.shape.color = Color.BLUE
  // shapes.add { bodyFixture.shape }

  val onBounce = {
    if (!body.isSensing(BodySense.IN_WATER) /* TODO: && has(MegaAbility.AIR_DASH) */)
        aButtonTask = AButtonTask.AIR_DASH
  }

  // feet fixture
  val feetFixture =
      Fixture(
          GameRectangle().setWidth(.6f * ConstVals.PPM).setHeight(.15f * ConstVals.PPM),
          FixtureType.FEET)
  feetFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  body.fixtures.add(FixtureType.FEET to feetFixture)
  feetFixture.shape.color = Color.GREEN
  shapes.add { feetFixture.bodyRelativeShape }

  // head fixture
  val headFixture =
      Fixture(
          GameRectangle().setWidth(.6f * ConstVals.PPM).setHeight(.15f * ConstVals.PPM),
          FixtureType.HEAD)
  headFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  body.fixtures.add(FixtureType.HEAD to headFixture)
  headFixture.shape.color = Color.RED
  shapes.add { headFixture.bodyRelativeShape }

  // left fixture
  val leftFixture = Fixture(GameRectangle().setWidth(.2f * ConstVals.PPM), FixtureType.SIDE)
  leftFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  leftFixture.properties.put(ConstKeys.SIDE, ConstKeys.LEFT)
  body.fixtures.add(FixtureType.SIDE to leftFixture)
  leftFixture.shape.color = Color.YELLOW
  shapes.add { leftFixture.bodyRelativeShape }

  // right fixture
  val rightFixture = Fixture(GameRectangle().setWidth(.2f * ConstVals.PPM), FixtureType.SIDE)
  rightFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  rightFixture.properties.put(ConstKeys.SIDE, ConstKeys.RIGHT)
  body.fixtures.add(FixtureType.SIDE to rightFixture)
  rightFixture.shape.color = Color.BLUE
  shapes.add { rightFixture.bodyRelativeShape }

  // damageable fixture
  val damageableFixture =
      Fixture(GameRectangle().setSize(.8f * ConstVals.PPM), FixtureType.DAMAGEABLE)
  body.fixtures.add(FixtureType.DAMAGEABLE to damageableFixture)
  damageableFixture.shape.color = Color.RED
  // shapes.add { damageableFixture.shape }

  // water listener fixture
  val waterListenerFixture =
      Fixture(
          GameRectangle().setSize(.8f * ConstVals.PPM, ConstVals.PPM / 4f),
          FixtureType.WATER_LISTENER)
  body.fixtures.add(FixtureType.WATER_LISTENER to waterListenerFixture)
  waterListenerFixture.shape.color = Color.PURPLE
  // shapes.add { waterListenerFixture.shape }

  // pre-process
  body.preProcess = Updatable {
    val wallSlidingOnIce =
        isBehaviorActive(BehaviorType.WALL_SLIDING) &&
            (body.isSensingAny(BodySense.SIDE_TOUCHING_ICE_LEFT, BodySense.SIDE_TOUCHING_ICE_RIGHT))
    val gravityValue =
        if (body.isSensing(BodySense.IN_WATER))
            if (wallSlidingOnIce) waterIceGravity else waterGravity
        else if (wallSlidingOnIce) iceGravity
        else if (body.isSensing(BodySense.FEET_ON_GROUND)) groundGravity else gravity

    when (directionRotation) {
      Direction.UP,
      Direction.DOWN -> {
        body.physics.gravity.set(0f, gravityValue * ConstVals.PPM)
        body.physics.defaultFrictionOnSelf =
            Vector2(ConstVals.STANDARD_RESISTANCE_X, ConstVals.STANDARD_RESISTANCE_Y)
        body.physics.velocityClamp.set(15f * ConstVals.PPM, 25f * ConstVals.PPM)
      }
      else -> {
        body.physics.gravity.set(gravityValue * ConstVals.PPM, 0f)
        body.physics.defaultFrictionOnSelf =
            Vector2(ConstVals.STANDARD_RESISTANCE_Y, ConstVals.STANDARD_RESISTANCE_X)
        body.physics.velocityClamp.set(25f * ConstVals.PPM, 15f * ConstVals.PPM)
      }
    }

    val rightSideOffset = Vector2(0.4f, 0f).scl(ConstVals.PPM.toFloat())
    rightFixture.offsetFromBodyCenter.set(rightSideOffset)
    leftFixture.offsetFromBodyCenter.set(rightSideOffset.scl(-1f))

    headFixture.offsetFromBodyCenter.y = 0.4f * ConstVals.PPM

    if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
      body.height = .45f * ConstVals.PPM
      feetFixture.offsetFromBodyCenter.y = -ConstVals.PPM / 4f
      (leftFixture.shape as Rectangle).setHeight(.2f * ConstVals.PPM)
      (rightFixture.shape as Rectangle).setHeight(.2f * ConstVals.PPM)
    } else {
      body.height = .95f * ConstVals.PPM
      feetFixture.offsetFromBodyCenter.y = -ConstVals.PPM / 2f
      (leftFixture.shape as Rectangle).setHeight(.5f * ConstVals.PPM)
      (rightFixture.shape as Rectangle).setHeight(.5f * ConstVals.PPM)
    }

    (bodyFixture.shape as Rectangle).set(body)
    (playerFixture.shape as Rectangle).set(body)
  }

  // add drawable bounds component
  addComponent(DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true))

  return BodyComponentCreator.create(this, body)
}
