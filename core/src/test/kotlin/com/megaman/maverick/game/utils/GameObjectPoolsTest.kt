package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class GameObjectPoolsTest : DescribeSpec({

    fun reclaim(frames: Int) = repeat(frames) { GameObjectPools.performNextReclaim() }

    beforeEach { reclaim(GameObjectPools.SCRATCH_LIFETIME_FRAMES) }

    afterEach {
        GameObjectPools.debugChecks = false
        reclaim(GameObjectPools.SCRATCH_LIFETIME_FRAMES)
    }

    describe("a scratch object") {

        it("is not given out again before the reclaim at the end of its third frame") {
            val scratch = GameObjectPools.fetch(Vector2::class).set(1f, 2f)

            reclaim(GameObjectPools.SCRATCH_LIFETIME_FRAMES - 1)

            GameObjectPools.fetch(Vector2::class) shouldNotBeSameInstanceAs scratch
            scratch shouldBe Vector2(1f, 2f)
        }

        it("goes back to the pool at the reclaim at the end of its third frame") {
            val scratch = GameObjectPools.fetch(Vector2::class).set(1f, 2f)

            reclaim(GameObjectPools.SCRATCH_LIFETIME_FRAMES)

            val next = GameObjectPools.fetch(Vector2::class)
            next shouldBeSameInstanceAs scratch
            next shouldBe Vector2(0f, 0f)
        }
    }

    describe("the debug checks") {

        it("poison a scratch object at its reclaim and never give it out again") {
            GameObjectPools.debugChecks = true
            val scratch = GameObjectPools.fetch(GameRectangle::class).set(1f, 2f, 3f, 4f)

            reclaim(GameObjectPools.SCRATCH_LIFETIME_FRAMES - 1)
            scratch.getX() shouldBe 1f

            reclaim(1)
            scratch.getX().isNaN().shouldBeTrue()
            scratch.getHeight().isNaN().shouldBeTrue()
            repeat(20) { GameObjectPools.fetch(GameRectangle::class) shouldNotBeSameInstanceAs scratch }
        }
    }

    it("throws for a type that has no pool") {
        shouldThrow<IllegalArgumentException> { GameObjectPools.fetch(String::class) }
    }
})
