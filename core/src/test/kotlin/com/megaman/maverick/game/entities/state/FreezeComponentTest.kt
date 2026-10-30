package com.megaman.maverick.game.entities.state

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class FreezeComponentTest : DescribeSpec({

    lateinit var calls: ArrayList<String>
    var mashed = false

    fun component() = FreezeComponent(
        MashThawRule(
            2,
            0.2f,
            mashed = {
                calls.add("read")
                mashed
            },
            onMash = { calls.add("mash") }
        ),
        onThaw = { calls.add("thaw") }
    )

    beforeEach {
        calls = ArrayList()
        mashed = false
    }

    describe("FreezeComponent") {
        it("does not read the mash while not frozen") {
            val component = component()

            component.update(1f)

            calls.shouldBeEmpty()
            component.frozen shouldBe false
        }

        it("does not read the mash during the cooldown") {
            val component = component()
            mashed = true
            component.freeze()

            component.update(0.01f)
            calls.clear()
            component.update(0.01f)

            calls.shouldBeEmpty()
        }

        it("thaws in the frame of the last mash, with the mash before the thaw") {
            val component = component()
            mashed = true
            component.freeze()

            component.update(0.01f)
            component.frozen shouldBe true

            calls.clear()
            component.update(0.3f)

            component.frozen shouldBe false
            calls shouldBe listOf("read", "mash", "thaw")
        }

        it("counts the mashes from zero on each freeze") {
            val component = component()
            mashed = true
            component.freeze()
            component.update(0.01f)

            component.reset()
            component.freeze()
            component.update(0.01f)

            component.frozen shouldBe true
        }

        it("does nothing on a second freeze") {
            val component = component()
            mashed = true
            component.freeze()
            component.update(0.01f)
            component.update(0.01f)

            component.freeze()
            calls.clear()
            component.update(0.01f)

            calls.shouldBeEmpty()
        }

        it("has the same state after reset as a new instance, and keeps its hooks") {
            val component = component()
            component.freeze()

            component.reset()

            component.frozen shouldBe false
            mashed = true
            component.freeze()
            component.update(0.01f)
            calls.contains("mash") shouldBe true
        }
    }
})
