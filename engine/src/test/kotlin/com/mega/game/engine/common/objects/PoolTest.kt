package com.mega.game.engine.common.objects

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PoolTest : DescribeSpec({
    describe("Pool") {

        // a mutable element with value-based equality, like Vector2 or GameRectangle: two distinct instances holding
        // the same values are "equal", and an instance's hash changes when it is mutated
        data class MutableValue(var x: Int = 0) {

            fun reset() {
                x = 0
            }
        }

        var supplied = 0

        fun newPool(
            startAmount: Int = 0,
            onFetch: ((MutableValue) -> Unit)? = null,
            onFree: ((MutableValue) -> Unit)? = null
        ): Pool<MutableValue> {
            supplied = 0
            return Pool(
                supplier = { supplied++; MutableValue() },
                startAmount = startAmount,
                onFetch = onFetch,
                onFree = onFree
            )
        }

        it("should supply a new element when the queue is empty") {
            val pool = newPool()

            pool.fetch()
            pool.fetch()

            supplied shouldBe 2
        }

        it("should return the same instance that was freed") {
            val pool = newPool()

            val first = pool.fetch()
            supplied shouldBe 1

            pool.free(first) shouldBe true

            val second = pool.fetch()

            (second === first) shouldBe true
            supplied shouldBe 1
        }

        it("should reject a double-free of the same instance") {
            val pool = newPool()
            val element = pool.fetch()

            pool.free(element) shouldBe true
            pool.free(element) shouldBe false

            // the rejected free must not have queued a second reference to the same instance
            val fetched = pool.fetch()
            (fetched === element) shouldBe true

            pool.fetch()
            supplied shouldBe 2
        }

        it("should pool two distinct instances that are equal by value") {
            val pool = newPool()

            val a = pool.fetch()
            val b = pool.fetch()
            a.x = 7
            b.x = 7

            (a == b) shouldBe true
            (a === b) shouldBe false

            // value-based membership would reject the second free and silently drop the instance
            pool.free(a) shouldBe true
            pool.free(b) shouldBe true

            supplied shouldBe 2

            pool.fetch()
            pool.fetch()

            supplied shouldBe 2
        }

        it("should keep reusing instances when onFetch resets the element and values repeat") {
            // this is the regression that broke every GameObjectPools type: onFetch resets the element, and the
            // freed values repeat every frame (e.g. darkness tile centers), so a value-hash guard would reject
            // every free after the first and force a fresh allocation on every fetch
            val pool = newPool(onFetch = { it.reset() })

            repeat(100) {
                val element = pool.fetch()
                element.x = 42
                pool.free(element) shouldBe true
            }

            supplied shouldBe 1
        }

        it("should reset the element via onFetch before handing it out") {
            val pool = newPool(onFetch = { it.reset() })

            val element = pool.fetch()
            element.x = 99
            pool.free(element)

            pool.fetch().x shouldBe 0
        }

        it("should invoke onFree when an element is accepted") {
            var freed = 0
            val pool = newPool(onFree = { freed++ })

            val element = pool.fetch()

            pool.free(element)
            freed shouldBe 1

            // a rejected double-free must not invoke onFree again
            pool.free(element)
            freed shouldBe 1
        }

        it("should prefill the queue with startAmount elements") {
            val pool = newPool(startAmount = 5)

            pool.fetch()
            supplied shouldBe 5
        }

        it("should clear both the queue and the free-set") {
            val pool = newPool()
            val element = pool.fetch()
            pool.free(element)

            pool.clear()

            // the queue is empty, so a new instance is supplied
            pool.fetch()
            supplied shouldBe 2

            // and the cleared free-set must not still consider the old instance pooled
            pool.free(element) shouldBe true
        }
    }
})
