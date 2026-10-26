package com.maverick.game.engine.entities

import com.maverick.game.engine.MockGameEntity
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.components.MissingComponentException
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.input.InputButton
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

private class OrientedEntity : GameEntity(mockk(relaxed = true)), IFaceable, IDirectional {
    override var facing = Facing.LEFT
    override var direction = Direction.DOWN
    override fun onSpawn(spawnProps: Properties) {}
    override fun onDestroy() {}
}

private data class QueryButton(override val index: Int) : InputButton

class EntityQueriesTest : DescribeSpec({

    describe("entity queries on an entity without the parts") {
        it("fail at the read for needed parts") {
            val entity = MockGameEntity()

            shouldThrow<MissingComponentException> { entity.body }.type shouldBe BodyComponent::class
            shouldThrow<IllegalStateException> { entity.facing }
            shouldThrow<IllegalStateException> { entity.direction }
        }

        it("give the empty value for optional parts") {
            val entity = MockGameEntity()

            entity.hasBody shouldBe false
            entity.isBehaviorActive("any") shouldBe false
            entity.isAnyBehaviorActive("any", "other") shouldBe false
            entity.input shouldBe InputFrame.RELEASED
        }

        it("give RELEASED for a controller component with no input source") {
            val entity = MockGameEntity().also { it.addComponent(ControllerComponent()) }

            entity.input shouldBe InputFrame.RELEASED
        }
    }

    describe("entity queries on an entity with the parts") {
        it("read the body, facing, direction, behaviors and input") {
            val entity = OrientedEntity()
            val body = Body(BodyType.DYNAMIC)
            val behavior = mockk<IBehavior>(relaxed = true) { every { isActive() } returns true }
            val frame = InputFrame.RELEASED.with(QueryButton(2), ButtonStatus.PRESSED)
            entity.addComponent(BodyComponent(body))
            entity.addComponent(BehaviorsComponent().also { it.addBehavior("jump", behavior) })
            entity.addComponent(ControllerComponent().also { it.input = InputSource { frame } })

            entity.hasBody shouldBe true
            entity.body shouldBeSameInstanceAs body
            entity.facing shouldBe Facing.LEFT
            entity.direction shouldBe Direction.DOWN
            entity.isBehaviorActive("jump") shouldBe true
            entity.isAnyBehaviorActive("walk", "jump") shouldBe true
            entity.input shouldBe frame
        }
    }
})
