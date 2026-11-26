package com.megaman.maverick.game.weapons

import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private const val HALF_TIME = 0.75f
private const val FULL_TIME = 1.5f

class BusterChargeTest : DescribeSpec({

    var halfMarks = 0
    var fullMarks = 0
    var clears = 0
    var scalar = 1f
    lateinit var charge: BusterCharge

    beforeEach {
        halfMarks = 0
        fullMarks = 0
        clears = 0
        scalar = 1f
        charge = BusterCharge(
            fullTime = FULL_TIME,
            halfTime = HALF_TIME,
            speedScalar = { scalar },
            onHalfCharged = { halfMarks++ },
            onFullyCharged = { fullMarks++ },
            onCleared = { clears++ }
        )
    }

    it("starts with no charge") {
        charge.status() shouldBe MegaChargeStatus.NOT_CHARGED
    }

    it("is half charged at the half time") {
        charge.hold(HALF_TIME - 0.01f)
        charge.status() shouldBe MegaChargeStatus.NOT_CHARGED

        charge.hold(0.01f)

        charge.status() shouldBe MegaChargeStatus.HALF_CHARGED
    }

    it("is fully charged at the full time") {
        charge.hold(FULL_TIME - 0.01f)
        charge.status() shouldBe MegaChargeStatus.HALF_CHARGED

        charge.hold(0.01f)

        charge.status() shouldBe MegaChargeStatus.FULLY_CHARGED
    }

    it("runs each threshold hook once per charge, also on one step past both") {
        charge.hold(FULL_TIME + 1f)
        charge.hold(FULL_TIME)

        halfMarks shouldBe 1
        fullMarks shouldBe 1
    }

    it("runs the threshold hooks again after a clear") {
        charge.hold(FULL_TIME)
        charge.clear()

        charge.hold(FULL_TIME)

        halfMarks shouldBe 2
        fullMarks shouldBe 2
    }

    it("clears the charge and runs the clear hook") {
        charge.hold(FULL_TIME)

        charge.clear()

        charge.status() shouldBe MegaChargeStatus.NOT_CHARGED
        clears shouldBe 1
    }

    it("scales the held time by the speed scalar") {
        scalar = 2f

        charge.hold(HALF_TIME / 2f)

        charge.status() shouldBe MegaChargeStatus.HALF_CHARGED
        halfMarks shouldBe 1
    }
})
