package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isAnyBehaviorActive
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.startSmoothFollow
import com.megaman.maverick.game.utils.extensions.stopSmoothFollow
import com.megaman.maverick.game.world.body.*

private const val TAG = "PlayerClimb"

private const val LADDER_MOVE_OFFSET = 0.5f

enum class ClimbEntry { HEAD, FEET }

class ClimbParams(
    val speed: () -> Float,
    val blocked: () -> Boolean,
    val feetGrabAllowed: () -> Boolean,
    val holdStill: () -> Boolean,
    val leavesOnFloor: Boolean,
    val onStart: (entry: ClimbEntry) -> Unit,
    val onEnd: (overTop: Boolean) -> Unit
)

// TODO: widen the player type when an enemy clone calls this (item 121)
fun playerClimbBehavior(player: MegaPlayer, params: ClimbParams): IBehavior = object : AbstractBehaviorImpl() {

    private lateinit var ladder: IBodyEntity
    private var entry = ClimbEntry.HEAD
    private var overTop = false

    private fun sensorBounds(type: FixtureType): GameRectangle {
        val position = player.body.fixtures.get(type).first().getShape().getBoundingRectangle()
            .getPositionPoint(if (player.isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT)
        return GameObjectPools.fetch(GameRectangle::class).setSize(0.25f * ConstVals.PPM).setCenter(position)
    }

    private fun stillOnLadder(): Boolean {
        val body = player.body
        val center = body.getCenter()
        val margin = LADDER_MOVE_OFFSET * ConstVals.PPM
        val ladderBody = ladder.body
        when {
            player.direction.isVertical() -> {
                if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) when {
                    player.direction == Direction.DOWN && center.y + margin < ladderBody.getY() -> return leave(true)
                    center.y - margin > ladderBody.getMaxY() -> return leave(player.direction != Direction.DOWN)
                }
                if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) when {
                    player.direction == Direction.DOWN && center.y - margin > ladderBody.getMaxY() -> return leave(false)
                    center.y + margin < ladderBody.getY() -> return leave(player.direction == Direction.DOWN)
                }
            }

            else -> {
                if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) when {
                    player.direction == Direction.LEFT && center.x + margin < ladderBody.getX() -> return leave(true)
                    center.x - margin > ladderBody.getMaxX() -> return leave(player.direction != Direction.LEFT)
                }
                if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) when {
                    player.direction == Direction.LEFT && center.x + margin > ladderBody.getMaxX() -> return leave(false)
                    center.x - margin < ladderBody.getX() -> return leave(player.direction == Direction.LEFT)
                }
            }
        }

        if (params.leavesOnFloor && player.input.isPressed(MegaControllerButton.DOWN) &&
            body.isSensing(BodySense.FEET_ON_GROUND)
        ) return false
        return !player.input.isJustPressed(MegaControllerButton.A)
    }

    private fun leave(top: Boolean): Boolean {
        overTop = top
        return false
    }

    override fun evaluate(delta: Float): Boolean {
        overTop = false
        if (!player.existenceState.liveAfterUpdate || !player.inPlay || !player.playerCanMove) return false

        val body = player.body
        if (player.damaged || !body.properties.containsKey(ConstKeys.LADDER) || player.isAnyBehaviorActive(
                BehaviorType.JUMPING,
                BehaviorType.SWIMMING,
                BehaviorType.JETPACKING,
                BehaviorType.AIR_DASHING,
                BehaviorType.GROUND_SLIDING
            ) || params.blocked()
        ) return false

        ladder = body.getProperty(ConstKeys.LADDER, IBodyEntity::class)!!

        if (player.isBehaviorActive(BehaviorType.CLIMBING)) return stillOnLadder()

        if (body.isSensing(BodySense.FEET_TOUCHING_LADDER) &&
            ladder.body.getBounds().overlaps(sensorBounds(FixtureType.FEET)) &&
            player.input.isPressed(MegaControllerButton.DOWN) && params.feetGrabAllowed()
        ) {
            entry = ClimbEntry.FEET
            return true
        }

        if (body.isSensing(BodySense.HEAD_TOUCHING_LADDER) &&
            ladder.body.getBounds().overlaps(sensorBounds(FixtureType.HEAD)) &&
            player.input.isPressed(MegaControllerButton.UP)
        ) {
            entry = ClimbEntry.HEAD
            return true
        }

        return false
    }

    override fun init(vararg args: Any) {
        val body = player.body
        val ladderBody = ladder.body
        when (player.direction) {
            Direction.UP, Direction.DOWN -> {
                body.setCenterX(ladderBody.getCenter().x)
                when {
                    body.getMaxY() <= ladderBody.getY() -> body.setY(ladderBody.getY())
                    body.getY() >= ladderBody.getMaxY() -> body.setMaxY(ladderBody.getMaxY())
                }
            }

            Direction.LEFT, Direction.RIGHT -> {
                body.setCenterY(ladderBody.getCenter().y)
                when {
                    body.getMaxX() <= ladderBody.getX() -> body.setX(ladderBody.getX())
                    body.getX() >= ladderBody.getMaxX() -> body.setMaxX(ladderBody.getMaxX())
                }
            }
        }
        body.physics.gravityOn = false
        body.physics.velocity.setZero()
        player.services.cameraController.startSmoothFollow()
        params.onStart(entry)
    }

    override fun act(delta: Float) {
        val body = player.body
        when (player.direction) {
            Direction.UP, Direction.DOWN -> body.setCenterX(ladder.body.getCenter().x)
            Direction.LEFT, Direction.RIGHT -> body.setCenterY(ladder.body.getCenter().y)
        }
        body.physics.gravityOn = false
        if (params.holdStill() || player.services.levelState.roomTransition) {
            GameLogger.debug(TAG, "act(): hold still or room transition")
            body.physics.velocity.setZero()
            return
        }

        val up = player.input.isPressed(MegaControllerButton.UP)
        val down = player.input.isPressed(MegaControllerButton.DOWN)
        val step = when {
            player.direction == Direction.DOWN && down -> -1f
            up -> 1f
            down -> -1f
            else -> 0f
        }
        if (step == 0f) {
            body.physics.velocity.setZero()
            return
        }
        val speed = params.speed() * step
        val velocity = GameObjectPools.fetch(Vector2::class)
        when (player.direction) {
            Direction.UP -> velocity.set(0f, speed)
            Direction.DOWN -> velocity.set(0f, -speed)
            Direction.LEFT -> velocity.set(-speed, 0f)
            Direction.RIGHT -> velocity.set(speed, 0f)
        }.scl(ConstVals.PPM.toFloat())
        body.physics.velocity.set(velocity)
    }

    override fun end() {
        player.services.cameraController.stopSmoothFollow()
        player.body.physics.gravityOn = true
        player.body.physics.velocity.setZero()
        params.onEnd(overTop)
    }
}
