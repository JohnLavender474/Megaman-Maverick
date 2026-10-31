package com.megaman.maverick.game.entities.state

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BubbleEmitterComponentTest : DescribeSpec({

    var canEmit = true
    var emits = 0
    lateinit var emitter: BubbleEmitterComponent

    beforeEach {
        canEmit = true
        emits = 0
        emitter = BubbleEmitterComponent(2f, canEmit = { canEmit }, emit = { emits++ })
    }

    describe("the bubble emitter") {
        it("does not emit before the delay") {
            emitter.update(1.9f)

            emits shouldBe 0
        }

        it("emits once at the delay") {
            emitter.update(2f)

            emits shouldBe 1
        }

        it("starts the delay again after it emits") {
            emitter.update(2f)
            emitter.update(1.9f)
            emits shouldBe 1

            emitter.update(0.1f)
            emits shouldBe 2
        }

        it("does not move the timer when it cannot emit") {
            canEmit = false
            emitter.update(5f)
            emits shouldBe 0

            canEmit = true
            emitter.update(1.9f)
            emits shouldBe 0
        }

        it("keeps the time across frames in which it cannot emit") {
            emitter.update(1f)

            canEmit = false
            emitter.update(5f)

            canEmit = true
            emitter.update(1f)

            emits shouldBe 1
        }

        it("keeps the time through a reset") {
            emitter.update(1f)

            emitter.reset()
            emitter.update(1f)

            emits shouldBe 1
        }
    }
})
