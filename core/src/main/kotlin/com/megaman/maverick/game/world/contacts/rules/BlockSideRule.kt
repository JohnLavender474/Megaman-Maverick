package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.world.body.*

object BlockSideRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b, delta)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(b)

    private fun begin(blockFixture: IFixture, sideFixture: IFixture, delta: Float) {
        if (blockFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

        val body = sideFixture.getBody()

        val sideType = sideFixture.getProperty(ConstKeys.SIDE)
        if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
        else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)

        val block = blockFixture.getEntity() as Block
        block.hitBySide(sideFixture)

        sideFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN)?.invoke(block, delta)
    }

    private fun continueContact(blockFixture: IFixture, sideFixture: IFixture) {
        if (blockFixture.hasFixtureLabel(FixtureLabel.NO_SIDE_TOUCHIE)) return

        val body = sideFixture.getBody()

        val stickToBlock =
            sideFixture.receiver(Receivers.STICK_TO_BLOCK)?.invoke(ProcessState.CONTINUE, blockFixture) ?: true
        if (stickToBlock) {
            val posDelta = blockFixture.getBody().getPositionDelta()
            body.translate(posDelta)
        }

        val sideType = sideFixture.getProperty(ConstKeys.SIDE)
        when (sideType) {
            ConstKeys.LEFT -> body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, true)
            else -> body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, true)
        }
    }

    private fun end(sideFixture: IFixture) {
        val body = sideFixture.getBody()

        val sideType = sideFixture.getProperty(ConstKeys.SIDE)
        if (sideType == ConstKeys.LEFT) body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_LEFT, false)
        else body.setBodySense(BodySense.SIDE_TOUCHING_BLOCK_RIGHT, false)
    }
}
