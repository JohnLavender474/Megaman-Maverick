package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class BodyGravityChangeRule(
    private val changeDirection: (entity: IDirectional, direction: Direction) -> Unit
) : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = continueContact(a, b)

    private fun begin(bodyFixture: IFixture, gravityChangeFixture: IFixture) {
        val body = bodyFixture.getBody()
        val pd = body.getPositionDelta()

        val bodyPointToCheck = when {
            pd.x > 0f -> {
                if (pd.y > 0f) body.getPositionPoint(Position.TOP_RIGHT)
                else if (pd.y < 0f) body.getPositionPoint(Position.BOTTOM_RIGHT) else body.getPositionPoint(Position.CENTER_RIGHT)
            }

            pd.x < 0f -> {
                if (body.getPositionDelta().y > 0f) body.getPositionPoint(Position.TOP_LEFT)
                else if (body.getPositionDelta().y < 0f) body.getPositionPoint(Position.BOTTOM_LEFT)
                else body.getPositionPoint(Position.CENTER_LEFT)
            }

            else -> {
                if (pd.y > 0f) body.getPositionPoint(Position.TOP_CENTER)
                else if (pd.y < 0f) body.getPositionPoint(Position.BOTTOM_CENTER) else body.getCenter()
            }
        }

        if (!gravityChangeFixture.getShape().contains(bodyPointToCheck)) return

        val entity = bodyFixture.getEntity()

        if (gravityChangeFixture.hasProperty(ConstKeys.GRAVITY) && entity is IScalableGravityEntity) {
            val canChangeGravityValue =
                bodyFixture.getOrDefaultProperty(ConstKeys.GRAVITY_CHANGEABLE, true, Boolean::class)

            if (canChangeGravityValue) {
                val scalar = gravityChangeFixture.getProperty(ConstKeys.GRAVITY, Float::class)!!
                entity.gravityScalar = scalar
            }
        }

        if (gravityChangeFixture.hasProperty(ConstKeys.DIRECTION)) {
            val canChangeGravityRotation =
                bodyFixture.getOrDefaultProperty(ConstKeys.GRAVITY_ROTATABLE, true, Boolean::class)
            if (canChangeGravityRotation) {
                val direction = gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return
                if (entity is IDirectional && entity.direction != direction)
                    changeDirection(entity, direction)
            }
        }
    }

    private fun continueContact(bodyFixture: IFixture, gravityChangeFixture: IFixture) {
        val direction = gravityChangeFixture.getProperty(ConstKeys.DIRECTION, Direction::class) ?: return

        val canChangeGravity = bodyFixture.properties.getOrDefault(ConstKeys.GRAVITY_ROTATABLE, true) as Boolean
        if (!canChangeGravity) return

        val body = bodyFixture.getBody()
        val pd = body.getPositionDelta()

        val bodyPointToCheck = when {
            pd.x > 0f -> when {
                pd.y > 0f -> body.getPositionPoint(Position.TOP_RIGHT)
                pd.y < 0f -> body.getPositionPoint(Position.BOTTOM_RIGHT)
                else -> body.getPositionPoint(Position.CENTER_RIGHT)
            }

            pd.x < 0f -> when {
                body.getPositionDelta().y > 0f -> body.getPositionPoint(Position.TOP_LEFT)
                body.getPositionDelta().y < 0f -> body.getPositionPoint(Position.BOTTOM_LEFT)
                else -> body.getPositionPoint(Position.CENTER_LEFT)
            }

            else -> when {
                pd.y > 0f -> body.getPositionPoint(Position.TOP_CENTER)
                pd.y < 0f -> body.getPositionPoint(Position.BOTTOM_CENTER)
                else -> body.getCenter()
            }
        }

        if (!gravityChangeFixture.getShape().contains(bodyPointToCheck)) return

        val entity = bodyFixture.getEntity()
        if (entity is IDirectional && entity.direction != direction) changeDirection(entity, direction)
    }
}
