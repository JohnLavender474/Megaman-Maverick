package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.bodyOverGround
import com.megaman.maverick.game.entities.megaman.constants.AButtonTask
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

        val entity = feetFixture.getEntity()
        if (entity is Megaman) {
            entity.aButtonTask = AButtonTask.JUMP

            val blockMakesSound = block.getOrDefaultProperty(
                "${ConstKeys.FEET}_${ConstKeys.SOUND}",
                !block.body.hasBodyLabel(BodyLabel.COLLIDE_DOWN_ONLY) || when (entity.direction) {
                    Direction.UP -> entity.body.physics.velocity.y <= 0f
                    Direction.DOWN -> entity.body.physics.velocity.y >= 0f
                    Direction.LEFT -> entity.body.physics.velocity.x >= 0f
                    Direction.RIGHT -> entity.body.physics.velocity.x <= 0f
                },
                Boolean::class
            )
            if (blockMakesSound && entity.canMakeLandSound) {
                entity.requestToPlaySound(SoundAsset.MEGAMAN_LAND_SOUND, false)
                entity.canMakeLandSound = false
            }
        }

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

        val entity = feetFixture.getEntity()
        if (entity is Megaman) entity.aButtonTask = AButtonTask.JUMP

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

        val entity = feetFixture.getEntity()
        if (entity is Megaman) {
            if (!entity.bodyOverGround) {
                entity.aButtonTask = when {
                    entity.body.isSensing(BodySense.IN_WATER) -> AButtonTask.SWIM
                    else -> AButtonTask.AIR_DASH
                }
            }
            if (!body.hasAnyFeetBlock()) entity.canMakeLandSound = true
        }

        block.hitByFeet(ProcessState.END, feetFixture)
        feetFixture.receiver(Receivers.HIT_BY_BLOCK, ProcessState.END)?.invoke(block, delta)
    }
}
