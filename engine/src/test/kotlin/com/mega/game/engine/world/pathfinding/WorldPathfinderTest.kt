package com.mega.game.engine.world.pathfinding

import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.pathfinding.PathfinderResult
import com.mega.game.engine.pathfinding.heuristics.ManhattanHeuristic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class WorldPathfinderTest : DescribeSpec({

    val heuristic = ManhattanHeuristic()

    fun createWorldPathfinder(
        matrix: Matrix<String>,
        allowDiagonal: Boolean,
        allowOutOfWorldBounds: Boolean
    ): WorldPathfinder {
        lateinit var startCoordinate: IntPair
        lateinit var targetCoordinate: IntPair
        val nodesToFilter = HashSet<IntPair>()

        matrix.forEach { x, y, value ->
            when (value) {
                "X" -> {
                    startCoordinate = IntPair(x, y)
                    targetCoordinate = IntPair(x, y)
                }

                "S" -> {
                    startCoordinate = IntPair(x, y)
                }

                "T" -> {
                    targetCoordinate = IntPair(x, y)
                }

                "!" -> nodesToFilter.add(x pairTo y)
            }
        }

        val filter: (IntPair) -> Boolean =
            { coordinate -> !matrix.isOutOfBounds(coordinate.x, coordinate.y) && !nodesToFilter.contains(coordinate) }

        return WorldPathfinder(
            start = startCoordinate,
            target = targetCoordinate,
            worldWidth = matrix.columns,
            worldHeight = matrix.rows,
            allowDiagonal = allowDiagonal,
            allowOutOfWorldBounds = allowOutOfWorldBounds,
            filter = filter,
            heuristic = heuristic
        )
    }

    it("should find the fastest path with WorldPathfinder - test 1") {
        // If
        val world = Matrix(
            gdxArrayOf(
                gdxArrayOf("S", "0", "0", "0"),
                gdxArrayOf("0", "0", "0", "0"),
                gdxArrayOf("0", "0", "0", "0"),
                gdxArrayOf("0", "0", "0", "T")
            )
        )

        val pathfinder = createWorldPathfinder(world, allowDiagonal = true, allowOutOfWorldBounds = false)

        // When
        val result = pathfinder.call()
        val path = result.path

        // Then
        path shouldNotBe null
        path!!.size shouldBe 4

        path[0] shouldBe IntPair(0, 3)
        path[1] shouldBe IntPair(1, 2)
        path[2] shouldBe IntPair(2, 1)
        path[3] shouldBe IntPair(3, 0)
    }

    it("should avoid obstacles - test 2") {
        // If
        val world = Matrix(
            gdxArrayOf(
                gdxArrayOf("S", "0", "!", "T"),
                gdxArrayOf("!", "0", "!", "0"),
                gdxArrayOf("0", "0", "!", "0"),
                gdxArrayOf("0", "0", "0", "0")
            )
        )

        val pathfinder = createWorldPathfinder(world, allowDiagonal = true, allowOutOfWorldBounds = false)

        // When
        val result = pathfinder.call()
        val path = result.path

        // Then
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

    it("should handle non-diagonal paths - test 3") {
        // If
        val world = Matrix(
            gdxArrayOf(
                gdxArrayOf("S", "0", "!", "T"),
                gdxArrayOf("!", "0", "!", "0"),
                gdxArrayOf("0", "0", "!", "0"),
                gdxArrayOf("0", "0", "0", "0")
            )
        )

        val pathfinder = createWorldPathfinder(world, allowDiagonal = false, allowOutOfWorldBounds = false)

        // When
        val result = pathfinder.call()
        val path = result.path

        // Then
        path shouldNotBe null
        path!!.size shouldBe 10

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

        val pathfinder = createWorldPathfinder(world, allowDiagonal = true, allowOutOfWorldBounds = false)

        // When
        val result = pathfinder.call()
        val path = result.path

        // Then
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

        val pathfinder = createWorldPathfinder(world, allowDiagonal = true, allowOutOfWorldBounds = false)

        // When
        val result: PathfinderResult = pathfinder.call()

        // Then
        result.path shouldBe null
        result.targetReached shouldBe true
    }
})
