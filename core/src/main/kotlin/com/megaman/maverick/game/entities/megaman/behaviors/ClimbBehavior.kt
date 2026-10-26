package com.megaman.maverick.game.entities.megaman.behaviors

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.input
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

const val MEGAMAN_CLIMB_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: ClimbBehavior"
const val MEGAMAN_LADDER_MOVE_OFFSET = 0.5f

internal fun Megaman.climbBehavior(): IBehavior {
    return object : AbstractBehaviorImpl() {

        private lateinit var ladder: IBodyEntity

        override fun evaluate(delta: Float): Boolean {
            if (dead || !inPlay || !canMove) return false

            if (damaged || !body.properties.containsKey(ConstKeys.LADDER) || isAnyBehaviorActive(
                    BehaviorType.JUMPING,
                    BehaviorType.SWIMMING,
                    BehaviorType.JETPACKING,
                    BehaviorType.AIR_DASHING,
                    BehaviorType.GROUND_SLIDING
                ) || (currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting)
            ) return false

            ladder = body.getProperty(ConstKeys.LADDER, IBodyEntity::class)!!

            val headPos = body.fixtures.get(FixtureType.HEAD)
                .first().getShape().getBoundingRectangle()
                .getPositionPoint(if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT)
            val headBounds = GameObjectPools.fetch(GameRectangle::class)
                .setSize(0.25f * ConstVals.PPM)
                .setCenter(headPos)

            val feetPos = body.fixtures.get(FixtureType.FEET)
                .first().getShape().getBoundingRectangle()
                .getPositionPoint(if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT)
            val feetBounds = GameObjectPools.fetch(GameRectangle::class)
                .setSize(0.25f * ConstVals.PPM)
                .setCenter(feetPos)

            val bodyCenter = body.getCenter()

            if (isBehaviorActive(BehaviorType.CLIMBING)) {
                when {
                    direction.isVertical() -> {
                        if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) when {
                            direction == Direction.DOWN &&
                                bodyCenter.y + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getY() ->
                                return false

                            bodyCenter.y - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxY() ->
                                return false
                        }

                        if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) when {
                            direction == Direction.DOWN &&
                                bodyCenter.y - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxY() ->
                                return false

                            bodyCenter.y + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getY() ->
                                return false
                        }
                    }

                    else -> {
                        if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) when {
                            direction == Direction.LEFT &&
                                bodyCenter.x + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getX() ->
                                return false

                            bodyCenter.x - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxX() ->
                                return false
                        }

                        if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) when {
                            direction == Direction.LEFT &&
                                bodyCenter.x + MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM > ladder.body.getMaxX() ->
                                return false

                            bodyCenter.x - MEGAMAN_LADDER_MOVE_OFFSET * ConstVals.PPM < ladder.body.getX() ->
                                return false
                        }
                    }
                }

                return !input.isJustPressed(MegaControllerButton.A)
            }

            if (body.isSensing(BodySense.FEET_TOUCHING_LADDER) &&
                ladder.body.getBounds().overlaps(feetBounds) &&
                input.isPressed(MegaControllerButton.DOWN)
            ) return true

            if (body.isSensing(BodySense.HEAD_TOUCHING_LADDER) &&
                ladder.body.getBounds().overlaps(headBounds) &&
                input.isPressed(MegaControllerButton.UP)
            ) return true

            return false
        }

        override fun init(vararg params: Any) {
            when (direction) {
                Direction.UP, Direction.DOWN -> {
                    body.setCenterX(ladder.body.getCenter().x)
                    when {
                        body.getMaxY() <= ladder.body.getY() -> body.setY(ladder.body.getY())
                        body.getY() >= ladder.body.getMaxY() -> body.setMaxY(ladder.body.getMaxY())
                    }
                }
                Direction.LEFT, Direction.RIGHT -> {
                    body.setCenterY(ladder.body.getCenter().y)
                    when {
                        body.getMaxX() <= ladder.body.getX() -> body.setX(ladder.body.getX())
                        body.getX() >= ladder.body.getMaxX() -> body.setMaxX(ladder.body.getMaxX())
                    }
                }
            }
            body.physics.gravityOn = false
            body.physics.velocity.setZero()

            game.setFocusSnappedAway(true)

            aButtonTask = when {
                body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
                else -> AButtonTask.AIR_DASH
            }

            canMakeLandSound = false
        }

        override fun act(delta: Float) {
            when (direction) {
                Direction.UP, Direction.DOWN -> body.setCenterX(ladder.body.getCenter().x)
                Direction.LEFT, Direction.RIGHT -> body.setCenterY(ladder.body.getCenter().y)
            }

            body.physics.gravityOn = false

            if (shooting || game.isProperty(ConstKeys.ROOM_TRANSITION, true)) {
                GameLogger.debug(MEGAMAN_CLIMB_BEHAVIOR_TAG, "shooting or room transition")
                body.physics.velocity.setZero()
                return
            }

            val velocity = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> when {
                    input.isPressed(MegaControllerButton.UP) -> velocity.set(
                        0f,
                        MegamanValues.CLIMB_VEL
                    )
                    input.isPressed(MegaControllerButton.DOWN) -> velocity.set(
                        0f,
                        MegamanValues.CLIMB_VEL * -1f
                    )
                    else -> velocity.setZero()
                }
                Direction.DOWN -> when {
                    input.isPressed(MegaControllerButton.DOWN) -> velocity.set(
                        0f,
                        MegamanValues.CLIMB_VEL
                    )
                    input.isPressed(MegaControllerButton.UP) -> velocity.set(
                        0f,
                        MegamanValues.CLIMB_VEL * -1f
                    )
                    else -> velocity.setZero()
                }
                Direction.LEFT -> when {
                    input.isPressed(MegaControllerButton.UP) ->
                        velocity.set(MegamanValues.CLIMB_VEL * -1f, 0f)
                    input.isPressed(MegaControllerButton.DOWN) ->
                        velocity.set(MegamanValues.CLIMB_VEL, 0f)
                    else -> velocity.setZero()

                }
                Direction.RIGHT -> when {
                    input.isPressed(MegaControllerButton.UP) -> velocity.set(
                        MegamanValues.CLIMB_VEL,
                        0f
                    )
                    input.isPressed(MegaControllerButton.DOWN) -> velocity.set(
                        MegamanValues.CLIMB_VEL * -1f,
                        0f
                    )
                    else -> velocity.setZero()
                }
            }.scl(ConstVals.PPM * movementScalar)

            body.physics.velocity.set(velocity)
        }

        override fun end() {
            game.setFocusSnappedAway(false)

            body.physics.gravityOn = true
            body.physics.velocity.setZero()

            aButtonTask = when {
                body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
                else -> AButtonTask.AIR_DASH
            }
        }
    }
}
