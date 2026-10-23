package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.world.contacts.ContactValues.ICE_FRICTION

object FeetIceRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a)

    private fun begin(feetFixture: IFixture) {
        val body = feetFixture.getBody()
        body.setBodySense(BodySense.FEET_ON_ICE, true)

        val applyIceFrictionX =
            body.getOrDefaultProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_X}", true, Boolean::class)
        if (applyIceFrictionX) body.physics.frictionOnSelf.x = ICE_FRICTION * ConstVals.PPM
    }

    private fun continueContact(feetFixture: IFixture) {
        val body = feetFixture.getBody()

        body.setBodySense(BodySense.FEET_ON_ICE, true)

        if (feetFixture.receiver(Receivers.ICE_FRICTION_FILTER)?.invoke() == false) return

        val applyIceFrictionX =
            body.getOrDefaultProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_X}", true, Boolean::class)
        if (applyIceFrictionX) body.physics.frictionOnSelf.x = ICE_FRICTION * ConstVals.PPM
    }

    private fun end(feetFixture: IFixture) {
        val body = feetFixture.getBody()
        body.setBodySense(BodySense.FEET_ON_ICE, false)
    }
}
