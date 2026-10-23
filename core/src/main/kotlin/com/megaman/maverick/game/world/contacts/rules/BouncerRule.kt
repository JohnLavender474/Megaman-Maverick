package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.utils.VelocityAlterator
import com.megaman.maverick.game.world.body.*

object BouncerRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b, delta)

    private fun begin(bouncerFixture: IFixture, bounceableFixture: IFixture, delta: Float) {
        val bounce =
            bouncerFixture.receiver(Receivers.VELOCITY_ALTERATION)!!.invoke(bounceableFixture, delta, ProcessState.BEGIN)
        VelocityAlterator.alterate(bounceableFixture.getBody(), bounce)

        bouncerFixture.receiver(Receivers.RUNNABLE)?.invoke()
        bounceableFixture.receiver(Receivers.RUNNABLE)?.invoke()
    }
}
