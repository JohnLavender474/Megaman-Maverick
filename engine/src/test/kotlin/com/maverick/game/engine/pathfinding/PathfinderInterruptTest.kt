package com.maverick.game.engine.pathfinding

import com.maverick.game.engine.common.objects.IntPair
import com.maverick.game.engine.pathfinding.heuristics.EuclideanHeuristic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class PathfinderInterruptTest : DescribeSpec({

    fun pathfinder() = Pathfinder(
        startCoordinate = IntPair(0, 0),
        targetCoordinate = IntPair(5, 0),
        filter = { x, y -> x in 0..10 && y in 0..10 },
        allowDiagonal = false,
        heuristic = EuclideanHeuristic()
    )

    describe("Pathfinder") {

        it("finds the path when its thread is not interrupted") {
            pathfinder().call().path.shouldNotBeNull()
        }

        it("stops with no path when its thread is interrupted, as after a timeout cancel") {
            Thread.currentThread().interrupt()
            try {
                pathfinder().call().path.shouldBeNull()
            } finally {
                Thread.interrupted() shouldBe true
            }
        }
    }
})
