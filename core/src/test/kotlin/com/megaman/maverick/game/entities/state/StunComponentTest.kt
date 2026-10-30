package com.megaman.maverick.game.entities.state

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StunComponentTest : DescribeSpec({

    fun component() = StunComponent(0.25f, onSurface = { true }, onStun = {})

    describe("StunComponent") {
        it("starts not stunned") {
            component().stunned shouldBe false
        }

        it("counts down only on the ground") {
            val component = component()
            component.start()

            component.update(1f, false)
            component.stunned shouldBe true

            component.update(1f, true)
            component.stunned shouldBe false
        }

        it("has the same state after reset as a new instance, and keeps its hooks") {
            var stuns = 0
            val onStun = { stuns++; Unit }
            val onSurface = { true }
            val component = StunComponent(0.25f, onSurface, onStun)
            component.start()

            component.reset()

            component.stunned shouldBe false
            component.onStun shouldBe onStun
            component.onSurface shouldBe onSurface
        }
    }
})
