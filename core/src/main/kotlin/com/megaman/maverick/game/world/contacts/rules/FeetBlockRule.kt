package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.world.body.*

object FeetBlockRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b, delta)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b, delta)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a, b, delta)

    private fun begin(feetFixture: IFixture, blockFixture: IFixture, delta: Float) {
        if (blockFixture.hasFixtureLabel(FixtureLabel.NO_FEET_TOUCHIE)) return
        if (feetFixture.receiver(Receivers.FILTER)?.invoke(blockFixture) == false) return

        val block = blockFixture.getEntity() as Block
        val body = feetFixture.getBody()

        if (!blockFixture.getBody().physics.collisionOn) {
            body.removeFeetBlock(block)
            if (!body.hasAnyFeetBlock()) body.setBodySense(BodySense.FEET_ON_GROUND, false)
            return
        }

        body.addFeetBlock(block)

        val stickToBlock =
            feetFixture.receiver(Receivers.STICK_TO_BLOCK)?.invoke(ProcessState.BEGIN, blockFixture) ?: true
        if (stickToBlock) {
            val posDelta = blockFixture.getBody().getPositionDelta()
            body.translate(posDelta)
        }

        feetFixture.receiver(Receivers.FEET_BLOCK_CONTACT)?.invoke(ProcessState.BEGIN, block)

        body.setBodySense(BodySense.FEET_ON_GROUND, true)

        block.hitByFeet(ProcessState.BEGIN, feetFixture)
        feetFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN)?.invoke(block, delta)
    }

    private fun continueContact(feetFixture: IFixture, blockFixture: IFixture, delta: Float) {
        if (blockFixture.hasFixtureLabel(FixtureLabel.NO_FEET_TOUCHIE)) return
        if (feetFixture.receiver(Receivers.FILTER)?.invoke(blockFixture) == false) return

        val block = blockFixture.getEntity() as Block

        val body = feetFixture.getBody()
        if (!blockFixture.getBody().physics.collisionOn) {
            body.removeFeetBlock(block)
            if (!body.hasAnyFeetBlock()) body.setBodySense(BodySense.FEET_ON_GROUND, false)
            return
        }

        body.addFeetBlock(block)

        val stickToBlock =
            feetFixture.receiver(Receivers.STICK_TO_BLOCK)?.invoke(ProcessState.CONTINUE, blockFixture) ?: true
        if (stickToBlock) {
            val posDelta = blockFixture.getBody().getPositionDelta()
            body.translate(posDelta)
        }

        feetFixture.receiver(Receivers.FEET_BLOCK_CONTACT)?.invoke(ProcessState.CONTINUE, block)

        body.setBodySense(BodySense.FEET_ON_GROUND, true)

        block.hitByFeet(ProcessState.CONTINUE, feetFixture)
        feetFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.CONTINUE)?.invoke(block, delta)
    }

    private fun end(feetFixture: IFixture, blockFixture: IFixture, delta: Float) {
        if (blockFixture.hasFixtureLabel(FixtureLabel.NO_FEET_TOUCHIE)) return
        if (feetFixture.receiver(Receivers.FILTER)?.invoke(blockFixture) == false) return

        val block = blockFixture.getEntity() as Block
        val body = feetFixture.getBody()

        body.removeFeetBlock(block)
        if (!body.hasAnyFeetBlock()) body.setBodySense(BodySense.FEET_ON_GROUND, false)

        feetFixture.receiver(Receivers.FEET_BLOCK_CONTACT)?.invoke(ProcessState.END, block)

        block.hitByFeet(ProcessState.END, feetFixture)
        feetFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.END)?.invoke(block, delta)
    }
}
