package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.interfaces.Updatable
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapeComponent
import com.engine.drawables.shapes.DrawableShapeHandle
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
  body.physics.defaultFrictionOnSelf =
      Vector2(ConstVals.STANDARD_RESISTANCE_X, ConstVals.STANDARD_RESISTANCE_Y)
  body.physics.velocityClamp.set(15f * ConstVals.PPM, 25f * ConstVals.PPM)

  // drawable shapes
  val shapes = Array<DrawableShapeHandle>()

  // player fixture
  val playerFixture = Fixture(GameRectangle().setWidth(.8f * ConstVals.PPM), FixtureType.PLAYER)
  body.fixtures.put(FixtureType.PLAYER, playerFixture)
  shapes.add(DrawableShapeHandle(playerFixture.shape, ShapeRenderer.ShapeType.Line))

  // body fixture
  val bodyFixture = Fixture(GameRectangle().setWidth(.8f * ConstVals.PPM), FixtureType.BODY)
  body.fixtures.put(FixtureType.BODY, bodyFixture)
  shapes.add(DrawableShapeHandle(bodyFixture.shape, ShapeRenderer.ShapeType.Line))

  val onBounce = {
    if (!body.isSensing(BodySense.IN_WATER) /* TODO: && has(MegaAbility.AIR_DASH) */) {
      aButtonTask = AButtonTask.AIR_DASH
    }
  }

  // feet fixture
  val feetFixture =
      Fixture(
          GameRectangle().setWidth(.6f * ConstVals.PPM).setHeight(.15f * ConstVals.PPM),
          FixtureType.FEET)
  feetFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  body.fixtures.put(FixtureType.FEET, feetFixture)
  shapes.add(DrawableShapeHandle(feetFixture.shape, ShapeRenderer.ShapeType.Line))

  // head fixture
  val headFixture =
      Fixture(
          GameRectangle().setWidth(.6f * ConstVals.PPM).setHeight(.15f * ConstVals.PPM),
          FixtureType.HEAD)
  headFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  body.fixtures.put(FixtureType.HEAD, headFixture)
  shapes.add(DrawableShapeHandle(headFixture.shape, ShapeRenderer.ShapeType.Line))

  // left fixture
  val leftFixture = Fixture(GameRectangle().setWidth(.2f * ConstVals.PPM), FixtureType.SIDE)
  leftFixture.offsetFromBodyCenter.x = -.4f * ConstVals.PPM
  leftFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  leftFixture.properties.put(ConstKeys.SIDE, ConstKeys.LEFT)
  body.fixtures.put(FixtureType.SIDE, leftFixture)
  shapes.add(DrawableShapeHandle(leftFixture.shape, ShapeRenderer.ShapeType.Line))

  // right fixture
  val rightFixture = Fixture(GameRectangle().setWidth(.2f * ConstVals.PPM), FixtureType.SIDE)
  rightFixture.offsetFromBodyCenter.x = .4f * ConstVals.PPM
  rightFixture.properties.put(ConstKeys.BOUNCE, onBounce)
  rightFixture.properties.put(ConstKeys.SIDE, ConstKeys.RIGHT)
  body.fixtures.put(FixtureType.SIDE, rightFixture)
  shapes.add(DrawableShapeHandle(rightFixture.shape, ShapeRenderer.ShapeType.Line))

  // damageable fixture
  val damageableFixture =
      Fixture(GameRectangle().setSize(.8f * ConstVals.PPM), FixtureType.DAMAGEABLE)
  body.fixtures.put(FixtureType.DAMAGEABLE, damageableFixture)
  shapes.add(DrawableShapeHandle(damageableFixture.shape, ShapeRenderer.ShapeType.Line))

  // water listener fixture
  val waterListenerFixture =
      Fixture(
          GameRectangle().setSize(.8f * ConstVals.PPM, ConstVals.PPM / 4f),
          FixtureType.WATER_LISTENER)
  body.fixtures.put(FixtureType.WATER_LISTENER, waterListenerFixture)
  shapes.add(DrawableShapeHandle(waterListenerFixture.shape, ShapeRenderer.ShapeType.Line))

  // pre-process
  body.preProcess = Updatable {
    headFixture.offsetFromBodyCenter.y = (if (upsideDown) -.4f else .4f) * ConstVals.PPM

    if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
      body.height = .45f * ConstVals.PPM
      feetFixture.offsetFromBodyCenter.y = (if (upsideDown) .25f else -.25f) * ConstVals.PPM
      (leftFixture.shape as Rectangle).setHeight(.2f * ConstVals.PPM)
      (rightFixture.shape as Rectangle).setHeight(.2f * ConstVals.PPM)
    } else {
      body.height = .95f * ConstVals.PPM
      feetFixture.offsetFromBodyCenter.y = (if (upsideDown) .45f else -.45f) * ConstVals.PPM
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

  // set entity for all fixtures
  body.fixtures.values().forEach { it.setEntity(this) }

  // add drawable shape component
  addComponent(DrawableShapeComponent(this, shapes))

  return BodyComponent(this, body)
}
