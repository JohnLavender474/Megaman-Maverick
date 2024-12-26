package com.mega.game.engine.world.container

import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class SimpleGridWorldContainerTest : DescribeSpec({
    describe("SimpleGridWorldContainer") {

        val ppm = 10
        lateinit var grid: SimpleGridWorldContainer

        beforeEach {
            grid = SimpleGridWorldContainer(ppm)
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
            for (x in 0..width) {
                for (y in 0..height) {
                    val cellBodies = grid.getBodies(x, y)

                    when {
                        x in 0..1 && y in 0..1 -> {
                            cellBodies.size shouldBe 1
                            cellBodies shouldContain bodies[0]
                        }

                        x in 4..5 && y in 4..5 -> {
                            cellBodies.size shouldBe 1
                            cellBodies shouldContain bodies[1]
                        }

                        x == 9 && y == 9 -> {
                            cellBodies.size shouldBe 1
                            cellBodies shouldContain bodies[2]
                        }

                        else -> {
                            cellBodies.size shouldBe 0
                        }
                    }
                }
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
            for (x in 0..width) {
                for (y in 0..height) {
                    val cellFixtures = grid.getFixtures(x, y)

                    when {
                        x in 0..1 && y in 0..1 -> {
                            cellFixtures.size shouldBe 1
                            cellFixtures shouldContain fixtures[0]
                        }

                        x in 4..5 && y in 4..5 -> {
                            cellFixtures.size shouldBe 1
                            cellFixtures shouldContain fixtures[1]
                        }

                        x == 9 && y == 9 -> {
                            cellFixtures.size shouldBe 1
                            cellFixtures shouldContain fixtures[2]
                        }

                        else -> {
                            cellFixtures.size shouldBe 0
                        }
                    }
                }
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
            val retrievedBodies = grid.getBodies(minX, minY, maxX, maxY)
            println(retrievedBodies)

            // then
            retrievedBodies shouldContain bodies[0]
            retrievedBodies shouldNotContain bodies[1]
            retrievedBodies shouldNotContain bodies[2]
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
            val retrievedFixtures = grid.getFixtures(minX, minY, maxX, maxY)

            // then
            retrievedFixtures shouldContain fixtures[0]
            retrievedFixtures shouldNotContain fixtures[1]
            retrievedFixtures shouldNotContain fixtures[2]
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
                    val cellBodies = grid.getBodies(x, y)
                    cellBodies.count() shouldBe 0
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
            val cellBodies1 = grid.getBodies(1, 1)
            val cellBodies2 = grid.getBodies(3, 3)
            cellBodies1.size shouldBe 1
            cellBodies2.size shouldBe 2
            cellBodies1 shouldContain bodies[0]
            cellBodies2 shouldContain bodies[1]
        }

        it("should subtract on exact integers when subtractOnExactInteger=true - 1") {
            // Given
            grid.adjustForExactGridMatch = true

            val body = Body(BodyType.DYNAMIC, 0f, 0f, 10f, 10f)

            // When
            grid.addBody(body)

            // Then
            // Without subtraction, it would have occupied (1,1). With subtraction, it will stay at (0,0)
            var cellBodies = grid.getBodies(0, 0)
            cellBodies.size shouldBe 1
            cellBodies shouldContain body

            cellBodies = grid.getBodies(1, 0)
            cellBodies.size shouldBe 0

            cellBodies = grid.getBodies(0, 1)
            cellBodies.size shouldBe 0

            cellBodies = grid.getBodies(1, 1)
            cellBodies.size shouldBe 0
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
            val cellBodies1 = grid.getBodies(1, 1)
            val cellBodies2 = grid.getBodies(3, 3)
            cellBodies1.size shouldBe 1
            cellBodies2.size shouldBe 1
            cellBodies1 shouldContain bodies[0]
            cellBodies2 shouldContain bodies[1]
        }

        it("should handle negative values with subtractOnExactInteger=true") {
            // Given
            grid.adjustForExactGridMatch = true
            val body = Body(BodyType.DYNAMIC, -10f, -10f, 10f, 10f)

            // When
            grid.addBody(body)

            // Then
            var cellBodies = grid.getBodies(-1, -1)
            cellBodies.size shouldBe 1
            cellBodies shouldContain body

            cellBodies = grid.getBodies(0, 0)
            cellBodies.size shouldBe 0

            cellBodies = grid.getBodies(0, 1)
            cellBodies.size shouldBe 0

            cellBodies = grid.getBodies(1, 0)
            cellBodies.size shouldBe 0
        }

        it("should not adjust when subtractOnExactInteger=false") {
            // Given
            grid = SimpleGridWorldContainer(ppm, adjustForExactGridMatch = false)

            val body = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)

            // When
            grid.addBody(body)

            // Then
            // It should occupy the (1,1) cell, since we are not subtracting from exact integers
            val cellBodies = grid.getBodies(1, 1)
            cellBodies.size shouldBe 1
            cellBodies shouldContain body
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
            val retrievedBodies = grid.getBodies(0, 0, 1, 1)

            // Then
            retrievedBodies.size shouldBe 1
            retrievedBodies shouldContain bodies[0]
            retrievedBodies shouldNotContain bodies[1]
        }

        it("should retrieve fixtures in the specified area and apply subtractOnExactInteger=false") {
            // Given
            val body = mockk<Body>()
            val fixture = Fixture(body, "Fixture1", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)

            // When
            grid.addFixture(fixture)

            // Then
            val retrievedFixtures = grid.getFixtures(1, 1)
            retrievedFixtures.size shouldBe 1
            retrievedFixtures shouldContain fixture
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
            for (x in 0 until 10) {
                for (y in 0 until 10) {
                    val cellBodies = grid.getBodies(x, y)
                    cellBodies.count() shouldBe 0
                }
            }
        }
    }
})
