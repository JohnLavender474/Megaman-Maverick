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
    }
})
