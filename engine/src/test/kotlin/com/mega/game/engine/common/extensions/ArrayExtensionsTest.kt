package com.mega.game.engine.common.extensions

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ArrayExtensionFunctionsTest :
    DescribeSpec({
        describe("gdxArrayOf") {
            it("should create an Array from the given elements") {
                val array = gdxArrayOf(1, 2, 3)
                array.size shouldBe 3
                array[0] shouldBe 1
                array[1] shouldBe 2
                array[2] shouldBe 3
            }
        }

        describe("fill") {
            it("should fill the array with the specified value") {
                val array = gdxArrayOf(1, 2, 3)
                array.fill(0)
                array.size shouldBe 3
                array[0] shouldBe 0
                array[1] shouldBe 0
                array[2] shouldBe 0
            }

            it("should create a filled array") {
                val array = gdxFilledArrayOf(5, 1)
                array.size shouldBe 5
                array[0] shouldBe 1
                array[1] shouldBe 1
                array[2] shouldBe 1
                array[3] shouldBe 1
                array[4] shouldBe 1
            }
        }

        describe("filter") {
            it("should filter elements based on the given predicate") {
                val array = gdxArrayOf(1, 2, 3, 4, 5)
                val filteredArray = array.filter { it % 2 == 0 }
                filteredArray shouldBe gdxArrayOf(2, 4)
            }
        }

        describe("map") {
            it("should transform elements using the provided transform function") {
                val array = gdxArrayOf(1, 2, 3)
                val mappedArray = array.map { it * 2 }
                mappedArray shouldBe gdxArrayOf(2, 4, 6)
            }
        }
    })
