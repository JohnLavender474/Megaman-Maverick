package com.maverick.game.engine.components

import com.maverick.game.engine.MockGameEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs

class ComponentKeyTest : DescribeSpec({

    class TestComponent : IGameComponent
    class OtherComponent : IGameComponent

    describe("ComponentKey") {
        it("gives the component of its type") {
            val entity = MockGameEntity()
            val component = TestComponent()
            entity.addComponent(component)

            entity[ComponentKey(TestComponent::class)] shouldBeSameInstanceAs component
        }

        it("gives null when the entity has no component of its type") {
            MockGameEntity()[ComponentKey(TestComponent::class)] shouldBe null
        }
    }

    describe("require") {
        it("gives the component") {
            val entity = MockGameEntity()
            val component = TestComponent()
            entity.addComponent(component)

            entity.require(TestComponent::class) shouldBeSameInstanceAs component
        }

        it("fails with the entity and the component name when the component is missing") {
            val entity = MockGameEntity()
            entity.addComponent(TestComponent())

            val e = shouldThrow<IllegalStateException> { entity.require(OtherComponent::class) }

            e.message!! shouldContain "has no OtherComponent"
            e.message!! shouldContain entity.toString()
        }
    }
})
