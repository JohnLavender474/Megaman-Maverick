package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import com.engine.common.interfaces.Updatable
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapeHandle
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import com.megaman.maverick.game.world.setEntity

internal fun Megaman.defineBodyComponent(): BodyComponent {
  val body = Body(BodyType.DYNAMIC)
  body.width = .75f * ConstVals.PPM
  body.physics.takeFrictionFromOthers = true
  body.physics.velocityClamp.set(15f * ConstVals.PPM, 25f * ConstVals.PPM)

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
  }

  // setBodySense entity for fixtures
  body.fixtures.values().forEach { it.setEntity(this) }

  return BodyComponent(this, body)
}
