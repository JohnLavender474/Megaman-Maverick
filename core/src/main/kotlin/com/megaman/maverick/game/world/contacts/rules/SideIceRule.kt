package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.world.contacts.ContactValues.ICE_FRICTION

object SideIceRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = touch(a)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = touch(a)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a)

    private fun touch(sideFixture: IFixture) {
        val body = sideFixture.getBody()

        val applyIceFrictionY =
            body.getOrDefaultProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_Y}", true, Boolean::class)
        if (applyIceFrictionY || sideFixture.receiver(Receivers.SIDE_ICE_FRICTION)?.invoke() == true)
            body.physics.frictionOnSelf.y = ICE_FRICTION * ConstVals.PPM

        val sideType = sideFixture.getProperty(ConstKeys.SIDE)
        if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, true)
        else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, true)
    }

    private fun end(sideFixture: IFixture) {
        val body = sideFixture.getBody()

        val sideType = sideFixture.getProperty(ConstKeys.SIDE)
        if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_ICE_LEFT, false)
        else body.setBodySense(BodySense.SIDE_TOUCHING_ICE_RIGHT, false)
    }
}
