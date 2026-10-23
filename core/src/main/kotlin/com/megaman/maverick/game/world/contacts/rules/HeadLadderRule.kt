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

object HeadLadderRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b)

    override fun onEnd(a: IFixture, b: IFixture, delta: Float) = end(a)

    private fun begin(headFixture: IFixture, ladderFixture: IFixture) {
        val body = headFixture.getBody()
        body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
        body.properties.put(ConstKeys.LADDER, ladderFixture.getEntity())
    }

    private fun continueContact(headFixture: IFixture, ladderFixture: IFixture) {
        val headEntity = headFixture.getEntity()
        val headDirection = if (headEntity is IDirectional) headEntity.direction else Direction.UP

        val headPoint = when (headDirection) {
            Direction.UP -> headFixture.getShape().getBoundingRectangle().getPositionPoint(Position.TOP_CENTER)
            Direction.DOWN -> headFixture.getShape().getBoundingRectangle().getPositionPoint(Position.BOTTOM_CENTER)
            Direction.LEFT -> headFixture.getShape().getBoundingRectangle().getPositionPoint(Position.CENTER_LEFT)
            Direction.RIGHT -> headFixture.getShape().getBoundingRectangle().getPositionPoint(Position.CENTER_RIGHT)
        }

        if (ladderFixture.getShape().contains(headPoint)) {
            val body = headFixture.getBody()
            body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, true)
            body.putProperty(ConstKeys.LADDER, ladderFixture.getEntity())
        }
    }

    private fun end(headFixture: IFixture) {
        val body = headFixture.getBody()
        body.setBodySense(BodySense.HEAD_TOUCHING_LADDER, false)
        if (!body.isSensing(BodySense.FEET_TOUCHING_LADDER)) body.properties.remove(ConstKeys.LADDER)
    }
}
