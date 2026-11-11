package com.megaman.maverick.game.players

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.set
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.entities.resetBehavior
import com.maverick.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.state.frozen
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.preserveMomentum
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

val PLAYER_BEHAVIORS_TO_END_ON_BOUNCE = gdxArrayOf(
    BehaviorType.WALL_SLIDING,
    BehaviorType.AIR_DASHING,
    BehaviorType.GROUND_SLIDING,
    BehaviorType.SWIMMING,
    BehaviorType.JETPACKING,
    BehaviorType.CLIMBING,
    BehaviorType.CROUCHING
)

class PlayerBodyParams(
    val width: Float,
    val height: Float,
    val crouchHeight: Float,
    val gravity: PlayerGravityComponent,
    val waterGravityScalar: Float,
    val clampX: Float,
    val clampY: Float,
    val onBounce: () -> Unit,
    val onFeetBounce: () -> Unit,
    val onFeetBlockBegin: (makesSound: Boolean) -> Unit,
    val onFeetBlockContinue: () -> Unit,
    val onFeetBlockEnd: () -> Unit,
    val onHeadHitBlock: () -> Unit,
    val sideSticksTo: (blockFixture: IFixture) -> Boolean,
    val onWater: (state: ProcessState) -> Unit,
    val onPreStep: () -> Unit
)

