package com.megaman.maverick.game.entities.state

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private const val HALF_TIME = 0.75f
private const val FULL_TIME = 1.5f

class WeaponChargeComponentTest : DescribeSpec({

    var halfMarks = 0
    lateinit var charge: WeaponChargeComponent

    beforeEach {
        halfMarks = 0
        charge = WeaponChargeComponent(HALF_TIME, FULL_TIME) { halfMarks++ }
    }

    it("starts with no charge") {
        charge.half shouldBe false
        charge.full shouldBe false
    }

    it("is half at the half time") {
        charge.update(HALF_TIME - 0.01f)
        charge.half shouldBe false

        charge.update(0.01f)

        charge.half shouldBe true
        charge.full shouldBe false
    }

    it("is full at the full time") {
        charge.update(FULL_TIME - 0.01f)
        charge.full shouldBe false

        charge.update(0.01f)

        charge.half shouldBe true
        charge.full shouldBe true
    }

    it("runs the hook once per charge") {
        charge.update(HALF_TIME)
        charge.update(FULL_TIME)

        halfMarks shouldBe 1
    }

    it("runs the hook again after a reset") {
        charge.update(HALF_TIME)
        charge.reset()

        charge.update(HALF_TIME)

        halfMarks shouldBe 2
    }

    it("clears the charge on a reset") {
        charge.update(FULL_TIME)

        charge.reset()

        charge.half shouldBe false
        charge.full shouldBe false
    }
})
