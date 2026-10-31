package com.megaman.maverick.game.entities.state

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TrailSpriteComponentTest : DescribeSpec({

    var spawns = true
    var tries = 0
    lateinit var trail: TrailSpriteComponent

    beforeEach {
        spawns = true
        tries = 0
        trail = TrailSpriteComponent(0.1f, trySpawn = {
            tries++
            spawns
        })
    }

    describe("the trail sprite timer") {
        it("does not try to spawn before the delay") {
            trail.update(0.09f)

            tries shouldBe 0
        }

        it("starts the delay again after a spawn") {
            trail.update(0.1f)
            trail.update(0.09f)
            tries shouldBe 1

            trail.update(0.01f)
            tries shouldBe 2
        }

        it("tries again in the next frame after no spawn") {
            spawns = false
            trail.update(0.1f)
            trail.update(0.01f)
            tries shouldBe 2

            spawns = true
            trail.update(0.01f)
            tries shouldBe 3

            trail.update(0.01f)
            tries shouldBe 3
        }

        it("keeps the time through a reset") {
            trail.update(0.05f)

            trail.reset()
            trail.update(0.05f)

            tries shouldBe 1
        }
    }
})
