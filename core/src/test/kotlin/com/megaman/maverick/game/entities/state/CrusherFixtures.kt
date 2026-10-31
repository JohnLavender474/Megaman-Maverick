package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.world.body.FixtureType
import io.mockk.every
import io.mockk.mockk

fun crusherTestPlayer(x: Float, y: Float): IGameEntity {
    val body = Body(BodyType.DYNAMIC)
    body.setSize(20f, 30f)
    body.setCenter(x, y)

    val head = Fixture(body, FixtureType.HEAD, GameRectangle().setSize(16f, 6f))
    head.offsetFromBodyAttachment.y = 15f
    body.addFixture(head)
    body.putProperty(ConstKeys.HEAD, head)

    val feet = Fixture(body, FixtureType.FEET, GameRectangle().setSize(16f, 6f))
    feet.offsetFromBodyAttachment.y = -15f
    body.addFixture(feet)
    body.putProperty(ConstKeys.FEET, feet)

    val left = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(6f, 20f))
    left.offsetFromBodyAttachment.x = -10f
    body.addFixture(left)
    body.putProperty("${ConstKeys.LEFT}_${ConstKeys.SIDE}", left)

    val right = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(6f, 20f))
    right.offsetFromBodyAttachment.x = 10f
    body.addFixture(right)
    body.putProperty("${ConstKeys.RIGHT}_${ConstKeys.SIDE}", right)

    return mockk<IGameEntity>(relaxed = true).also {
        every { it.getComponent(BodyComponent::class) } returns BodyComponent(body)
    }
}
