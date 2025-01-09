package com.megaman.maverick.game.entities.megaman.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
import com.megaman.maverick.game.entities.megaman.constants.MegaAbility
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

const val MEGAMAN_BODY_WIDTH = 1f
const val MEGAMAN_BODY_HEIGHT = 1.35f

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

val Megaman.feetOnGround: Boolean
    get() = (body.getProperty(ConstKeys.FEET_ON_GROUND) as () -> Boolean).invoke()

internal fun Megaman.defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(MEGAMAN_BODY_WIDTH * ConstVals.PPM, MEGAMAN_BODY_HEIGHT * ConstVals.PPM)
    body.putProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_Y}", false)
    body.physics.applyFrictionX = true
    body.physics.applyFrictionY = true

    val debugShapes = Array<() -> IDrawableShape?>()
    // debugShapes.add { body.getBounds() }

    val playerFixture = Fixture(body, FixtureType.PLAYER, GameRectangle(body))
    body.addFixture(playerFixture)

    val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
    body.addFixture(bodyFixture)
    body.putProperty(ConstKeys.BODY, bodyFixture)

    val onBounce = {
        if (!body.isSensing(BodySense.IN_WATER) && has(MegaAbility.AIR_DASH)) aButtonTask = AButtonTask.AIR_DASH
    }

    val feetFixture =
        Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM))
    feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
    feetFixture.setRunnable(onBounce)
    body.addFixture(feetFixture)
    debugShapes.add { feetFixture }
    body.putProperty(ConstKeys.FEET, feetFixture)

    // The feet gravity fixture is a consumer that checks for overlap with blocks. If there is a contact with a block,
    // then Megaman's gravity should be adjusted accordingly. Note the differences in size and offset between this feet
    // fixture and the other feet fixture.
    val feetGravityFixture =
        Fixture(body, FixtureType.CUSTOM, GameRectangle().setSize(0.9f * ConstVals.PPM, 0.1f * ConstVals.PPM))
    val feetGravityFixtureFilter: (IFixture) -> Boolean = { it.getType() == FixtureType.BLOCK }
    feetGravityFixture.putProperty(ConstKeys.FILTER, feetGravityFixtureFilter)
    val feetGravitySet = ObjectSet<IFixture>()
    feetGravityFixture.putProperty(ConstKeys.SET, feetGravitySet)
    val handleContact: (ProcessState, IFixture, IFixture) -> Unit = handleContact@{ pState, f1, _ ->
        when (pState) {
            ProcessState.BEGIN, ProcessState.CONTINUE -> {
                if ((f1.getEntity() as Block).body.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) &&
                    body.physics.velocity.y > 0f
                ) {
                    feetGravitySet.remove(f1)
                    return@handleContact
                }

                feetGravitySet.add(f1)
            }

            else -> feetGravitySet.remove(f1)
        }
    }
    feetGravityFixture.putProperty(ConstKeys.FUNCTION, handleContact)
    val isFeetOnGround: () -> Boolean = { !feetGravitySet.isEmpty }
    body.putProperty(ConstKeys.FEET_ON_GROUND, isFeetOnGround)
    body.onReset.put("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") { feetGravitySet.clear() }
    feetGravityFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
    body.addFixture(feetGravityFixture)

    val headFixture =
        Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.5f * ConstVals.PPM))
    headFixture.offsetFromBodyAttachment.y = (body.getHeight() / 2f) - 0.2f * ConstVals.PPM
    headFixture.setRunnable(onBounce)
    body.addFixture(headFixture)
    debugShapes.add { headFixture }
    body.putProperty(ConstKeys.HEAD, headFixture)

    val leftFixture =
        Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, ConstVals.PPM.toFloat()))
    leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
    leftFixture.offsetFromBodyAttachment.y = 0.1f * ConstVals.PPM
    leftFixture.setRunnable(onBounce)
    leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
    body.addFixture(leftFixture)
    debugShapes.add { leftFixture}
    body.putProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", leftFixture)

    val rightFixture =
        Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, ConstVals.PPM.toFloat()))
    rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
    rightFixture.offsetFromBodyAttachment.y = 0.1f * ConstVals.PPM
    rightFixture.setRunnable(onBounce)
    rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
    body.addFixture(rightFixture)
    debugShapes.add { rightFixture}
    body.putProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", rightFixture)

    val damagableRect = GameRectangle()
    val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, damagableRect)
    damageableFixture.attachedToBody = false
    body.addFixture(damageableFixture)
    body.putProperty(ConstKeys.DAMAGEABLE, damageableFixture)
    damageableFixture.drawingColor = Color.PURPLE
    debugShapes.add { damageableFixture }

    val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle(body))
    body.addFixture(waterListenerFixture)

    val teleporterListenerFixture = Fixture(body, FixtureType.TELEPORTER_LISTENER, GameRectangle(body))
    body.addFixture(teleporterListenerFixture)

    body.preProcess.put(ConstKeys.DEFAULT) {
        if (!ready) {
            body.physics.velocity.setZero()
            body.physics.gravity.setZero()
            return@put
        }

        val wallSlidingOnIce = isBehaviorActive(BehaviorType.WALL_SLIDING) &&
            (body.isSensingAny(BodySense.SIDE_TOUCHING_ICE_LEFT, BodySense.SIDE_TOUCHING_ICE_RIGHT))

        var gravityValue = when {
            body.isSensing(BodySense.IN_WATER) -> if (wallSlidingOnIce) waterIceGravity else waterGravity
            !feetGravitySet.isEmpty -> groundGravity
            wallSlidingOnIce -> iceGravity
            else -> gravity
        }
        gravityValue *= gravityScalar

        when (direction) {
            Direction.UP,
            Direction.DOWN -> {
                body.physics.gravity.set(0f, gravityValue * ConstVals.PPM)
                body.physics.defaultFrictionOnSelf.set(ConstVals.STANDARD_RESISTANCE_X, ConstVals.STANDARD_RESISTANCE_Y)

                val clamp = GameObjectPools.fetch(Vector2::class)
                body.physics.velocityClamp.set(
                    if (isBehaviorActive(BehaviorType.RIDING_CART))
                        clamp.set(MegamanValues.CART_RIDE_MAX_SPEED, MegamanValues.CLAMP_Y)
                    else clamp.set(MegamanValues.CLAMP_X, MegamanValues.CLAMP_Y)
                ).scl(ConstVals.PPM.toFloat())

                damagableRect.let {
                    val size = GameObjectPools.fetch(Vector2::class)
                    when {
                        isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) ->
                            size.set(MEGAMAN_BODY_HEIGHT, MEGAMAN_BODY_WIDTH)

                        else -> size.set(MEGAMAN_BODY_WIDTH, MEGAMAN_BODY_HEIGHT)
                    }
                    it.setSize(size.scl(ConstVals.PPM.toFloat()))

                    val position = DirectionPositionMapper.getInvertedPosition(direction)
                    it.positionOnPoint(body.getBounds().getPositionPoint(position), position)
                }
            }

            else -> {
                body.physics.gravity.set(gravityValue * ConstVals.PPM, 0f)
                body.physics.defaultFrictionOnSelf.set(ConstVals.STANDARD_RESISTANCE_Y, ConstVals.STANDARD_RESISTANCE_X)

                val clamp = GameObjectPools.fetch(Vector2::class)
                body.physics.velocityClamp.set(
                    if (isBehaviorActive(BehaviorType.RIDING_CART))
                        clamp.set(MegamanValues.CLAMP_Y, MegamanValues.CART_RIDE_MAX_SPEED)
                    else clamp.set(MegamanValues.CLAMP_Y, MegamanValues.CLAMP_X)
                ).scl(ConstVals.PPM.toFloat())

                damagableRect.let {
                    val size = GameObjectPools.fetch(Vector2::class)
                    when {
                        isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) ->
                            size.set(MEGAMAN_BODY_WIDTH, MEGAMAN_BODY_HEIGHT)

                        else -> size.set(MEGAMAN_BODY_HEIGHT, MEGAMAN_BODY_WIDTH)
                    }
                    it.setSize(size.scl(ConstVals.PPM.toFloat()))

                    val position = DirectionPositionMapper.getInvertedPosition(direction)
                    it.positionOnPoint(body.getBounds().getPositionPoint(position), position)
                }
            }
        }
    }

    addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

    return BodyComponentCreator.create(this, body)
}
