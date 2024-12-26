package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.gdxArrayOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class LoopSpec : DescribeSpec({
    describe("Loop") {

        lateinit var elements: Array<String>
        lateinit var loop: Loop<String>

        fun initialize(startBeforeFirst: Boolean = false) {
            elements = gdxArrayOf("a", "b", "c")
            loop = Loop(elements, startBeforeFirst)
        }

        describe("with elements and not starting before the first element") {
            it("should return elements in order and loop back to the start") {
                initialize()
                loop.getCurrent() shouldBe "a"
                loop.next() shouldBe "b"
                loop.next() shouldBe "c"
                loop.next() shouldBe "a"
            }

            it("should get the current element correctly") {
                initialize()
                loop.next() // "b"
                loop.getCurrent() shouldBe "b"
                loop.next() // "c"
                loop.getCurrent() shouldBe "c"
            }
        }

        describe("with elements and starting before the first element") {
            it("should start before the first element and loop correctly") {
                initialize(true)
                loop.isBeforeFirst() shouldBe true
                loop.next() shouldBe "a"
                loop.isBeforeFirst() shouldBe false
            }

            it("should throw NoSuchElementException when calling getCurrent before calling next") {
                initialize(true)
                shouldThrow<NoSuchElementException> {
                    loop.getCurrent()
                }
            }
        }

        describe("resetting the loop") {
            it("should reset to the first element") {
                initialize()
                loop.next() // "b"
                loop.reset() // back to "a"
                loop.getCurrent() shouldBe "a"
                loop.next() shouldBe "b"
            }
        }

        describe("setting the index manually") {
            it("should set and get the correct index") {
                initialize()
                loop.setIndex(2)
                loop.getCurrent() shouldBe "c"
                loop.next() shouldBe "a"
            }
        }

        describe("equality and hashcode") {
            val loop1 = Loop(elements)
            val loop2 = Loop(loop1)

            it("should be equal if they have the same elements and index") {
                loop1 shouldBe loop2
                loop1.hashCode() shouldBe loop2.hashCode()
            }

            it("should be equal even if they have different elements or index") {
                loop1.next()
                loop1 shouldBe loop2
            }
        }
    }
})
