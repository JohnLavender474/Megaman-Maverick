package com.mega.game.engine.world.container

import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.body.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class SimpleGridWorldContainerTest : DescribeSpec({
    describe("SimpleGridWorldContainer") {

        val ppm = 10
        lateinit var grid: SimpleGridWorldContainer
        val outBodies1 = MutableOrderedSet<IBody>()
        val outBodies2 = MutableOrderedSet<IBody>()
        val outFixtures = MutableOrderedSet<IFixture>()

        beforeEach {
            grid = SimpleGridWorldContainer(ppm)
            outBodies1.clear()
            outBodies2.clear()
            outFixtures.clear()
        }

        it("should add bodies to the correct cells") {
            // if
            grid.adjustForExactGridMatch = false
            val width = 10
            val height = 10

            val bodies = gdxArrayOf(
                Body(BodyType.DYNAMIC, 0f, 0f, 10f, 10f),
                Body(BodyType.DYNAMIC, 42f, 42f, 15f, 15f),
                Body(BodyType.DYNAMIC, 92f, 92f, 5f, 5f)
            )

            // when
            bodies.forEach { grid.addBody(it) }

            // then
            for (x in 0..width) for (y in 0..height) {
                grid.getBodies(x, y, outBodies1)

                when {
                    x in 0..1 && y in 0..1 -> {
                        outBodies1.size shouldBe 1
                        outBodies1 shouldContain bodies[0]
                    }

                    x in 4..5 && y in 4..5 -> {
                        outBodies1.size shouldBe 1
                        outBodies1 shouldContain bodies[1]
                    }

                    x == 9 && y == 9 -> {
                        outBodies1.size shouldBe 1
                        outBodies1 shouldContain bodies[2]
                    }

                    else -> outBodies1.size shouldBe 0
                }

                outBodies1.clear()
            }
        }

        it("should add fixtures to the correct cells") {
            // given
            grid.adjustForExactGridMatch = false
            val width = 10
            val height = 10

            val body = mockk<Body>()
            val fixtures = gdxArrayOf(
                Fixture(body, "Fixture1", GameRectangle(0f, 0f, 10f, 10f), attachedToBody = false),
                Fixture(body, "Fixture2", GameRectangle(42f, 42f, 15f, 15f), attachedToBody = false),
                Fixture(body, "Fixture3", GameRectangle(92f, 92f, 5f, 5f), attachedToBody = false)
            )

            // when
            fixtures.forEach { grid.addFixture(it) }

            // then
            for (x in 0..width) for (y in 0..height) {
                grid.getFixtures(x, y, outFixtures)

                when {
                    x in 0..1 && y in 0..1 -> {
                        outFixtures.size shouldBe 1
                        outFixtures shouldContain fixtures[0]
                    }

                    x in 4..5 && y in 4..5 -> {
                        outFixtures.size shouldBe 1
                        outFixtures shouldContain fixtures[1]
                    }

                    x == 9 && y == 9 -> {
                        outFixtures.size shouldBe 1
                        outFixtures shouldContain fixtures[2]
                    }

                    else -> outFixtures.size shouldBe 0
                }

                outFixtures.clear()
            }
        }

        it("should retrieve bodies in the specified area") {
            // given
            val bodies = gdxArrayOf(
                Body(BodyType.DYNAMIC, 10f, 10f, 20f, 20f),
                Body(BodyType.DYNAMIC, 40f, 40f, 10f, 10f),
                Body(BodyType.DYNAMIC, 80f, 80f, 5f, 5f)
            )

            bodies.forEach { grid.addBody(it) }

            val minX = 0
            val minY = 0
            val maxX = 3
            val maxY = 3

            // when
            grid.getBodies(minX, minY, maxX, maxY, outBodies1)
            println(outBodies1)

            // then
            outBodies1 shouldContain bodies[0]
            outBodies1 shouldNotContain bodies[1]
            outBodies1 shouldNotContain bodies[2]
        }

        it("should retrieve fixtures in the specified area") {
            // given
            val body = mockk<Body>()
            val fixtures = gdxArrayOf(
                Fixture(body, "Fixture1", GameRectangle(10f, 10f, 20f, 20f), attachedToBody = false),
                Fixture(body, "Fixture2", GameRectangle(40f, 40f, 10f, 10f), attachedToBody = false),
                Fixture(body, "Fixture3", GameRectangle(80f, 80f, 5f, 5f), attachedToBody = false)
            )

            fixtures.forEach { grid.addFixture(it) }

            val minX = 0
            val minY = 0
            val maxX = 3
            val maxY = 3

            // when
            grid.getFixtures(minX, minY, maxX, maxY, outFixtures)

            // then
            outFixtures shouldContain fixtures[0]
            outFixtures shouldNotContain fixtures[1]
            outFixtures shouldNotContain fixtures[2]
        }

        it("should clear the grid") {
            // given
            val width = 100
            val height = 100

            val bodies = gdxArrayOf(
                Body(BodyType.DYNAMIC, 10f, 10f, 20f, 20f),
                Body(BodyType.DYNAMIC, 40f, 40f, 10f, 10f),
                Body(BodyType.DYNAMIC, 80f, 80f, 5f, 5f)
            )

            bodies.forEach { grid.addBody(it) }

            // when
            grid.clear()

            // then
            for (x in 0 until width) {
                for (y in 0 until height) {
                    grid.getBodies(x, y, outBodies1)
                    outBodies1.count() shouldBe 0
                }
            }
        }

        it("should add bodies to the correct cells with subtractOnExactInteger=false") {
            // If
            grid.adjustForExactGridMatch = false
            val bodies = gdxArrayOf(
                Body(BodyType.DYNAMIC, 10f, 10f, 20f, 20f),
                Body(BodyType.DYNAMIC, 30f, 30f, 10f, 10f)
            )

            // When
            bodies.forEach { grid.addBody(it) }

            // Then
            grid.getBodies(1, 1, outBodies1)
            grid.getBodies(3, 3, outBodies2)
            outBodies1.size shouldBe 1
            outBodies2.size shouldBe 2
            outBodies1 shouldContain bodies[0]
            outBodies2 shouldContain bodies[1]
        }

        it("should subtract on exact integers when subtractOnExactInteger=true - 1") {
            // Given
            grid.adjustForExactGridMatch = true

            val body = Body(BodyType.DYNAMIC, 0f, 0f, 10f, 10f)

            // When
            grid.addBody(body)

            // Then
            // Without subtraction, it would have occupied (1,1). With subtraction, it will stay at (0,0)
            grid.getBodies(0, 0, outBodies1)
            outBodies1.size shouldBe 1
            outBodies1 shouldContain body

            outBodies1.clear()

            grid.getBodies(1, 0, outBodies1)
            outBodies1.size shouldBe 0

            outBodies1.clear()

            grid.getBodies(0, 1, outBodies1)
            outBodies1.size shouldBe 0

            outBodies1.clear()

            grid.getBodies(1, 1, outBodies1)
            outBodies1.size shouldBe 0
        }

        it("should add bodies to the correct cells with subtractOnExactInteger=true- 2") {
            // If
            grid.adjustForExactGridMatch = true
            val bodies = gdxArrayOf(
                Body(BodyType.DYNAMIC, 10f, 10f, 20f, 20f),
                Body(BodyType.DYNAMIC, 30f, 30f, 10f, 10f)
            )

            // When
            bodies.forEach { grid.addBody(it) }

            // Then
            grid.getBodies(1, 1, outBodies1)
            grid.getBodies(3, 3, outBodies2)
            outBodies1.size shouldBe 1
            outBodies2.size shouldBe 1
            outBodies1 shouldContain bodies[0]
            outBodies2 shouldContain bodies[1]
        }

        it("should handle negative values with subtractOnExactInteger=true") {
            // Given
            grid.adjustForExactGridMatch = true
            val body = Body(BodyType.DYNAMIC, -10f, -10f, 10f, 10f)

            // When
            grid.addBody(body)

            // Then
            grid.getBodies(-1, -1, outBodies1)
            outBodies1.size shouldBe 1
            outBodies1 shouldContain body

            outBodies1.clear()

            grid.getBodies(0, 0, outBodies1)
            outBodies1.size shouldBe 0

            outBodies1.clear()

            grid.getBodies(0, 1, outBodies1)
            outBodies1.size shouldBe 0

            outBodies1.clear()

            grid.getBodies(1, 0, outBodies1)
            outBodies1.size shouldBe 0
        }

        it("should not adjust when subtractOnExactInteger=false") {
            // Given
            grid = SimpleGridWorldContainer(ppm, adjustForExactGridMatch = false)

            val body = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)

            // When
            grid.addBody(body)

            // Then
            // It should occupy the (1,1) cell, since we are not subtracting from exact integers
            grid.getBodies(1, 1, outBodies1)
            outBodies1.size shouldBe 1
            outBodies1 shouldContain body
        }

        it("should retrieve bodies in the specified area and apply subtractOnExactInteger=true") {
            // Given
            grid = SimpleGridWorldContainer(ppm, adjustForExactGridMatch = true)

            val bodies = gdxArrayOf(
                Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f),
                Body(BodyType.DYNAMIC, 30f, 30f, 10f, 10f)
            )

            bodies.forEach { grid.addBody(it) }

            // When
            grid.getBodies(0, 0, 1, 1, outBodies1)

            // Then
            outBodies1.size shouldBe 1
            outBodies1 shouldContain bodies[0]
            outBodies1 shouldNotContain bodies[1]
        }

        it("should retrieve fixtures in the specified area and apply subtractOnExactInteger=false") {
            // Given
            val body = mockk<Body>()
            val fixture = Fixture(body, "Fixture1", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)

            // When
            grid.addFixture(fixture)

            // Then
            grid.getFixtures(1, 1, outFixtures)
            outFixtures.size shouldBe 1
            outFixtures shouldContain fixture
        }

        it("should clear the grid") {
            // Given
            val bodies = gdxArrayOf(
                Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f),
                Body(BodyType.DYNAMIC, 30f, 30f, 10f, 10f)
            )

            bodies.forEach { grid.addBody(it) }

            // When
            grid.clear()

            // Then
            for (x in 0 until 10) for (y in 0 until 10) {
                grid.getBodies(x, y, outBodies1)
                outBodies1.count() shouldBe 0
            }
        }

        it("forEachBody single-cell should invoke action for matching bodies") {
            // Given
            val body1 = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)
            val body2 = Body(BodyType.DYNAMIC, 30f, 30f, 10f, 10f)
            grid.addBody(body1)
            grid.addBody(body2)

            val visited = mutableListOf<IBody>()

            // When
            grid.forEachBody(1, 1, { body, _ -> visited.add(body) })

            // Then
            visited shouldContain body1
            visited shouldNotContain body2
        }

        it("forEachBody range should invoke action for all bodies in range") {
            // Given
            val body1 = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)
            val body2 = Body(BodyType.DYNAMIC, 30f, 30f, 10f, 10f)
            grid.addBody(body1)
            grid.addBody(body2)

            val visited = mutableListOf<IBody>()

            // When
            grid.forEachBody(1, 1, 3, 3, { body, _ -> visited.add(body) })

            // Then
            visited shouldContain body1
            visited shouldContain body2
        }

        it("forEachBody action can filter inline") {
            // Given
            val body1 = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)
            val body2 = Body(BodyType.STATIC, 10f, 10f, 10f, 10f)
            grid.addBody(body1)
            grid.addBody(body2)

            val visited = mutableListOf<IBody>()

            // When - action itself skips non-DYNAMIC bodies
            grid.forEachBody(1, 1) { body, _ -> if (body.type == BodyType.DYNAMIC) visited.add(body); true }

            // Then
            visited shouldContain body1
            visited shouldNotContain body2
        }

        it("forEachBody stops early when action returns false") {
            // Given
            val bodies = (1..5).map { Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f) }
            bodies.forEach { grid.addBody(it) }

            val visited = mutableListOf<IBody>()

            // When - stop after first body
            val result = grid.forEachBody(1, 1) { body, _ ->
                visited.add(body)
                false
            }

            // Then
            result shouldBe false
            visited.size shouldBe 1
        }

        it("forEachFixture single-cell should invoke action for matching fixtures") {
            // Given
            val body = mockk<Body>()
            val fixture1 = Fixture(body, "F1", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)
            val fixture2 = Fixture(body, "F2", GameRectangle(30f, 30f, 10f, 10f), attachedToBody = false)
            grid.addFixture(fixture1)
            grid.addFixture(fixture2)

            val visited = mutableListOf<IFixture>()

            // When
            grid.forEachFixture(1, 1, { fixture, _ -> visited.add(fixture) })

            // Then
            visited shouldContain fixture1
            visited shouldNotContain fixture2
        }

        it("forEachFixture range should invoke action for all fixtures in range") {
            // Given
            val body = mockk<Body>()
            val fixture1 = Fixture(body, "F1", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)
            val fixture2 = Fixture(body, "F2", GameRectangle(30f, 30f, 10f, 10f), attachedToBody = false)
            grid.addFixture(fixture1)
            grid.addFixture(fixture2)

            val visited = mutableListOf<IFixture>()

            // When
            grid.forEachFixture(1, 1, 3, 3, { fixture, _ -> visited.add(fixture) })

            // Then
            visited shouldContain fixture1
            visited shouldContain fixture2
        }

        it("forEachFixture action can filter inline") {
            // Given
            val body = mockk<Body>()
            val fixture1 = Fixture(body, "keep", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)
            val fixture2 = Fixture(body, "skip", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)
            grid.addFixture(fixture1)
            grid.addFixture(fixture2)

            val visited = mutableListOf<IFixture>()

            // When
            grid.forEachFixture(1, 1) { fixture, _ -> if (fixture.getType() == "keep") visited.add(fixture); true }

            // Then
            visited shouldContain fixture1
            visited shouldNotContain fixture2
        }

        it("copy should be independent of the original once the original is cleared and rebuilt") {
            // given
            val body = mockk<Body>()
            val original = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)
            val originalFixture = Fixture(body, "F1", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)
            grid.addBody(original)
            grid.addFixture(originalFixture)

            // when - snapshot, then clear and repopulate the original with different contents
            val snapshot = grid.copy()

            grid.clear()

            val replacement = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)
            val replacementFixture = Fixture(body, "F2", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)
            grid.addBody(replacement)
            grid.addFixture(replacementFixture)

            // then - the snapshot still holds what it held when it was taken
            snapshot.getBodies(1, 1, outBodies1)
            outBodies1.size shouldBe 1
            outBodies1 shouldContain original
            outBodies1 shouldNotContain replacement

            snapshot.getFixtures(1, 1, outFixtures)
            outFixtures.size shouldBe 1
            outFixtures shouldContain originalFixture
            outFixtures shouldNotContain replacementFixture

            // and the original holds only the replacements
            outBodies2.clear()
            grid.getBodies(1, 1, outBodies2)
            outBodies2.size shouldBe 1
            outBodies2 shouldContain replacement
        }

        it("clear should empty reused cells rather than leaving stale contents behind") {
            // given - a cell that will be reused across rebuilds, as the world system does each cycle
            val stale = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)
            grid.addBody(stale)

            // when - clear, then repopulate the same cell with a *different* body. using a different
            // body matters: re-adding the same one would be collapsed by the cell's dedup and so would
            // pass even if `clear` had done nothing
            grid.clear()

            val fresh = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)
            grid.addBody(fresh)

            // then
            grid.getBodies(1, 1, outBodies1)
            outBodies1.size shouldBe 1
            outBodies1 shouldContain fresh
            outBodies1 shouldNotContain stale

            val visited = mutableListOf<IBody>()
            grid.forEachBody(1, 1) { b, _ -> visited.add(b); true }
            visited.size shouldBe 1
            visited shouldContain fresh
        }

        it("clear should drop cells rather than retain them, so occupancy tracks live contents") {
            // given - a body far from the origin, as the world system sees once the camera has moved on
            grid.addBody(Body(BodyType.DYNAMIC, 1000f, 1000f, 10f, 10f))
            grid.getOccupiedBodyCellCount() shouldBe 4

            // when - cleared and rebuilt somewhere else entirely, cycle after cycle
            repeat(20) { i ->
                grid.clear()
                grid.addBody(Body(BodyType.DYNAMIC, i * 100f, i * 100f, 10f, 10f))
            }

            // then - occupancy reflects only what is currently in the grid. retaining emptied entries
            // instead would let this grow without bound as the player travels, which is what made
            // `clear` and `copy` get slower the further into a level you were
            grid.getOccupiedBodyCellCount() shouldBe 4
        }

        it("clear should recycle cells so rebuilding does not reallocate them") {
            // given - a populated grid whose cells are about to be released
            grid.addBody(Body(BodyType.DYNAMIC, 0f, 0f, 10f, 10f))
            grid.addFixture(Fixture(mockk<Body>(), "F1", GameRectangle(0f, 0f, 10f, 10f), attachedToBody = false))

            val bodyCells = grid.getOccupiedBodyCellCount()
            val fixtureCells = grid.getOccupiedFixtureCellCount()
            grid.getPooledBodyCellCount() shouldBe 0
            grid.getPooledFixtureCellCount() shouldBe 0

            // when
            grid.clear()

            // then - every cell is parked for reuse rather than left to be collected
            grid.getPooledBodyCellCount() shouldBe bodyCells
            grid.getPooledFixtureCellCount() shouldBe fixtureCells

            // and - refilling draws them back out of the pool instead of allocating
            grid.addBody(Body(BodyType.DYNAMIC, 0f, 0f, 10f, 10f))
            grid.getPooledBodyCellCount() shouldBe 0
            grid.getOccupiedBodyCellCount() shouldBe bodyCells
        }

        it("cells should collapse duplicate adds") {
            // given
            val body = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)
            val fixture = Fixture(mockk<Body>(), "F1", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)

            // when - added repeatedly without an intervening clear
            repeat(3) {
                grid.addBody(body)
                grid.addFixture(fixture)
            }

            // then - visited once, not once per add
            val visitedBodies = mutableListOf<IBody>()
            grid.forEachBody(1, 1) { b, _ -> visitedBodies.add(b); true }
            visitedBodies.size shouldBe 1

            val visitedFixtures = mutableListOf<IFixture>()
            grid.forEachFixture(1, 1) { f, _ -> visitedFixtures.add(f); true }
            visitedFixtures.size shouldBe 1
        }

        it("forEachFixture stops early when action returns false") {
            // Given
            val body = mockk<Body>()
            val fixtures = (1..5).map { Fixture(body, "F$it", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false) }
            fixtures.forEach { grid.addFixture(it) }

            val visited = mutableListOf<IFixture>()

            // When - stop after first fixture
            val result = grid.forEachFixture(1, 1) { fixture, _ ->
                visited.add(fixture)
                false
            }

            // Then
            result shouldBe false
            visited.size shouldBe 1
        }
    }
})
