package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.addFixtureLabel
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.setReceiver

class HalfDomeShield(
    body: Body,
    radius: Float,
    private val direction: () -> Direction,
    private val passes: (IProjectileEntity) -> Boolean
) {

    private val fixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(radius))

    private val center = GameRectangle()
    private val localCenter = GameRectangle()
    private val shotBounds = GameRectangle()
    private val localShot = GameRectangle()

    init {
        fixture.addFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION)
        fixture.setReceiver(Receivers.NO_PROJECTILE_COLLISION_EXCEPTION) { projectile, _ ->
            !passes(projectile) && projectile is IBodyEntity && reachesDome(projectile)
        }
        body.addFixture(fixture)
        body.preProcess.put(ConstKeys.SHIELD) {
            fixture.bodyAttachmentPosition = when (direction()) {
                Direction.UP -> Position.BOTTOM_CENTER
                Direction.DOWN -> Position.TOP_CENTER
                Direction.LEFT -> Position.CENTER_RIGHT
                Direction.RIGHT -> Position.CENTER_LEFT
            }
        }
    }

    private fun reachesDome(projectile: IBodyEntity): Boolean {
        val circle = fixture.getShape() as GameCircle
        val circleCenter = circle.getCenter(GameObjectPools.fetch(Vector2::class))
        center.set(circleCenter.x, circleCenter.y, 0f, 0f)
        GravityFrame.toLocal(direction(), center, localCenter)
        GravityFrame.toLocal(direction(), projectile.body.getBounds(shotBounds), localShot)
        return localShot.getMaxY() >= localCenter.getY()
    }
}
