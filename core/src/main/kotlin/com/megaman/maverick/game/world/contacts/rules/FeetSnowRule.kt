package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.world.contacts.ContactValues.SNOW_FRICTION_X
import com.megaman.maverick.game.world.contacts.ContactValues.SNOW_FRICTION_Y

object FeetSnowRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = touch(ProcessState.BEGIN, a, b, delta)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = touch(ProcessState.CONTINUE, a, b, delta)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a, b, delta)

    private fun touch(state: ProcessState, feetFixture: IFixture, snowFixture: IFixture, delta: Float) {
        val body = feetFixture.getBody()
        body.setBodySense(BodySense.FEET_ON_SNOW, true)

        val takeFriction =
            body.getOrDefaultProperty("${ConstKeys.TAKE_FRICTION}_${ConstKeys.SNOW}", true, Boolean::class)
        if (takeFriction)
            body.physics.frictionOnSelf.set(SNOW_FRICTION_X, SNOW_FRICTION_Y).scl(ConstVals.PPM.toFloat())

        snowFixture.receiver(Receivers.HIT_BY_FEET, state)?.invoke(feetFixture, delta)
    }

    private fun end(feetFixture: IFixture, snowFixture: IFixture, delta: Float) {
        val body = feetFixture.getBody()
        body.setBodySense(BodySense.FEET_ON_SNOW, false)

        snowFixture.receiver(Receivers.HIT_BY_FEET, ProcessState.END)?.invoke(feetFixture, delta)
    }
}
