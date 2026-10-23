package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.*

object ConsumerRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = consume(ProcessState.BEGIN, a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = consume(ProcessState.CONTINUE, a, b)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = consume(ProcessState.END, a, b)

    private fun consume(state: ProcessState, consumerFixture: IFixture, consumableFixture: IFixture) {
        consumerFixture.receiver(Receivers.CONSUMER)?.invoke(state, consumableFixture)
    }
}

object BodyBodyRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    private fun begin(bodyFixture1: IFixture, bodyFixture2: IFixture) {
        bodyFixture1.receiver(Receivers.HIT_BY_BODY)?.invoke(
            bodyFixture2.getEntity() as IBodyEntity,
            ProcessState.BEGIN
        )

        bodyFixture2.receiver(Receivers.HIT_BY_BODY)?.invoke(
            bodyFixture1.getEntity() as IBodyEntity,
            ProcessState.BEGIN
        )
    }
}

object HitByFeetRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.BEGIN, a, b, delta)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.CONTINUE, a, b, delta)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.END, a, b, delta)

    private fun hit(state: ProcessState, targetFixture: IFixture, feetFixture: IFixture, delta: Float) {
        targetFixture.receiver(Receivers.HIT_BY_FEET, state)?.invoke(feetFixture, delta)
    }
}

object HitByExplosionRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = hit(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = hit(a, b)

    private fun hit(targetFixture: IFixture, explosionFixture: IFixture) {
        targetFixture.receiver(Receivers.HIT_BY_EXPLOSION)?.invoke(explosionFixture.getEntity() as IBodyEntity)
    }
}

object BodySideRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.BEGIN, a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.CONTINUE, a, b)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.END, a, b)

    private fun hit(state: ProcessState, bodyFixture: IFixture, sideFixture: IFixture) {
        if (bodyFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

        val bodyEntity = bodyFixture.getEntity() as IBodyEntity
        sideFixture.receiver(Receivers.HIT_BY_BODY)?.invoke(bodyEntity, state)
    }
}

object SideSideRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.BEGIN, a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.CONTINUE, a, b)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = hit(ProcessState.END, a, b)

    private fun hit(state: ProcessState, side1Fixture: IFixture, side2Fixture: IFixture) {
        side1Fixture.receiver(Receivers.HIT_BY_SIDE)?.invoke(side2Fixture, state)
        side2Fixture.receiver(Receivers.HIT_BY_SIDE)?.invoke(side1Fixture, state)
    }
}

object ShieldSideRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    private fun begin(shieldFixture: IFixture, sideFixture: IFixture) {
        sideFixture.receiver(Receivers.HIT_BY_SHIELD)?.invoke(ProcessState.BEGIN, shieldFixture)
    }
}

object BodyPlayerRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    private fun begin(bodyFixture: IFixture, playerFixture: IFixture) {
        val playerEntity = playerFixture.getEntity()
        bodyFixture.receiver(Receivers.HIT_BY_PLAYER)?.invoke(playerEntity.asPlayer!!)
    }
}
