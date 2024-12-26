package com.mega.game.engine.behaviors

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.spyk

class BehaviorTest :
    DescribeSpec({
        describe("AbstractBehavior") {
            class MockBehaviorImpl : AbstractBehaviorImpl() {

                var evaluate = false

                var init = false
                var end = false

                var act = 0f

                override fun evaluate(delta: Float) = evaluate

                override fun init() {
                    init = true
                }

                override fun act(delta: Float) {
                    act += delta
                }

                override fun end() {
                    end = true
                }
            }

            lateinit var mockBehavior: MockBehaviorImpl

            beforeEach {
                clearAllMocks()
                mockBehavior = spyk(MockBehaviorImpl())
            }

            it("should be inactive initially") { mockBehavior.isActive() shouldBe false }

            it("should update correctly") {
                // if
                mockBehavior.evaluate = true

                // when
                mockBehavior.update(1f)

                // then
                mockBehavior.isActive() shouldBe true
                mockBehavior.init shouldBe true
                mockBehavior.end shouldBe false
                mockBehavior.act shouldBe 1f
            }

            it("should become inactive when evaluated to false") {
                // if
                mockBehavior.evaluate = false

                // when
                mockBehavior.update(0.1f)

                // then
                mockBehavior.isActive() shouldBe false
                mockBehavior.init shouldBe false
                mockBehavior.end shouldBe false
                mockBehavior.act shouldBe 0f
            }

            it("should end when becoming inactive") {
                // if
                mockBehavior.evaluate = true
                mockBehavior.update(1f)

                // when
                mockBehavior.evaluate = false
                mockBehavior.update(1f)

                // then
                mockBehavior.isActive() shouldBe false
                mockBehavior.init shouldBe true
                mockBehavior.end shouldBe true
                mockBehavior.act shouldBe 1f
            }
        }
    })
