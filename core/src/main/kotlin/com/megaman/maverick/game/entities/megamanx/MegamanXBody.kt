package com.megaman.maverick.game.entities.megamanx

import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.state.FocusComponent
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.entities.state.stunned
import com.megaman.maverick.game.players.bodyOverGround
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

internal fun MegamanX.defineBodyComponent(): BodyComponent {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(MegamanXValues.BODY_WIDTH * ConstVals.PPM, MegamanXValues.BODY_HEIGHT * ConstVals.PPM)
    body.physics.applyFrictionX = false
    body.physics.applyFrictionY = false
    // TODO: X passes every one-way block (a ladder top, and also a one-way platform) while DOWN is held, because X
    //  cannot climb yet; climb comes in a later phase of item 081
    val passThroughOneWay: () -> Boolean = { input.isPressed(MegaControllerButton.DOWN) }
    body.putProperty(BodyKeys.PASS_THROUGH_ONE_WAY, passThroughOneWay)

    val playerFixture = Fixture(body, FixtureType.PLAYER, GameRectangle())
    body.addFixture(playerFixture)

    val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
    body.addFixture(bodyFixture)
    body.putProperty(ConstKeys.BODY, bodyFixture)

    val feetFixture = Fixture(
        body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM)
    )
    body.addFixture(feetFixture)
    body.putProperty(ConstKeys.FEET, feetFixture)

    val feetGravityRect = GameRectangle().setSize(
        (MegamanXValues.BODY_WIDTH - 0.1f) * ConstVals.PPM, 0.1f * ConstVals.PPM
    )
    val feetGravityFixture = Fixture(body, FixtureType.CONSUMER, feetGravityRect)
    feetGravityFixture.setReceiver(Receivers.FILTER) {
        it.getType() == FixtureType.BLOCK && it.receiver(Receivers.FILTER)?.invoke(feetGravityFixture) != false
    }
    val feetGravitySet = ObjectSet<IFixture>()
    feetGravityFixture.setReceiver(Receivers.CONSUMER) consumer@{ processState, fixture ->
        when (processState) {
            ProcessState.BEGIN, ProcessState.CONTINUE -> {
                val block = fixture.getEntity() as Block
                // TODO: the rising test reads velocity.y, so it assumes that the body points up, as Megaman's does
                //  (item 058)
                if (block.body.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) &&
                    (body.physics.velocity.y > 0f || passThroughOneWay())
                ) {
                    feetGravitySet.remove(fixture)
                    return@consumer
                }
                feetGravitySet.add(fixture)
            }

            ProcessState.END -> feetGravitySet.remove(fixture)
        }
    }
    val isBodyOverGround: () -> Boolean = { !feetGravitySet.isEmpty }
    body.putProperty(ConstKeys.BODY_OVER_GROUND, isBodyOverGround)
    body.onReset.put("${ConstKeys.FEET}_${ConstKeys.GRAVITY}") { feetGravitySet.clear() }
    body.addFixture(feetGravityFixture)
    body.putProperty("${ConstKeys.FEET}_${ConstKeys.GRAVITY}", feetGravityFixture)

    val headFixture = Fixture(
        body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM)
    )
    body.addFixture(headFixture)
    body.putProperty(ConstKeys.HEAD, headFixture)

    val leftFixture = Fixture(
        body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, 0.75f * ConstVals.PPM)
    )
    leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
    body.addFixture(leftFixture)
    body.putProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", leftFixture)

    val rightFixture = Fixture(
        body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, 0.75f * ConstVals.PPM)
    )
    rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
    body.addFixture(rightFixture)
    body.putProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", rightFixture)

    val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
    body.addFixture(damageableFixture)
    body.putProperty(ConstKeys.DAMAGEABLE, damageableFixture)

    val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle())
    body.addFixture(waterListenerFixture)

    val teleporterListenerFixture = Fixture(body, FixtureType.TELEPORTER_LISTENER, GameRectangle())
    body.addFixture(teleporterListenerFixture)

    val fixturesToSizeToBody = gdxArrayOf(
        bodyFixture, playerFixture, damageableFixture, waterListenerFixture, teleporterListenerFixture
    )

    body.preProcess.put(ConstKeys.DEFAULT) {
        fixturesToSizeToBody.forEach { fixture -> (fixture.rawShape as GameRectangle).set(body) }

        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        feetGravityFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f

        if (!inPlay) {
            body.physics.velocity.setZero()
            body.physics.gravity.setZero()
            return@put
        }

        val vertical = direction.isVertical()
        body.physics.applyFrictionX = damaged && vertical
        body.physics.applyFrictionY = damaged && !vertical
        if (vertical) body.physics.defaultFrictionOnSelf.set(MegamanXValues.KNOCKBACK_DRAG, 0f)
        else body.physics.defaultFrictionOnSelf.set(0f, MegamanXValues.KNOCKBACK_DRAG)

        if (!running && !damaged && !stunned && !holdsSideSpeed) when (direction) {
            Direction.UP, Direction.DOWN -> body.physics.velocity.x = 0f
            Direction.LEFT, Direction.RIGHT -> body.physics.velocity.y = 0f
        }

        val gravity = upSign() * ConstVals.PPM * when {
            bodyOverGround -> MegamanXValues.GROUND_GRAVITY
            else -> MegamanXValues.GRAVITY
        }

        when (direction) {
            Direction.UP, Direction.DOWN -> {
                body.physics.gravity.set(0f, gravity)
                body.physics.velocityClamp.set(MegamanXValues.CLAMP_X, MegamanXValues.FALL_MAX)
                    .scl(ConstVals.PPM.toFloat())
            }

            Direction.LEFT, Direction.RIGHT -> {
                body.physics.gravity.set(gravity, 0f)
                body.physics.velocityClamp.set(MegamanXValues.FALL_MAX, MegamanXValues.CLAMP_X)
                    .scl(ConstVals.PPM.toFloat())
            }
        }
    }

    return BodyComponentCreator.create(this, body)
}

internal fun MegamanX.defineFocusComponent() = FocusComponent(
    bounds = { body.getBounds() },
    position = {
        val position = DirectionPositionMapper.getInvertedPosition(direction)
        val focus = body.getBounds().getPositionPoint(position)
        val offset = MegamanXValues.BODY_HEIGHT * ConstVals.PPM / 2f
        when (direction) {
            Direction.UP -> focus.y += offset
            Direction.DOWN -> focus.y -= offset
            Direction.LEFT -> focus.x -= offset
            Direction.RIGHT -> focus.x += offset
        }
        focus
    }
)
