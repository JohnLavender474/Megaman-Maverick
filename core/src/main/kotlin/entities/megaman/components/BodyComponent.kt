package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.world.body.*

val Megaman.feetFixture: Fixture
    get() = body.getProperty(ConstKeys.FEET) as Fixture

val Megaman.leftSideFixture: Fixture
    get() = body.getProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", Fixture::class)!!

val Megaman.rightSideFixture: Fixture
    get() = body.getProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", Fixture::class)!!

val Megaman.headFixture: Fixture
    get() = body.getProperty(ConstKeys.HEAD, Fixture::class)!!

val Megaman.bodyFixture: Fixture
    get() = body.getProperty(ConstKeys.BODY, Fixture::class)!!

val Megaman.damageableFixture: Fixture
    get() = body.getProperty(ConstKeys.DAMAGEABLE, Fixture::class)!!

internal fun Megaman.defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.color = Color.BROWN
    body.width = 0.75f * ConstVals.PPM
    body.physics.takeFrictionFromOthers = true

    val debugShapes = Array<() -> IDrawableShape?>()
    debugShapes.add { body.getBodyBounds() }

    val playerFixture = Fixture(body, FixtureType.PLAYER, GameRectangle().setWidth(0.75f * ConstVals.PPM))
    body.addFixture(playerFixture)
    playerFixture.rawShape.color = Color.WHITE
    debugShapes.add { playerFixture.getShape() }

    val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setWidth(0.8f * ConstVals.PPM))
    body.addFixture(bodyFixture)
    bodyFixture.rawShape.color = Color.BLUE
    debugShapes.add { bodyFixture.getShape() }
    body.putProperty(ConstKeys.BODY, bodyFixture)

    val onBounce = {
        if (!body.isSensing(BodySense.IN_WATER) && has(MegaAbility.AIR_DASH)) aButtonTask = AButtonTask.AIR_DASH
    }

    val feetFixture =
        Fixture(body, FixtureType.FEET, GameRectangle().setWidth(0.6f * ConstVals.PPM).setHeight(0.15f * ConstVals.PPM))
    feetFixture.setRunnable(onBounce)
    body.addFixture(feetFixture)
    feetFixture.rawShape.color = Color.GREEN
    debugShapes.add { feetFixture.getShape() }
    body.putProperty(ConstKeys.FEET, feetFixture)

    val headFixture =
        Fixture(body, FixtureType.HEAD, GameRectangle().setWidth(0.6f * ConstVals.PPM).setHeight(0.15f * ConstVals.PPM))
    headFixture.setRunnable(onBounce)
    body.addFixture(headFixture)
    headFixture.rawShape.color = Color.RED
    debugShapes.add { headFixture.getShape() }
    body.putProperty(ConstKeys.HEAD, headFixture)

    val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setWidth(0.2f * ConstVals.PPM))
    leftFixture.setRunnable(onBounce)
    leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
    body.addFixture(leftFixture)
    leftFixture.rawShape.color = Color.YELLOW
    debugShapes.add { leftFixture.getShape() }
    body.putProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", leftFixture)

    val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setWidth(0.2f * ConstVals.PPM))
    rightFixture.setRunnable(onBounce)
    rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
    body.addFixture(rightFixture)
    rightFixture.rawShape.color = Color.BLUE
    debugShapes.add { rightFixture.getShape() }
    body.putProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", rightFixture)

    val damageableFixture =
        Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.9f * ConstVals.PPM))
    body.addFixture(damageableFixture)
    damageableFixture.rawShape.color = Color.RED
    debugShapes.add { damageableFixture.getShape() }
    body.putProperty(ConstKeys.DAMAGEABLE, damageableFixture)

    val waterListenerFixture = Fixture(
        body, FixtureType.WATER_LISTENER, GameRectangle().setSize(0.8f * ConstVals.PPM, ConstVals.PPM / 4f)
    )
    body.addFixture(waterListenerFixture)
    waterListenerFixture.rawShape.color = Color.PURPLE
    debugShapes.add { waterListenerFixture.getShape() }

    val teleporterListenerFixture =
        Fixture(body, FixtureType.TELEPORTER_LISTENER, GameRectangle().setWidth(0.5f * ConstVals.PPM))
    body.addFixture(teleporterListenerFixture)
    teleporterListenerFixture.rawShape.color = Color.CYAN
    debugShapes.add { teleporterListenerFixture.getShape() }

    body.preProcess.put(ConstKeys.DEFAULT, Updatable {
        if (!ready) {
            body.physics.velocity.setZero()
            return@Updatable
        }

        val wallSlidingOnIce = isBehaviorActive(BehaviorType.WALL_SLIDING) &&
            (body.isSensingAny(BodySense.SIDE_TOUCHING_ICE_LEFT, BodySense.SIDE_TOUCHING_ICE_RIGHT))
        var gravityValue =
            if (body.isSensing(BodySense.IN_WATER)) {
                if (wallSlidingOnIce) waterIceGravity else waterGravity
            } else if (wallSlidingOnIce) iceGravity
            // TODO: else if (isBehaviorActive(BehaviorType.WALL_SLIDING)) wallSlideGravity
            else if (body.isSensing(BodySense.FEET_ON_GROUND)) groundGravity
            else gravity
        gravityValue *= gravityScalar

        when (directionRotation!!) {
            Direction.UP,
            Direction.DOWN -> {
                body.physics.gravity.set(0f, gravityValue * ConstVals.PPM)
                body.physics.defaultFrictionOnSelf =
                    when (directionRotation!!) {
                        Direction.UP,
                        Direction.DOWN -> Vector2(ConstVals.STANDARD_RESISTANCE_X, ConstVals.STANDARD_RESISTANCE_Y)

                        Direction.LEFT,
                        Direction.RIGHT -> Vector2(ConstVals.STANDARD_RESISTANCE_Y, ConstVals.STANDARD_RESISTANCE_X)
                    }

                body.physics.velocityClamp = (
                    if (isBehaviorActive(BehaviorType.RIDING_CART))
                        Vector2(MegamanValues.CART_RIDE_MAX_SPEED, MegamanValues.CLAMP_Y)
                    else if (isBehaviorActive(BehaviorType.JUMPING))
                        Vector2(MegamanValues.CLAMP_X, MegamanValues.CLAMP_Y)
                    /*
                    TODO:
                    else if (isBehaviorActive(BehaviorType.WALL_SLIDING))
                        Vector2(MegamanValues.CLAMP_X, MegamanValues.WALL_SLIDE_CLAMP_Y)
                     */
                    else Vector2(MegamanValues.CLAMP_X, MegamanValues.CLAMP_Y))
                    .scl(ConstVals.PPM.toFloat())
            }

            else -> {
                body.physics.gravity.set(gravityValue * ConstVals.PPM, 0f)
                body.physics.defaultFrictionOnSelf =
                    Vector2(ConstVals.STANDARD_RESISTANCE_Y, ConstVals.STANDARD_RESISTANCE_X)

                body.physics.velocityClamp = (
                    if (isBehaviorActive(BehaviorType.RIDING_CART))
                        Vector2(MegamanValues.CLAMP_Y, MegamanValues.CART_RIDE_MAX_SPEED)
                    else if (isBehaviorActive(BehaviorType.JUMPING))
                        Vector2(MegamanValues.CLAMP_X, MegamanValues.CLAMP_Y)
                    /*
                    TODO:
                    else if (isBehaviorActive(BehaviorType.WALL_SLIDING))
                        Vector2(MegamanValues.WALL_SLIDE_CLAMP_Y, MegamanValues.CLAMP_X)
                     */
                    else Vector2(MegamanValues.CLAMP_Y, MegamanValues.CLAMP_X))
                    .scl(ConstVals.PPM.toFloat())
            }
        }

        rightFixture.offsetFromBodyCenter = Vector2(0.5f, 0f /* 0.25f */).scl(ConstVals.PPM.toFloat())
        leftFixture.offsetFromBodyCenter = Vector2(-0.5f, 0f /* 0.25f */).scl(ConstVals.PPM.toFloat())

        // TODO: should ground sliding result in different sizes for body and fixture?
        /*
        if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) {
            body.height = 0.45f * ConstVals.PPM
            (playerFixture.rawShape as GameRectangle).height = 0.5f * ConstVals.PPM
            headFixture.offsetFromBodyCenter.y = ConstVals.PPM / 4f
            feetFixture.offsetFromBodyCenter.y = -ConstVals.PPM / 4f
            (leftFixture.rawShape as Rectangle).setHeight(0.25f /* 0.1f */ * ConstVals.PPM)
            (rightFixture.rawShape as Rectangle).setHeight(0.25f /* 0.1f */ * ConstVals.PPM)
        } else {

         */

        // TODO: if these values are final, then move them out of the pre-process update
        body.height = 0.95f * ConstVals.PPM
        (playerFixture.rawShape as GameRectangle).height = ConstVals.PPM.toFloat()
        (teleporterListenerFixture.rawShape as GameRectangle).height = ConstVals.PPM.toFloat()
        headFixture.offsetFromBodyCenter.y = ConstVals.PPM / 2f
        feetFixture.offsetFromBodyCenter.y = -ConstVals.PPM / 2f
        (leftFixture.rawShape as Rectangle).setHeight(0.6f /* 0.4f */ * ConstVals.PPM)
        (rightFixture.rawShape as Rectangle).setHeight(0.6f /* 0.4f */ * ConstVals.PPM)
        // }

        (bodyFixture.rawShape as Rectangle).set(body)

        // TODO: see above TODO stub regarding ground sliding and body/fixtures sizes
        /*
        val playerFixtureHeight = (if (isBehaviorActive(BehaviorType.GROUND_SLIDING)) 0.25f else 0.5f) * ConstVals.PPM
        (playerFixture.rawShape as Rectangle).height = playerFixtureHeight
        (teleporterListenerFixture.rawShape as Rectangle).height = playerFixtureHeight
         */
    })

    addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

    return BodyComponentCreator.create(this, body)
}
