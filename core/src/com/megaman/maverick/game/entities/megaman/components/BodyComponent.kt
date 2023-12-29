package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.GameLogger
import com.engine.common.interfaces.Updatable
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.shapes.DrawableShapeComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.BButtonTask
import com.megaman.maverick.game.world.*

const val MEGAMAN_BODY_COMPONENT_TAG = "MegamanBodyComponentTag"

internal fun Megaman.defineBodyComponent(): BodyComponent {
  val body = Body(BodyType.DYNAMIC)
  body.width = .75f * ConstVals.PPM
  body.physics.takeFrictionFromOthers = true
  body.physics.defaultFrictionOnSelf =
      Vector2(ConstVals.STANDARD_RESISTANCE_X, ConstVals.STANDARD_RESISTANCE_Y)
  body.physics.velocityClamp.set(15f * ConstVals.PPM, 25f * ConstVals.PPM)

  // drawable shapes
  val shapes = Array<() -> IDrawableShape>()

  // player fixture
  val playerFixture = Fixture(GameRectangle().setWidth(.8f * ConstVals.PPM), FixtureType.PLAYER)
  body.fixtures.add(FixtureType.PLAYER to playerFixture)
  playerFixture.shape.color = Color.WHITE
  shapes.add { playerFixture.shape }

  // body fixture
  val bodyFixture = Fixture(GameRectangle().setWidth(.8f * ConstVals.PPM), FixtureType.BODY)
  body.fixtures.add(FixtureType.BODY to bodyFixture)
  bodyFixture.shape.color = Color.BLUE
  shapes.add { bodyFixture.shape }

  val onBounce = {
    if (!body.isSensing(BodySense.IN_WATER) /* TODO: && has(MegaAbility.AIR_DASH) */)
        bButtonTask = BButtonTask.AIR_DASH
  }

  // feet fixture
  val feetFixture =
      Fixture(
          GameRectangle().setWidth(.6f * ConstVals.PPM).setHeight(.15f * ConstVals.PPM),
          FixtureType.FEET)
  feetFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  body.fixtures.add(FixtureType.FEET to feetFixture)
  feetFixture.shape.color = Color.GREEN
  shapes.add { feetFixture.shape }

  // head fixture
  val headFixture =
      Fixture(
          GameRectangle().setWidth(.6f * ConstVals.PPM).setHeight(.15f * ConstVals.PPM),
          FixtureType.HEAD)
  headFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  body.fixtures.add(FixtureType.HEAD to headFixture)
  headFixture.shape.color = Color.ORANGE
  shapes.add { headFixture.shape }

  // left fixture
  val leftFixture = Fixture(GameRectangle().setWidth(.2f * ConstVals.PPM), FixtureType.SIDE)
  leftFixture.offsetFromBodyCenter.x = -.4f * ConstVals.PPM
  leftFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  leftFixture.properties.put(ConstKeys.SIDE, ConstKeys.LEFT)
  body.fixtures.add(FixtureType.SIDE to leftFixture)
  leftFixture.shape.color = Color.YELLOW
  shapes.add { leftFixture.shape }

  // right fixture
  val rightFixture = Fixture(GameRectangle().setWidth(.2f * ConstVals.PPM), FixtureType.SIDE)
  rightFixture.offsetFromBodyCenter.x = .4f * ConstVals.PPM
  rightFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  rightFixture.properties.put(ConstKeys.SIDE, ConstKeys.RIGHT)
  body.fixtures.add(FixtureType.SIDE to rightFixture)
  rightFixture.shape.color = Color.YELLOW
  shapes.add { rightFixture.shape }

  // damageable fixture
  val damageableFixture =
      Fixture(GameRectangle().setSize(.8f * ConstVals.PPM), FixtureType.DAMAGEABLE)
  body.fixtures.add(FixtureType.DAMAGEABLE to damageableFixture)
  damageableFixture.shape.color = Color.RED
  shapes.add { damageableFixture.shape }

  // water listener fixture
  val waterListenerFixture =
      Fixture(
          GameRectangle().setSize(.8f * ConstVals.PPM, ConstVals.PPM / 4f),
          FixtureType.WATER_LISTENER)
  body.fixtures.add(FixtureType.WATER_LISTENER to waterListenerFixture)
  waterListenerFixture.shape.color = Color.PURPLE
  shapes.add { waterListenerFixture.shape }

  // pre-process
  val logPositionTimer = Timer(2f)
  body.preProcess = Updatable {
    logPositionTimer.update(it)
    if (logPositionTimer.isFinished()) {
      GameLogger.debug(MEGAMAN_BODY_COMPONENT_TAG, "body.preProcess(): body = $body")
      logPositionTimer.reset()
    }

    headFixture.offsetFromBodyCenter.y = (if (upsideDown) -.4f else .4f) * ConstVals.PPM

    if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
      body.height = .45f * ConstVals.PPM
      feetFixture.offsetFromBodyCenter.y = (if (upsideDown) .25f else -.25f) * ConstVals.PPM
      (leftFixture.shape as Rectangle).setHeight(.2f * ConstVals.PPM)
      (rightFixture.shape as Rectangle).setHeight(.2f * ConstVals.PPM)
    } else {
      body.height = .95f * ConstVals.PPM
      feetFixture.offsetFromBodyCenter.y = (if (upsideDown) .5f else -.5f) * ConstVals.PPM
      (leftFixture.shape as Rectangle).setHeight(.5f * ConstVals.PPM)
      (rightFixture.shape as Rectangle).setHeight(.5f * ConstVals.PPM)
    }

    (bodyFixture.shape as Rectangle).set(body)
    (playerFixture.shape as Rectangle).set(body)

    val wallSlidingOnIce =
        isBehaviorActive(BehaviorType.WALL_SLIDING) &&
            (body.isSensingAny(BodySense.SIDE_TOUCHING_ICE_LEFT, BodySense.SIDE_TOUCHING_ICE_RIGHT))

    val gravityY =
        if (body.isSensing(BodySense.IN_WATER))
            if (wallSlidingOnIce) waterIceGravity else waterGravity
        else if (wallSlidingOnIce) iceGravity
        else if (body.isSensing(BodySense.FEET_ON_GROUND)) groundGravity else gravity

    body.physics.gravity.y = gravityY * ConstVals.PPM
  }

  // add drawable shape component
  addComponent(DrawableShapeComponent(this, debugShapeSuppliers = shapes, debug = true))

  return BodyComponentCreator.create(this, body)
}
