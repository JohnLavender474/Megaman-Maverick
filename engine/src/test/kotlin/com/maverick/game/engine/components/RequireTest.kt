package com.maverick.game.engine.components

import com.maverick.game.engine.MockGameEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class RequireTest : DescribeSpec({

    class TestComponent : IGameComponent
    class OtherComponent : IGameComponent

    describe("require") {
        it("gives the component") {
            val entity = MockGameEntity()
            val component = TestComponent()
            entity.addComponent(component)

            entity.require(TestComponent::class) shouldBeSameInstanceAs component
        }

        it("throws a MissingComponentException with the missing type when the component is missing") {
            val entity = MockGameEntity()
            entity.addComponent(TestComponent())

            val e = shouldThrow<MissingComponentException> { entity.require(OtherComponent::class) }

            e.type shouldBe OtherComponent::class
        }
    }
})
