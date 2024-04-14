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
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.world.*

const val MEGAMAN_BODY_COMPONENT_TAG = "MegamanBodyComponent"

val Megaman.feet: IFixture
    get() {
        for (entry in body.fixtures) {
            if (entry.first == FixtureType.FEET) return entry.second
        }
        throw IllegalStateException("Cannot find Megaman's feet")
    }

internal fun Megaman.defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.color = Color.BROWN
    body.width = .75f * ConstVals.PPM
    body.physics.takeFrictionFromOthers = true

    val shapes = Array<() -> IDrawableShape?>()
    shapes.add { body.rotatedBounds }

    val playerFixture = Fixture(body, FixtureType.PLAYER, GameRectangle().setWidth(0.8f * ConstVals.PPM))
    body.addFixture(playerFixture)
    playerFixture.rawShape.color = Color.WHITE
    shapes.add { playerFixture.getShape() }

    val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setWidth(.8f * ConstVals.PPM))
    body.addFixture(bodyFixture)
    bodyFixture.rawShape.color = Color.BLUE
    shapes.add { bodyFixture.getShape() }

    val onBounce = {
        if (!body.isSensing(BodySense.IN_WATER) /* TODO: && has(MegaAbility.AIR_DASH) */)
            aButtonTask = AButtonTask.AIR_DASH
    }

    val feetFixture =
        Fixture(
            body,
            FixtureType.FEET,
            GameRectangle().setWidth(.6f * ConstVals.PPM).setHeight(.15f * ConstVals.PPM)
        )
    feetFixture.setRunnable(onBounce)
    body.addFixture(feetFixture)
    feetFixture.rawShape.color = Color.GREEN
    shapes.add { feetFixture.getShape() }

    val headFixture =
        Fixture(
            body,
            FixtureType.HEAD,
            GameRectangle().setWidth(.6f * ConstVals.PPM).setHeight(.15f * ConstVals.PPM)
        )
    headFixture.setRunnable(onBounce)
    body.addFixture(headFixture)
    headFixture.rawShape.color = Color.RED
    shapes.add { headFixture.getShape() }

    val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setWidth(.2f * ConstVals.PPM))
    leftFixture.setRunnable(onBounce)
    leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
    body.addFixture(leftFixture)
    leftFixture.rawShape.color = Color.YELLOW
    shapes.add { leftFixture.getShape() }

    val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setWidth(.2f * ConstVals.PPM))
    rightFixture.setRunnable(onBounce)
    rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
    body.addFixture(rightFixture)
    rightFixture.rawShape.color = Color.BLUE
    shapes.add { rightFixture.getShape() }

    val damageableFixture =
        Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(.8f * ConstVals.PPM))
    body.addFixture(damageableFixture)
    damageableFixture.rawShape.color = Color.RED

    val waterListenerFixture =
        Fixture(
            body,
            FixtureType.WATER_LISTENER,
            GameRectangle().setSize(.8f * ConstVals.PPM, ConstVals.PPM / 4f)
        )
    body.addFixture(waterListenerFixture)
    waterListenerFixture.rawShape.color = Color.PURPLE

    body.preProcess.put(ConstKeys.DEFAULT, Updatable {
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
                    when (directionRotation) {
                        Direction.UP,
                        Direction.DOWN -> Vector2(ConstVals.STANDARD_RESISTANCE_X, ConstVals.STANDARD_RESISTANCE_Y)

                        Direction.LEFT,
                        Direction.RIGHT -> Vector2(ConstVals.STANDARD_RESISTANCE_Y, ConstVals.STANDARD_RESISTANCE_X)
                    }

                body.physics.velocityClamp = if (isBehaviorActive(BehaviorType.RIDING_CART))
                    Vector2(
                        MegamanValues.CART_RIDE_MAX_SPEED * ConstVals.PPM,
                        MegamanValues.CLAMP_Y * ConstVals.PPM
                    )
                else Vector2(
                    MegamanValues.CLAMP_X * ConstVals.PPM,
                    MegamanValues.CLAMP_Y * ConstVals.PPM
                )
            }

            else -> {
                body.physics.gravity.set(gravityValue * ConstVals.PPM, 0f)
                body.physics.defaultFrictionOnSelf =
                    Vector2(ConstVals.STANDARD_RESISTANCE_Y, ConstVals.STANDARD_RESISTANCE_X)

                body.physics.velocityClamp = if (isBehaviorActive(BehaviorType.RIDING_CART))
                    Vector2(
                        MegamanValues.CLAMP_Y * ConstVals.PPM,
                        MegamanValues.CART_RIDE_MAX_SPEED * ConstVals.PPM
                    )
                else Vector2(
                    MegamanValues.CLAMP_Y * ConstVals.PPM,
                    MegamanValues.CLAMP_X * ConstVals.PPM
                )
            }
        }

        val rightSideOffset = Vector2(0.5f, 0f).scl(ConstVals.PPM.toFloat())
        rightFixture.offsetFromBodyCenter.set(rightSideOffset)
        leftFixture.offsetFromBodyCenter.set(rightSideOffset.scl(-1f))

        if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
            body.height = .45f * ConstVals.PPM
            headFixture.offsetFromBodyCenter.y = ConstVals.PPM / 4f
            feetFixture.offsetFromBodyCenter.y = -ConstVals.PPM / 4f
            (leftFixture.rawShape as Rectangle).setHeight(.25f * ConstVals.PPM)
            (rightFixture.rawShape as Rectangle).setHeight(.25f * ConstVals.PPM)
        } else {
            body.height = .95f * ConstVals.PPM
            headFixture.offsetFromBodyCenter.y = ConstVals.PPM / 2f
            feetFixture.offsetFromBodyCenter.y = -ConstVals.PPM / 2f
            (leftFixture.rawShape as Rectangle).setHeight(.6f * ConstVals.PPM)
            (rightFixture.rawShape as Rectangle).setHeight(.6f * ConstVals.PPM)
        }

        (bodyFixture.rawShape as Rectangle).set(body)
        (playerFixture.rawShape as Rectangle).set(body)
    })

    addComponent(DrawableShapesComponent(this, debugShapeSuppliers = shapes, debug = true))

    return BodyComponentCreator.create(this, body)
}
