package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.world.body.*

object BlockBodyRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b, delta)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b, delta)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a, b, delta)

    private fun begin(blockFixture: IFixture, bodyFixture: IFixture, delta: Float) {
        if (blockFixture.hasFixtureLabel(FixtureLabel.NO_BODY_TOUCHIE)) return

        val block = blockFixture.getEntity() as Block
        block.hitByBody(bodyFixture)

        bodyFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN)?.invoke(block, delta)

        val body = bodyFixture.getBody()
        body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, true)
    }

    private fun continueContact(blockFixture: IFixture, bodyFixture: IFixture, delta: Float) {
        if (blockFixture.hasFixtureLabel(FixtureLabel.NO_BODY_TOUCHIE)) return

        val block = blockFixture.getEntity() as Block
        bodyFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.CONTINUE)?.invoke(block, delta)

        val body = bodyFixture.getBody()
        body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, true)
    }

    private fun end(blockFixture: IFixture, bodyFixture: IFixture, delta: Float) {
        val block = blockFixture.getEntity() as Block
        bodyFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.END)?.invoke(block, delta)

        val body = bodyFixture.getBody()
        body.setBodySense(BodySense.BODY_TOUCHING_BLOCK, false)
    }
}
