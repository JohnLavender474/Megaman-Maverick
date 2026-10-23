package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

object FeetLadderRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a)

    private fun begin(feetFixture: IFixture, ladderFixture: IFixture) {
        val body = feetFixture.getBody()
        body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
        body.putProperty(ConstKeys.LADDER, ladderFixture.getEntity())
    }

    private fun continueContact(feetFixture: IFixture, ladderFixture: IFixture) {
        val feetEntity = feetFixture.getEntity()

        val feetDirection = if (feetEntity is IDirectional) feetEntity.direction else Direction.UP
        val feetPoint = when (feetDirection) {
            Direction.UP -> feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.BOTTOM_CENTER)
            Direction.DOWN -> feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.TOP_CENTER)
            Direction.LEFT -> feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.CENTER_RIGHT)
            Direction.RIGHT -> feetFixture.getShape().getBoundingRectangle().getPositionPoint(Position.CENTER_LEFT)
        }

        if (ladderFixture.getShape().contains(feetPoint)) {
            val body = feetFixture.getBody()
            body.setBodySense(BodySense.FEET_TOUCHING_LADDER, true)
            body.putProperty(ConstKeys.LADDER, ladderFixture.getEntity())
        }
    }

    private fun end(feetFixture: IFixture) {
        val body = feetFixture.getBody()
        body.setBodySense(BodySense.FEET_TOUCHING_LADDER, false)
        if (!body.isSensing(BodySense.HEAD_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
    }
}
