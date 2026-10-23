package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.utils.VelocityAlterator
import com.megaman.maverick.game.world.body.*

object BodyForceRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = push(ProcessState.BEGIN, true, a, b, delta)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = push(ProcessState.CONTINUE, true, a, b, delta)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = push(ProcessState.END, false, a, b, delta)

    private fun push(
        state: ProcessState,
        applied: Boolean,
        bodyFixture: IFixture,
        forceFixture: IFixture,
        delta: Float
    ) {
        val receiveForce =
            bodyFixture.getOrDefaultProperty("${ConstKeys.RECEIVE}_${ConstKeys.FORCE}", true, Boolean::class)
        if (!receiveForce) return

        val forceAlteration =
            forceFixture.receiver(Receivers.VELOCITY_ALTERATION)!!.invoke(bodyFixture, delta, state)
        val forceListener = bodyFixture.receiver(Receivers.FORCE_ALTERATION, state)
        if (forceListener != null) forceListener.invoke(forceAlteration)
        else VelocityAlterator.alterate(bodyFixture.getBody(), forceAlteration)

        bodyFixture.getBody().setBodySense(BodySense.FORCE_APPLIED, applied)
        forceFixture.receiver(Receivers.RUNNABLE)?.invoke()
    }
}
