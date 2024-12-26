package com.mega.game.engine

import com.mega.game.engine.components.IGameComponent
import com.mega.game.engine.entities.GameEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks

class GameEntityTest :
    DescribeSpec({
        describe("Entity class") {

            class MockComponent : IGameComponent

            lateinit var entity: GameEntity
            lateinit var mockComponent: IGameComponent

            beforeEach {
                clearAllMocks()
                entity = MockGameEntity()
                mockComponent = MockComponent()
                entity.clearComponents()
            }

            it("should put, get, and remove properties correctly") {
                val key = "propertyKey"
                val value = "propertyValue"

                entity.putProperty(key, value)
                entity.getProperty(key) shouldBe value
                entity.hasProperty(key) shouldBe true

                entity.removeProperty(key)
                entity.getProperty(key) shouldBe null
                entity.hasProperty(key) shouldBe false
            }

            it("should put and get componentMap correctly") {
                entity.addComponent(mockComponent)
                entity.getComponent(MockComponent::class) shouldBe mockComponent
                entity.hasComponent(MockComponent::class) shouldBe true

                entity.removeComponent(MockComponent::class)
                entity.getComponent(MockComponent::class) shouldBe null
                entity.hasComponent(MockComponent::class) shouldBe false
            }

            it("should clear componentMap correctly") {
                entity.addComponent(mockComponent)
                entity.clearComponents()

                entity.getComponent(MockComponent::class) shouldBe null
                entity.hasComponent(MockComponent::class) shouldBe false
            }
        }
    })
