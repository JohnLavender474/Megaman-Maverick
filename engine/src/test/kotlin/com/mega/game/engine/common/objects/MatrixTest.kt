package com.mega.game.engine.common.objects

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MatrixTest :
    DescribeSpec({
        describe("MatrixIterator") {
            lateinit var iterator: MatrixIterator<Int?>

            beforeEach {
                val matrix = Matrix<Int?>(3, 3)
                matrix[0, 0] = 1
                matrix[1, 0] = 2
                matrix[2, 0] = 3

                iterator = MatrixIterator(matrix)
            }

            it("should iterate through elements correctly") {
                val elements = mutableListOf<Int>()
                while (iterator.hasNext()) {
                    elements.add(iterator.next()!!)
                }
                elements shouldContainAll listOf(1, 2, 3)
            }

            it("should throw NoSuchElementException when there are no more elements") {
                for (i in 0 until 3) {
                    iterator.next()
                }
                shouldThrow<Exception> { iterator.next() }
            }

            it("should remove elements correctly") {
                // if
                val matrix = Matrix<Int?>(3, 3)
                matrix[0, 0] = 1
                matrix[1, 0] = 2
                matrix[2, 0] = 3

                // when
                iterator = MatrixIterator(matrix)
                val value = iterator.next()

                // then
                value shouldBe 1

                // when
                iterator.remove()

                // then
                matrix[0, 0] shouldBe null
                iterator.hasNext() shouldBe true
                iterator.next() shouldBe 2
            }
        }

        describe("Matrix") {
            lateinit var matrix: Matrix<Int>

            beforeEach { matrix = Matrix(3, 4) }

            it("should have the correct initial properties") {
                matrix.columns shouldBe 4
                matrix.rows shouldBe 3
                matrix.size shouldBe 0
            }

            describe("out of bounds") {
                it("should return true for row out of bounds") {
                    matrix.isOutOfBounds(0, 3) shouldBe true
                }

                it("should return true for column out of bounds") {
                    matrix.isOutOfBounds(4, 0) shouldBe true
                }

                it("should return false for row out of bounds") {
                    matrix.isOutOfBounds(0, 2) shouldBe false
                }

                it("should return false for column out of bounds") {
                    matrix.isOutOfBounds(3, 0) shouldBe false
                }

                it("should throw IndexOutOfBoundsException when getting an element") {
                    shouldThrow<IndexOutOfBoundsException> { matrix[4, 3] }
                }

                it("should throw IndexOutOfBoundsException when setting an element") {
                    shouldThrow<IndexOutOfBoundsException> { matrix.set(4, 3, 1) }
                }
            }

            it("should set elements correctly - test 1") {
                matrix[0, 0] = 1
                matrix[1, 1] = 2
                matrix[2, 2] = 3

                for (i in 0 until 3) {
                    for (j in 0 until 3) {
                        when (i pairTo j) {
                            0 pairTo 0 -> {
                                matrix[i, j] shouldBe 1
                            }

                            1 pairTo 1 -> {
                                matrix[i, j] shouldBe 2
                            }

                            2 pairTo 2 -> {
                                matrix[i, j] shouldBe 3
                            }

                            else -> {
                                matrix[i, j] shouldBe null
                            }
                        }
                    }
                }
            }

            it("should set elements correctly - test 2") {
                matrix[0, 0] = 1
                matrix[0, 0] = 2

                val element = matrix[0, 0]
                val elementToIndexMap = matrix.elementToIndexMap
                val set1 = elementToIndexMap[1]
                val set2 = elementToIndexMap[2]

                element shouldBe 2
                elementToIndexMap.size shouldBe 1
                elementToIndexMap[2] shouldNotBe null
                set1 shouldBe null
                set2 shouldNotBe null
                set2?.shouldContain(0 pairTo 0)
            }

            it("should set elements correctly - test 3") {
                matrix[0, 0] = 1
                matrix[0, 0] = null

                val element = matrix[0, 0]
                val elementToIndexMap = matrix.elementToIndexMap
                val set1 = elementToIndexMap[1]

                element shouldBe null
                elementToIndexMap.size shouldBe 0
                set1 shouldBe null
            }

            it("should set elements correctly - test 4") {
                val oldVal1 = matrix.set(0, 0, 1)
                val oldVal2 = matrix.set(0, 0, 2)

                oldVal1 shouldBe null
                oldVal2 shouldBe 1
            }

            it("should get indexes correctly") {
                matrix[0, 0] = 1
                matrix[1, 0] = 1
                matrix[1, 1] = 2
                matrix[2, 2] = 3

                val indexes1 = matrix.getIndexes(1)
                val indexes2 = matrix.getIndexes(2)
                val indexes3 = matrix.getIndexes(3)

                indexes1 shouldContainAll setOf(0 pairTo 0, 1 pairTo 0)
                indexes2 shouldContainAll setOf(1 pairTo 1)
                indexes3 shouldContainAll emptySet()
            }

            it("should check if an element exists correctly") {
                matrix[0, 0] = 1
                matrix[1, 1] = 2

                matrix.contains(1) shouldBe true
                matrix.contains(2) shouldBe true
                matrix.contains(3) shouldBe false
            }

            it("should check if all elements in a collection exist correctly") {
                matrix[0, 0] = 1
                matrix[1, 1] = 2

                matrix.containsAll(listOf(1, 2)) shouldBe true
                matrix.containsAll(listOf(1, 2, 3)) shouldBe false
            }

            it("should clear the matrix correctly") {
                matrix[0, 0] = 1
                matrix[1, 1] = 2

                matrix.clear()
                matrix.size shouldBe 0
                matrix.contains(1) shouldBe false
                matrix.contains(2) shouldBe false
            }

            it("should add elements correctly") {
                matrix.add(1) shouldBe true
                matrix[0, 0] shouldBe 1
                matrix.size shouldBe 1
            }

            it("should return false when adding elements if the matrix is full") {
                matrix.add(1)
                matrix.add(2)
                matrix.add(3)
                matrix.add(4)
                matrix.add(5)
                matrix.add(6)
                matrix.add(7)
                matrix.add(8)
                matrix.add(9)
                matrix.add(10)
                matrix.add(11)
                matrix.add(12)

                matrix.add(13) shouldBe false
                matrix.size shouldBe 12
            }

            it("should check if the matrix is empty correctly") {
                matrix.isEmpty() shouldBe true
                matrix.add(1)
                matrix.isEmpty() shouldBe false
            }

            it("should iterate through the matrix correctly") {
                matrix[0, 0] = 1
                matrix[1, 0] = 2
                matrix[2, 0] = 3

                val elements = mutableListOf<Int?>()
                for (element in matrix) {
                    elements.add(element)
                }
                elements shouldContainAll listOf(1, 2, 3)
            }

            it("should retain elements correctly") {
                matrix[0, 0] = 1
                matrix[1, 1] = 2
                matrix[2, 2] = 3

                matrix.retainAll(listOf(2, 3)) shouldBe true
                matrix.size shouldBe 2
                matrix.contains(1) shouldBe false
                matrix.contains(2) shouldBe true
                matrix.contains(3) shouldBe true
            }

            it("should remove elements correctly - test 1") {
                matrix[0, 0] = 1
                matrix[1, 1] = 2
                matrix[2, 2] = 3

                matrix.remove(2) shouldBe true
                matrix.size shouldBe 2
                matrix.contains(1) shouldBe true
                matrix.contains(2) shouldBe false
                matrix.contains(3) shouldBe true
            }

            it("should remove elements correctly - test 2") {
                matrix[0, 0] = 1
                matrix[1, 1] = 2
                matrix[2, 2] = 3

                matrix.remove(4) shouldBe false
                matrix.size shouldBe 3
            }

            it("should remove all elements correctly") {
                matrix[0, 0] = 1
                matrix[1, 1] = 2
                matrix[2, 2] = 3

                matrix.removeAll(listOf(1, 2, 3)) shouldBe true
                matrix.isEmpty() shouldBe true
            }

            it("should calculate hashCode correctly") {
                val matrix1 = Matrix<Int>(3, 3)
                matrix1[0, 0] = 1
                matrix1[1, 1] = 2
                matrix1[2, 2] = 3

                val matrix2 = Matrix<Int>(3, 3)
                matrix2[0, 0] = 1
                matrix2[1, 1] = 2
                matrix2[2, 2] = 3

                matrix1.hashCode() shouldBe matrix2.hashCode()
            }

            it("should check for equality correctly") {
                val matrix1 = Matrix<Int>(3, 3)
                matrix1[0, 0] = 1
                matrix1[1, 1] = 2
                matrix1[2, 2] = 3

                val matrix2 = Matrix<Int>(3, 3)
                matrix2[0, 0] = 1
                matrix2[1, 1] = 2
                matrix2[2, 2] = 3

                matrix1 shouldBe matrix2
            }

            it("should convert to string correctly") {
                matrix[0, 0] = 1
                matrix[1, 1] = 2
                matrix[2, 2] = 3

                matrix.toString() shouldBe "[[null, null, 3, null], [null, 2, null, null], [1, null, null, null]]"
            }
        }
    })
