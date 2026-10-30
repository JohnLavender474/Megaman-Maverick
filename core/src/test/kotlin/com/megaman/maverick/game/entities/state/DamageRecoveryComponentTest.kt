package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.time.Timer
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DamageRecoveryComponentTest : DescribeSpec({

    describe("DamageRecoveryComponent") {
        it("starts not recovering, with no flash") {
            val component = DamageRecoveryComponent(1f, 0.1f)

            component.recovering shouldBe false
            component.flash shouldBe false
        }

        it("starts on the frame that damage ends, with no false frame between them") {
            val damageTimer = Timer(0.5f)
            val component = DamageRecoveryComponent(1.5f, 0.05f)
            val delta = 1f / 60f

            damageTimer.update(0f)
            do {
                damageTimer.update(delta)
                val damaged = !damageTimer.isFinished()
                component.update(delta, damaged, damageTimer.isJustFinished())
                (damaged || component.recovering) shouldBe true
            } while (damaged)
        }

        it("toggles the flash at the interval and ends with no flash") {
            val component = DamageRecoveryComponent(1f, 0.1f)
            component.update(0f, false, true)

            component.update(0.05f, false, false)
            component.flash shouldBe false

            component.update(0.05f, false, false)
            component.flash shouldBe true

            component.update(0.1f, false, false)
            component.flash shouldBe false

            component.update(0.1f, false, false)
            component.flash shouldBe true

            component.update(1f, false, false)
            component.recovering shouldBe false
            component.flash shouldBe false
        }

        it("does not count down while damaged") {
            val component = DamageRecoveryComponent(1f, 0.1f)
            component.update(0f, false, true)

            component.update(5f, true, false)

            component.recovering shouldBe true
        }

        it("has the same state after reset as a new instance") {
            val component = DamageRecoveryComponent(1f, 0.1f)
            component.update(0f, false, true)
            component.update(0.1f, false, false)

            component.reset()

            component.recovering shouldBe false
            component.flash shouldBe false
        }
    }
})
