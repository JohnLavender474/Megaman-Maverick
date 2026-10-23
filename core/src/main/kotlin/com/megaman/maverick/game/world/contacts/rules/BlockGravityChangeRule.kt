package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.world.body.*

object BlockGravityChangeRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = swapFriction(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = swapFriction(a, b)

    private fun swapFriction(blockFixture: IFixture, gravityChangeFixture: IFixture) {
        val direction = gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return

        val canChangeGravity = blockFixture.properties.getOrDefault(
            ConstKeys.GRAVITY_ROTATABLE, true, Boolean::class
        )
        if (!canChangeGravity) return

        if (direction.isHorizontal()) {
            val blockBody = blockFixture.getBody()
            val frictionX = blockBody.physics.frictionToApply.x
            blockBody.physics.frictionToApply.x = blockBody.physics.frictionToApply.y
            blockBody.physics.frictionToApply.y = frictionX
        }
    }
}
