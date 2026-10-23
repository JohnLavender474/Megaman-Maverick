package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.world.body.*

object HeadBlockRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b, delta)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b, delta)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a, b, delta)

    private fun begin(headFixture: IFixture, blockFixture: IFixture, delta: Float) {
        if (blockFixture.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) return

        val body = headFixture.getBody()
        body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)
        body.physics.velocity.y = 0f

        val block = blockFixture.getEntity() as Block

        block.hitByHead(ProcessState.BEGIN, headFixture)
        headFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN)?.invoke(block, delta)
    }

    private fun continueContact(headFixture: IFixture, blockFixture: IFixture, delta: Float) {
        if (blockFixture.getBody().hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY)) return

        val body = headFixture.getBody()
        body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, true)

        val block = blockFixture.getEntity() as Block

        block.hitByHead(ProcessState.CONTINUE, headFixture)
        headFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.CONTINUE)?.invoke(block, delta)
    }

    private fun end(headFixture: IFixture, blockFixture: IFixture, delta: Float) {
        val body = headFixture.getBody()
        body.setBodySense(BodySense.HEAD_TOUCHING_BLOCK, false)

        val block = blockFixture.getEntity() as Block

        block.hitByHead(ProcessState.END, headFixture)
        headFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.END)?.invoke(block, delta)
    }
}
