package com.mega.game.engine.pathfinding

import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.pathfinding.heuristics.ManhattanHeuristic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PathfinderTest : DescribeSpec({

    val heuristic = ManhattanHeuristic()

    fun createPathfinderParams(matrix: Matrix<String>, allowDiagonal: Boolean): PathfinderParams {
        lateinit var startCoordinate: IntPair
        lateinit var targetCoordinate: IntPair
        val nodesToFilter = HashSet<IntPair>()

        matrix.forEach { x, y, value ->
            when (value) {
                "S" -> {
                    startCoordinate = IntPair(x, y)
                }

                "T" -> {
                    targetCoordinate = IntPair(x, y)
                }

                "X" -> {
                    startCoordinate = IntPair(x, y)
                    targetCoordinate = IntPair(x, y)
                }

                "!" -> nodesToFilter.add(x pairTo y)
            }
        }

        val filter: (IntPair) -> Boolean =
            { coordinate -> !matrix.isOutOfBounds(coordinate.x, coordinate.y) && !nodesToFilter.contains(coordinate) }

        return PathfinderParams(
            startCoordinateSupplier = { startCoordinate },
            targetCoordinateSupplier = { targetCoordinate },
            allowDiagonal = { allowDiagonal },
            filter = filter
        )
    }

    it("createPathfinderParams") {
        // If
        val array = gdxArrayOf(
            gdxArrayOf("S", "0", "0"),
            gdxArrayOf("0", "0", "T")
        )
        val world = Matrix(array)

        // when
        val params = createPathfinderParams(world, true)

        // then
        world.forEach { x, y, element ->
            when (element) {
                "S" -> {
                    x shouldBe 0
                    y shouldBe 1
                }

                "T" -> {
                    x shouldBe 2
                    y shouldBe 0
                }

                else -> {}
            }
        }

        params.startCoordinateSupplier() shouldBe IntPair(0, 1)
        params.targetCoordinateSupplier() shouldBe IntPair(2, 0)
    }

    describe("Pathfinder") {
        it("should find the fastest path - test 1") {
            // If
            val world = Matrix(
                gdxArrayOf(
                    gdxArrayOf("S", "0", "0", "0"),
                    gdxArrayOf("0", "0", "0", "0"),
                    gdxArrayOf("0", "0", "0", "0"),
                    gdxArrayOf("0", "0", "0", "T")
                )
            )

            val params = createPathfinderParams(world, true)

            params.startCoordinateSupplier() shouldBe IntPair(0, 3)
            params.targetCoordinateSupplier() shouldBe IntPair(3, 0)

            val pathfinder = Pathfinder(
                params.startCoordinateSupplier(),
                params.targetCoordinateSupplier(),
                params.filter,
                params.allowDiagonal(),
                heuristic
            )

            // when
            val result = pathfinder.call()
            val targetReached = result.targetReached
            targetReached shouldBe false
            val path = result.path

            // then
            path shouldNotBe null
            path!!.size shouldBe 4

            println(path)

            path[0] shouldBe IntPair(0, 3)
            path[1] shouldBe IntPair(1, 2)
            path[2] shouldBe IntPair(2, 1)
            path[3] shouldBe IntPair(3, 0)
        }

        it("should find the fastest path - test 2") {
            // If
            val world = Matrix(
                gdxArrayOf(
                    gdxArrayOf("S", "0", "!", "T"),
                    gdxArrayOf("!", "0", "!", "0"),
                    gdxArrayOf("0", "0", "!", "0"),
                    gdxArrayOf("0", "0", "0", "0")
                )
            )

            val params = createPathfinderParams(world, true)
            val pathfinder = Pathfinder(
                params.startCoordinateSupplier(),
                params.targetCoordinateSupplier(),
                params.filter,
                params.allowDiagonal(),
                heuristic
            )

            // when
            val result = pathfinder.call()
            val path = result.path

            println(path)

            // then
            path shouldNotBe null
            path!!.size shouldBe 7

            path[0] shouldBe IntPair(0, 3)
            path[1] shouldBe IntPair(1, 2)
            path[2] shouldBe IntPair(1, 1)
            path[3] shouldBe IntPair(2, 0)
            path[4] shouldBe IntPair(3, 1)
            path[5] shouldBe IntPair(3, 2)
            path[6] shouldBe IntPair(3, 3)
        }

        it("should find the fastest path - test 3") {
            // If
            val world = Matrix(
                gdxArrayOf(
                    gdxArrayOf("S", "0", "!", "T"),
                    gdxArrayOf("!", "0", "!", "0"),
                    gdxArrayOf("0", "0", "!", "0"),
                    gdxArrayOf("0", "0", "0", "0")
                )
            )

            val params = createPathfinderParams(world, false)
            val pathfinder = Pathfinder(
                params.startCoordinateSupplier(),
                params.targetCoordinateSupplier(),
                params.filter,
                params.allowDiagonal(),
                heuristic
            )

            // when
            val result = pathfinder.call()
            val path = result.path

            // then
            path shouldNotBe null
            path!!.size shouldBe 10

            println(path)

            path[0] shouldBe IntPair(0, 3)
            path[1] shouldBe IntPair(1, 3)
            path[2] shouldBe IntPair(1, 2)
            path[3] shouldBe IntPair(1, 1)
            path[4] shouldBe IntPair(1, 0)
            path[5] shouldBe IntPair(2, 0)
            path[6] shouldBe IntPair(3, 0)
            path[7] shouldBe IntPair(3, 1)
            path[8] shouldBe IntPair(3, 2)
            path[9] shouldBe IntPair(3, 3)
        }

        it("should not find a path if blocked") {
            // If
            val world = Matrix(
                gdxArrayOf(
                    gdxArrayOf("S", "0", "!", "T"),
                    gdxArrayOf("!", "0", "!", "0"),
                    gdxArrayOf("0", "0", "!", "0"),
                    gdxArrayOf("0", "0", "!", "0")
                )
            )

            val params = createPathfinderParams(world, true)
            val pathfinder = Pathfinder(
                params.startCoordinateSupplier(),
                params.targetCoordinateSupplier(),
                params.filter,
                params.allowDiagonal(),
                heuristic
            )

            // when
            val result = pathfinder.call()
            val path = result.path

            // then
            path shouldBe null
        }

        it("should detect if start equals target") {
            // If
            val world = Matrix(
                gdxArrayOf(
                    gdxArrayOf("X", "0", "0", "0"),
                    gdxArrayOf("0", "0", "0", "0"),
                    gdxArrayOf("0", "0", "0", "0"),
                    gdxArrayOf("0", "0", "0", "0")
                )
            )

            val params = createPathfinderParams(world, true)
            val pathfinder = Pathfinder(
                params.startCoordinateSupplier(),
                params.targetCoordinateSupplier(),
                params.filter,
                params.allowDiagonal(),
                heuristic
            )

            // when
            val result = pathfinder.call()

            // then
            result.path shouldBe null
            result.targetReached shouldBe true
        }

        describe("Pathfinder with maxIterations and maxDistance") {

            it("should return the best path when maxIterations is reached") {
                // Given
                val world = Matrix(
                    gdxArrayOf(
                        gdxArrayOf("S", "0", "0", "0"),
                        gdxArrayOf("!", "!", "!", "0"),
                        gdxArrayOf("0", "0", "0", "T")
                    )
                )

                val params = createPathfinderParams(world, allowDiagonal = true)
                val pathfinder = Pathfinder(
                    startCoordinate = params.startCoordinateSupplier(),
                    targetCoordinate = params.targetCoordinateSupplier(),
                    filter = params.filter,
                    allowDiagonal = params.allowDiagonal(),
                    heuristic = ManhattanHeuristic(),
                    maxIterations = 3,
                    returnBestPathOnFailure = true
                )

                // When
                val result = pathfinder.call()

                // Then
                result.path shouldNotBe null
                result.path!!.size shouldBe 3
            }

            it("should return null when maxIterations is reached and returnBestPathOnFailure=false") {
                // Given
                val world = Matrix(
                    gdxArrayOf(
                        gdxArrayOf("S", "0", "0", "0"),
                        gdxArrayOf("!", "!", "!", "0"),
                        gdxArrayOf("0", "0", "0", "T")
                    )
                )

                val params = createPathfinderParams(world, allowDiagonal = true)
                val pathfinder = Pathfinder(
                    startCoordinate = params.startCoordinateSupplier(),
                    targetCoordinate = params.targetCoordinateSupplier(),
                    filter = params.filter,
                    allowDiagonal = params.allowDiagonal(),
                    heuristic = ManhattanHeuristic(),
                    maxIterations = 3,
                    returnBestPathOnFailure = false
                )

                // When
                val result = pathfinder.call()

                // Then
                result.path shouldBe null
            }

            it("should return the best path when maxDistance is reached") {
                // Given
                val world = Matrix(
                    gdxArrayOf(
                        gdxArrayOf("S", "0", "0", "0"),
                        gdxArrayOf("0", "0", "0", "0"),
                        gdxArrayOf("0", "0", "0", "T")
                    )
                )

                val params = createPathfinderParams(world, allowDiagonal = false)
                val pathfinder = Pathfinder(
                    startCoordinate = params.startCoordinateSupplier(),
                    targetCoordinate = params.targetCoordinateSupplier(),
                    filter = params.filter,
                    allowDiagonal = params.allowDiagonal(),
                    heuristic = ManhattanHeuristic(),
                    maxDistance = 2,
                    returnBestPathOnFailure = true
                )

                // When
                val result = pathfinder.call()
                println(result)

                // Then
                result.path shouldNotBe null
                result.path!!.size shouldBe 3
            }

            it("should return null when maxDistance is reached and returnBestPathOnFailure=false") {
                // Given
                val world = Matrix(
                    gdxArrayOf(
                        gdxArrayOf("S", "0", "0", "0"),
                        gdxArrayOf("0", "0", "0", "0"),
                        gdxArrayOf("0", "0", "0", "T")
                    )
                )

                val params = createPathfinderParams(world, allowDiagonal = true)
                val pathfinder = Pathfinder(
                    startCoordinate = params.startCoordinateSupplier(),
                    targetCoordinate = params.targetCoordinateSupplier(),
                    filter = params.filter,
                    allowDiagonal = params.allowDiagonal(),
                    heuristic = ManhattanHeuristic(),
                    maxDistance = 2,
                    returnBestPathOnFailure = false
                )

                // When
                val result = pathfinder.call()

                // Then
                result.path shouldBe null
            }

            it("should find the full path when no limits are imposed") {
                // Given
                val world = Matrix(
                    gdxArrayOf(
                        gdxArrayOf("S", "0", "0", "0"),
                        gdxArrayOf("!", "!", "!", "0"),
                        gdxArrayOf("0", "0", "0", "T")
                    )
                )

                val params = createPathfinderParams(world, allowDiagonal = true)
                val pathfinder = Pathfinder(
                    startCoordinate = params.startCoordinateSupplier(),
                    targetCoordinate = params.targetCoordinateSupplier(),
                    filter = params.filter,
                    allowDiagonal = params.allowDiagonal(),
                    heuristic = ManhattanHeuristic(),
                    maxIterations = 1000,
                    maxDistance = 1000,
                    returnBestPathOnFailure = true
                )

                // When
                val result = pathfinder.call()

                // Then
                result.path shouldNotBe null
                result.path!!.size shouldBe 5
            }

            it("should find the full path when not allowed to move diagonally") {
                // Given
                val world = Matrix(
                    gdxArrayOf(
                        gdxArrayOf("S", "0", "0", "0"),
                        gdxArrayOf("!", "!", "!", "0"),
                        gdxArrayOf("0", "0", "0", "T")
                    )
                )

                val params = createPathfinderParams(world, allowDiagonal = false)
                val pathfinder = Pathfinder(
                    startCoordinate = params.startCoordinateSupplier(),
                    targetCoordinate = params.targetCoordinateSupplier(),
                    filter = params.filter,
                    allowDiagonal = params.allowDiagonal(),
                    heuristic = ManhattanHeuristic(),
                    maxIterations = 1000,
                    maxDistance = 1000,
                    returnBestPathOnFailure = true
                )

                // When
                val result = pathfinder.call()

                // Then
                result.path shouldNotBe null
                result.path!!.size shouldBe 6
            }
        }
    }
})
