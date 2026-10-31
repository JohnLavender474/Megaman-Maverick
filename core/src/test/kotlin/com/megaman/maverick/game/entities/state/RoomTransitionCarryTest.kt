package com.megaman.maverick.game.entities.state

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

private class CarriedEntity : GameEntity(mockk(relaxed = true)), IBodyEntity {

    var carries = true

    init {
        addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
        addComponent(RoomTransitionCarryComponent(0.35f, carries = { carries }))
    }

    val carry: RoomTransitionCarryComponent
        get() = getComponent(RoomTransitionCarryComponent::class)!!

    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

class RoomTransitionCarryTest : DescribeSpec({

    lateinit var entity: CarriedEntity

    beforeEach { entity = CarriedEntity() }

    describe("the room-transition carry") {
        it("brings back the y velocity for a vertical direction") {
            entity.body.physics.velocity.set(3f, 7f)
            entity.beginRoomTransitionCarry()

            entity.body.physics.velocity.y = 0f
            entity.endRoomTransitionCarry()

            entity.body.physics.velocity shouldBe Vector2(3f, 7f)
            entity.carry.carriedY shouldBe null
        }

        it("brings back the x velocity for a horizontal direction") {
            entity.body.direction = Direction.LEFT
            entity.body.physics.velocity.set(3f, 7f)
            entity.beginRoomTransitionCarry()

            entity.body.physics.velocity.x = 0f
            entity.endRoomTransitionCarry()

            entity.body.physics.velocity shouldBe Vector2(3f, 7f)
            entity.carry.carriedX shouldBe null
        }

        it("zeroes the velocity at both ends when it does not carry") {
            entity.carries = false
            entity.body.physics.velocity.set(3f, 7f)

            entity.beginRoomTransitionCarry()
            entity.body.physics.velocity shouldBe Vector2(0f, 0f)

            entity.body.physics.velocity.set(3f, 7f)
            entity.endRoomTransitionCarry()
            entity.body.physics.velocity shouldBe Vector2(0f, 0f)
        }

        it("brings back the y velocity when the direction changes during the transition") {
            entity.body.physics.velocity.set(3f, 7f)
            entity.beginRoomTransitionCarry()

            entity.body.direction = Direction.LEFT
            entity.body.physics.velocity.setZero()
            entity.endRoomTransitionCarry()

            entity.body.physics.velocity shouldBe Vector2(0f, 7f)
            entity.carry.carriedY shouldBe null
        }

        it("brings back the x velocity when the direction changes during the transition") {
            entity.body.direction = Direction.LEFT
            entity.body.physics.velocity.set(3f, 7f)
            entity.beginRoomTransitionCarry()

            entity.body.direction = Direction.UP
            entity.body.physics.velocity.setZero()
            entity.endRoomTransitionCarry()

            entity.body.physics.velocity shouldBe Vector2(3f, 0f)
            entity.carry.carriedX shouldBe null
        }

        it("keeps zero velocity when it carries only at the end") {
            entity.carries = false
            entity.body.physics.velocity.set(3f, 7f)
            entity.beginRoomTransitionCarry()

            entity.carries = true
            entity.endRoomTransitionCarry()

            entity.body.physics.velocity shouldBe Vector2(0f, 0f)
        }

        it("clears the carried value at the end when it does not carry") {
            entity.body.physics.velocity.set(3f, 7f)
            entity.beginRoomTransitionCarry()

            entity.carries = false
            entity.endRoomTransitionCarry()

            entity.carry.carriedY shouldBe null
        }

        it("does not bring back a stale value from an earlier transition") {
            entity.body.physics.velocity.set(3f, 7f)
            entity.beginRoomTransitionCarry()
            entity.carry.reset()

            entity.carries = false
            entity.body.physics.velocity.setZero()
            entity.beginRoomTransitionCarry()

            entity.carries = true
            entity.endRoomTransitionCarry()

            entity.body.physics.velocity shouldBe Vector2(0f, 0f)
        }

        it("pauses from the start of the transition until the timer ends") {
            entity.roomTransitionPaused shouldBe false

            entity.beginRoomTransitionCarry()
            entity.roomTransitionPaused shouldBe true

            entity.carry.update(1f)
            entity.roomTransitionPaused shouldBe false
        }

        it("clears the pause on reset and keeps the carried values and the hook") {
            val carries = entity.carry.carries
            entity.body.physics.velocity.set(3f, 7f)
            entity.beginRoomTransitionCarry()

            entity.carry.reset()

            entity.roomTransitionPaused shouldBe false
            entity.carry.carriedY shouldBe 7f
            (entity.carry.carries === carries) shouldBe true
        }
    }
})
