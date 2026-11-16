package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.entities.isBehaviorActive
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.isSensing

private const val TAG = "PlayerSwim"

class SwimParams(
    val time: Float,
    val speed: () -> Float,
    val blocked: () -> Boolean,
    val canStart: () -> Boolean
)

// TODO: widen the player type when an enemy clone calls this (item 121)
fun playerSwimBehavior(player: MegaPlayer, params: SwimParams): IBehavior = object : AbstractBehaviorImpl() {

    private val timer = Timer(params.time)

    override fun evaluate(delta: Float): Boolean {
        if (!player.existenceState.liveAfterUpdate || !player.inPlay || !player.playerCanMove) return false

        val body = player.body
        if (player.damaged || !body.isSensing(BodySense.IN_WATER) || body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) ||
            params.blocked()
        ) return false

        return when {
            player.isBehaviorActive(BehaviorType.SWIMMING) -> !timer.isFinished()
            else -> player.input.isJustPressed(MegaControllerButton.A) && params.canStart()
        }
    }

    override fun init(vararg args: Any) {
        GameLogger.debug(TAG, "init()")
        timer.reset()

        // TODO: DOWN and RIGHT negate the signed speed again, so the stroke pushes the wrong way
        //  (bug candidate, 060m TODO.md)
        val speed = params.speed()
        val impulse = GameObjectPools.fetch(Vector2::class)
        when (player.direction) {
            Direction.UP -> impulse.set(0f, speed)
            Direction.DOWN -> impulse.set(0f, -speed)
            Direction.LEFT -> impulse.set(speed, 0f)
            Direction.RIGHT -> impulse.set(-speed, 0f)
        }.scl(ConstVals.PPM.toFloat())
        player.body.physics.velocity.add(impulse)
    }

    override fun act(delta: Float) = timer.update(delta)

    override fun end() = timer.reset()
}
