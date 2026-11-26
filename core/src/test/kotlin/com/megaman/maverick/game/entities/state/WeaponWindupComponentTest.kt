package com.megaman.maverick.game.entities.state

import com.megaman.maverick.game.weapons.WeaponWindup
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private class CountingWindup : WeaponWindup {

    var held = 0f
    var clears = 0

    override fun hold(delta: Float) {
        held += delta
    }

    override fun clear() {
        held = 0f
        clears++
    }
}

class WeaponWindupComponentTest : DescribeSpec({

    lateinit var component: WeaponWindupComponent
    lateinit var first: CountingWindup
    lateinit var second: CountingWindup

    beforeEach {
        component = WeaponWindupComponent()
        first = CountingWindup()
        second = CountingWindup()
    }

    it("has no active windup at the start") {
        component.active shouldBe null
    }

    it("holds a windup and makes it active") {
        component.hold(first, 0.5f)
        component.hold(first, 0.25f)

        component.active shouldBe first
        first.held shouldBe 0.75f
        first.clears shouldBe 0
    }

    it("clears the old windup when another one is held") {
        component.hold(first, 0.5f)

        component.hold(second, 0.25f)

        component.active shouldBe second
        first.clears shouldBe 1
        first.held shouldBe 0f
        second.held shouldBe 0.25f
    }

    it("clears the active windup on a stop") {
        component.hold(first, 0.5f)

        component.stop()

        component.active shouldBe null
        first.clears shouldBe 1
    }

    it("clears nothing on a stop with no active windup") {
        component.stop()
        component.hold(first, 0.5f)
        component.stop()
        component.stop()

        first.clears shouldBe 1
    }

    it("stops on a reset") {
        component.hold(first, 0.5f)

        component.reset()

        component.active shouldBe null
        first.clears shouldBe 1
    }
})
