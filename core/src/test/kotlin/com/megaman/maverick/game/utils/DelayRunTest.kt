package com.megaman.maverick.game.utils

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class DelayRunTest : DescribeSpec({

    lateinit var services: MegaServices

    beforeEach { services = testServices() }

    fun frame(delta: Float) {
        while (!services.runQueue.isEmpty) services.runQueue.removeLast().invoke()
        services.updatables.values().toList().forEach { it.update(delta) }
    }

    describe("delayRun") {
        it("runs the action once after the delay, then removes the run and reuses it for the next call") {
            var runs = 0
            val id = MegaUtilMethods.delayRun(services, 1f) { runs++ }
            val run = services.updatables[id]

            frame(0.5f)
            runs shouldBe 0
            frame(0.5f)
            runs shouldBe 1
            frame(1f)
            runs shouldBe 1
            services.updatables.containsKey(id) shouldBe false

            val next = MegaUtilMethods.delayRun(services, 0.25f) { runs += 10 }
            next shouldBe id
            services.updatables[next] shouldBeSameInstanceAs run
            frame(0.25f)
            runs shouldBe 11
        }

        it("gives each pending run its own id, and a cancelled run never runs") {
            var runs = 0
            val first = MegaUtilMethods.delayRun(services, 1f) { runs++ }
            val second = MegaUtilMethods.delayRun(services, 1f) { runs += 10 }
            first shouldNotBe second

            MegaUtilMethods.cancelDelayRun(services, first)
            frame(1f)

            runs shouldBe 10
            services.updatables.containsKey(first) shouldBe false
        }

        it("ignores a cancel of a run that has already finished") {
            var runs = 0
            val id = MegaUtilMethods.delayRun(services, 1f) { runs++ }
            frame(1f)

            MegaUtilMethods.cancelDelayRun(services, id)
            frame(0f)

            runs shouldBe 1
            val next = MegaUtilMethods.delayRun(services, 1f) { runs += 10 }
            val other = MegaUtilMethods.delayRun(services, 1f) { runs += 100 }
            next shouldNotBe other
            frame(1f)
            runs shouldBe 111
        }
    }
})
