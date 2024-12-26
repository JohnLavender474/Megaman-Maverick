package com.mega.game.engine.world.body

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class BodyComponentTest :
    DescribeSpec({
        describe("BodyComponent class") {
            val mockBody = mockk<Body>()
            val bodyComponent = BodyComponent(mockBody)

            it("should have the correct initial properties") { bodyComponent.body shouldBe mockBody }

            it("should reset the fixtureBody when reset() is called") {
                every { mockBody.reset() } just Runs
                bodyComponent.reset()
                verify { mockBody.reset() }
            }
        }
    })
