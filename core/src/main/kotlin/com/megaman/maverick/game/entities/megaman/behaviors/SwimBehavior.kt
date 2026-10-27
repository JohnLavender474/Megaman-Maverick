package com.megaman.maverick.game.entities.megaman.behaviors

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.behaviors.AbstractBehaviorImpl
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.*
import com.megaman.maverick.game.entities.megaman.input
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

const val MEGAMAN_SWIM_BEHAVIOR_TAG = "Megaman: BehaviorsComponent: SwimBehavior"

internal fun Megaman.swimBehavior(): IBehavior {
    return object : AbstractBehaviorImpl() {

        private val timer = Timer(MegamanValues.SWIM_TIMER)

        override fun evaluate(delta: Float): Boolean {
            if (!existenceState.liveAfterUpdate || !inPlay || !canMove) return false

            if (damaged || !body.isSensing(BodySense.IN_WATER) || body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) ||
                (currentWeapon == MegamanWeapon.NEEDLE_SPIN && shooting)
            ) return false

            return when {
                isBehaviorActive(BehaviorType.SWIMMING) -> return !timer.isFinished()
                else -> input.isJustPressed(MegaControllerButton.A) && aButtonTask == AButtonTask.SWIM
            }
        }

        override fun init(vararg params: Any) {
            GameLogger.debug(MEGAMAN_SWIM_BEHAVIOR_TAG, "Init method called")

            timer.reset()

            val impulse = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> impulse.set(0f, swimVel)
                Direction.DOWN -> impulse.set(0f, -swimVel)
                Direction.LEFT -> impulse.set(swimVel, 0f)
                Direction.RIGHT -> impulse.set(-swimVel, 0f)
            }.scl(ConstVals.PPM.toFloat())

            if (game.state.containsEnhancement(MegaEnhancement.JUMP_BOOST)) impulse.scl(MegaEnhancement.JUMP_BOOST_SCALAR)

            body.physics.velocity.add(impulse)
        }

        override fun act(delta: Float) = timer.update(delta)

        override fun end() = timer.reset()
    }
}