// TODO: widen the player type when an enemy clone calls this (item 121)
fun <T> playerBody(player: T, params: PlayerBodyParams): Body where T : MegaPlayer, T : IScalableGravityEntity {
    val body = Body(BodyType.DYNAMIC)
    body.putProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_Y}", false)
    body.putProperty(BodyKeys.PASS_THROUGH_ONE_WAY) { player.isBehaviorActive(BehaviorType.CLIMBING) }
    body.putProperty(BodyKeys.FALL_THROUGH_PRESS_UP) {
        player.isBehaviorActive(BehaviorType.CLIMBING) && player.input.isJustPressed(MegaControllerButton.UP)
    }
    body.physics.applyFrictionX = true
    body.physics.applyFrictionY = true
    body.drawingColor = Color.GRAY

    val playerFixture = Fixture(body, FixtureType.PLAYER, GameRectangle())
    body.addFixture(playerFixture)

    val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
    body.addFixture(bodyFixture)
    body.putProperty(ConstKeys.BODY, bodyFixture)

    val onBounce: () -> Unit = {
        PLAYER_BEHAVIORS_TO_END_ON_BOUNCE.forEach { behaviorType ->
            if (player.isBehaviorActive(behaviorType)) player.resetBehavior(behaviorType)
        }
        params.onBounce()
    }

    val feetFixture =
        Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM))
    feetFixture.setReceiver(Receivers.RUNNABLE) {
        onBounce.invoke()
        params.onFeetBounce()
    }
    feetFixture.setReceiver(Receivers.STICK_TO_BLOCK) { _, _ -> !body.isSensing(BodySense.IN_WATER) }
    feetFixture.setReceiver(Receivers.ICE_FRICTION_FILTER) {
        !(body.isSensing(BodySense.FEET_ON_GROUND) &&
            (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) && player.isFacing(Facing.LEFT) ||
                (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) && player.isFacing(Facing.RIGHT))))
    }
    feetFixture.setReceiver(Receivers.FEET_BLOCK_CONTACT) { state, block ->
        when (state) {
            ProcessState.BEGIN -> {
                val blockMakesSound = block.getOrDefaultProperty(
                    "${ConstKeys.FEET}_${ConstKeys.SOUND}",
                    !block.body.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) || when (player.direction) {
                        Direction.UP -> body.physics.velocity.y <= 0f
                        Direction.DOWN -> body.physics.velocity.y >= 0f
                        Direction.LEFT -> body.physics.velocity.x >= 0f
                        Direction.RIGHT -> body.physics.velocity.x <= 0f
                    },
                    Boolean::class
                )
                params.onFeetBlockBegin(blockMakesSound)
            }

            ProcessState.CONTINUE -> params.onFeetBlockContinue()

            ProcessState.END -> params.onFeetBlockEnd()
        }
    }
    body.addFixture(feetFixture)
    feetFixture.drawingColor = Color.GREEN
    body.putProperty(ConstKeys.FEET, feetFixture)

    // The feet gravity fixture is a consumer that checks for overlap with blocks. If there is a contact with a block,
    // then the player's gravity should be adjusted accordingly. Note the differences in size and offset between this
    // feet fixture and the other feet fixture.
    val feetGravityRect = GameRectangle().setHeight(0.1f * ConstVals.PPM)
    val feetGravityFixture = Fixture(body, FixtureType.CONSUMER, feetGravityRect)
    feetGravityFixture.setReceiver(Receivers.FILTER) {
        it.getType() == FixtureType.BLOCK && it.receiver(Receivers.FILTER)?.invoke(feetGravityFixture) != false
    }
    val feetGravitySet = ObjectSet<IFixture>()
    feetGravityFixture.putProperty(ConstKeys.SET, feetGravitySet)
    feetGravityFixture.setReceiver(Receivers.CONSUMER) consumer@{ processState, fixture ->
        when (processState) {
            ProcessState.BEGIN, ProcessState.CONTINUE -> {
                val block = fixture.getEntity() as Block
                if (block.body.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) && body.physics.velocity.y > 0f) {
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

    val headFixture =
        Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.25f * ConstVals.PPM))
    headFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN) { _, _ -> params.onHeadHitBlock() }
    headFixture.setReceiver(Receivers.RUNNABLE, onBounce)
    body.addFixture(headFixture)
    headFixture.drawingColor = Color.ORANGE
    body.putProperty(ConstKeys.HEAD, headFixture)

    fun sideFixture(side: String): Fixture {
        val fixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.2f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        fixture.offsetFromBodyAttachment.y = 0.1f * ConstVals.PPM
        fixture.setReceiver(Receivers.RUNNABLE, onBounce)
        fixture.putProperty(ConstKeys.SIDE, side)
        fixture.setReceiver(Receivers.SIDE_ICE_FRICTION) { player.isBehaviorActive(BehaviorType.WALL_SLIDING) }
        fixture.setReceiver(Receivers.STICK_TO_BLOCK) { _, blockFixture -> params.sideSticksTo(blockFixture) }
        body.addFixture(fixture)
        body.putProperty("${side}_${ConstKeys.SIDE}", fixture)
        return fixture
    }

    val leftFixture = sideFixture(ConstKeys.LEFT)
    val rightFixture = sideFixture(ConstKeys.RIGHT)

    val damagableRect = GameRectangle()
    val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, damagableRect)
    damageableFixture.attachedToBody = false
    body.addFixture(damageableFixture)
    body.putProperty(ConstKeys.DAMAGEABLE, damageableFixture)
    damageableFixture.drawingColor = Color.PURPLE

    val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle())
    waterListenerFixture.setReceiver(Receivers.WATER_CONTACT) { state, wasInWater ->
        when (state) {
            ProcessState.BEGIN -> {
                params.onWater(state)

                player.gravityScalar = params.waterGravityScalar

                if (!wasInWater) {
                    if (player.direction.isVertical()) body.physics.velocity.y = 0f
                    else body.physics.velocity.x = 0f
                }
            }

            ProcessState.CONTINUE -> params.onWater(state)

            ProcessState.END -> {
                params.onWater(state)
                player.gravityScalar = 1f
            }
        }
    }
    body.addFixture(waterListenerFixture)

    val teleporterListenerFixture = Fixture(body, FixtureType.TELEPORTER_LISTENER, GameRectangle())
    body.addFixture(teleporterListenerFixture)

    val fixturesToSizeToBody = gdxArrayOf(
        bodyFixture, playerFixture, waterListenerFixture, teleporterListenerFixture
    )

    body.preProcess.put(ConstKeys.DEFAULT) {
        if (abs(body.physics.velocity.x) < 0.025f * ConstVals.PPM) body.physics.velocity.x = 0f
        if (abs(body.physics.velocity.y) < 0.025f * ConstVals.PPM) body.physics.velocity.y = 0f

        params.onPreStep()

        val height = when {
            player.isAnyBehaviorActive(BehaviorType.GROUND_SLIDING, BehaviorType.CROUCHING) -> params.crouchHeight
            else -> params.height
        }
        body.setSize(params.width * ConstVals.PPM, height * ConstVals.PPM)

        fixturesToSizeToBody.forEach { fixture ->
            val bounds = fixture.rawShape as GameRectangle
            bounds.set(body)
        }

        val feetGravityWidth = when {
            player.isBehaviorActive(BehaviorType.WALL_SLIDING) -> params.width - 0.35f
            else -> params.width - 0.1f
        }
        feetGravityRect.setWidth(feetGravityWidth * ConstVals.PPM)
        feetGravityFixture.offsetFromBodyAttachment.x = when {
            player.isAnyBehaviorActive(BehaviorType.CLIMBING, BehaviorType.WALL_SLIDING) || body.isSensingAll(
                BodySense.SIDE_TOUCHING_BLOCK_LEFT,
                BodySense.SIDE_TOUCHING_BLOCK_RIGHT
            ) -> 0f
            body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT) -> 0.1f * ConstVals.PPM
            body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT) -> -0.1f * ConstVals.PPM
            else -> 0f
        }
        feetGravityFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f

        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f

        if (!player.inPlay) {
            body.physics.velocity.setZero()
            body.physics.gravity.setZero()
            return@put
        }

        val wallSlidingOnIce = player.isBehaviorActive(BehaviorType.WALL_SLIDING) &&
            (body.isSensingAny(BodySense.SIDE_TOUCHING_ICE_LEFT, BodySense.SIDE_TOUCHING_ICE_RIGHT))

        if (player.isBehaviorActive(BehaviorType.GROUND_SLIDING)) when (player.direction) {
            Direction.UP, Direction.DOWN -> body.physics.velocity.y = 0f
            else -> body.physics.velocity.x = 0f
        }

        val gravity = params.gravity
        var gravityValue = when {
            player.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> 0f
            body.isSensing(BodySense.IN_WATER) -> when {
                wallSlidingOnIce || player.frozen -> gravity.waterIce
                else -> gravity.water
            }
            player.bodyOverGround -> gravity.ground
            wallSlidingOnIce || player.frozen -> gravity.ice
            player.isBehaviorActive(BehaviorType.JUMPING) -> gravity.jump
            else -> gravity.fall
        }
        gravityValue *= player.gravityScalar

        when (player.direction) {
            Direction.UP,
            Direction.DOWN -> {
                body.physics.gravity.set(0f, gravityValue * ConstVals.PPM)

                body.physics.defaultFrictionOnSelf.set(
                    ConstVals.STANDARD_RESISTANCE_X,
                    ConstVals.STANDARD_RESISTANCE_Y
                )

                body.physics.velocityClamp.set(params.clampX, params.clampY).scl(ConstVals.PPM.toFloat())

                damagableRect.let {
                    val size = GameObjectPools.fetch(Vector2::class)
                    when {
                        player.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> size.set(params.height, params.width)
                        player.isBehaviorActive(BehaviorType.CROUCHING) -> size.set(params.width)
                        else -> size.set(params.width, params.height)
                    }
                    it.setSize(size.scl(ConstVals.PPM.toFloat()))

                    val position = DirectionPositionMapper.getInvertedPosition(player.direction)
                    it.positionOnPoint(body.getBounds().getPositionPoint(position), position)
                }
            }
            else -> {
                body.physics.gravity.set(gravityValue * ConstVals.PPM, 0f)

                body.physics.defaultFrictionOnSelf.set(
                    ConstVals.STANDARD_RESISTANCE_Y,
                    ConstVals.STANDARD_RESISTANCE_X
                )

                body.physics.velocityClamp.set(params.clampY, params.clampX).scl(ConstVals.PPM.toFloat())

                damagableRect.let {
                    val size = GameObjectPools.fetch(Vector2::class)
                    when {
                        player.isBehaviorActive(BehaviorType.GROUND_SLIDING) -> size.set(params.width, params.height)
                        player.isBehaviorActive(BehaviorType.CROUCHING) -> size.set(params.width)
                        else -> size.set(params.height, params.width)
                    }
                    it.setSize(size.scl(ConstVals.PPM.toFloat()))

                    val position = DirectionPositionMapper.getInvertedPosition(player.direction)
                    it.positionOnPoint(body.getBounds().getPositionPoint(position), position)
                }
            }
        }

        if (body.isSensing(BodySense.FEET_ON_ICE)) player.preserveMomentum()
    }

    return body
}
