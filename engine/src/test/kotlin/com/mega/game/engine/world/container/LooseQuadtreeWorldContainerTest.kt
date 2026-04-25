package com.mega.game.engine.world.container

import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.world.body.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.mockk

/**
 * Tests for [LooseQuadtreeWorldContainer].
 *
 * Because the loose quadtree is a candidate-set structure (it may return entities from
 * neighboring nodes whose loose bounds overlap the query region), tests verify two things:
 *   1. An entity IS present when querying a region that genuinely overlaps it.
 *   2. An entity is NOT present when querying a region that is far enough away that no
 *      node containing the entity could have loose bounds reaching the query.
 *
 * ppm = 10 throughout (grid cell = 10x10 world units).
 * World bounds: [-1000, -1000, 1000, 1000] (grid cells -100 to 100 in each axis).
 */
class LooseQuadtreeWorldContainerTest : DescribeSpec({

    describe("LooseQuadtreeWorldContainer") {

        val ppm = 10

        lateinit var tree: LooseQuadtreeWorldContainer
        val outBodies = MutableOrderedSet<IBody>()
        val outFixtures = MutableOrderedSet<IFixture>()

        beforeEach {
            tree = LooseQuadtreeWorldContainer(
                ppm = ppm,
                worldMinX = -1000f,
                worldMinY = -1000f,
                worldMaxX = 1000f,
                worldMaxY = 1000f,
                maxDepth = 6,
                loosenessFactor = 2f
            )
            outBodies.clear()
            outFixtures.clear()
        }

        // ── Basic add & retrieve ──────────────────────────────────────────────────────

        it("should find a body when querying its grid cell") {
            // world [50, 50, 60, 60] -> grid cell (5, 5)
            val body = Body(BodyType.DYNAMIC, 50f, 50f, 10f, 10f)
            tree.addBody(body)

            tree.getBodies(5, 5, outBodies)

            outBodies shouldContain body
        }

        it("should not find a body when querying a far-away region") {
            // body at grid (5, 5); query at grid (50, 50) – (60, 60)
            val body = Body(BodyType.DYNAMIC, 50f, 50f, 10f, 10f)
            tree.addBody(body)

            tree.getBodies(50, 50, 60, 60, outBodies)

            outBodies shouldNotContain body
        }

        it("should find a fixture when querying its grid cell") {
            val mockBody = mockk<Body>()
            // world [30, 30, 40, 40] -> grid cell (3, 3)
            val fixture = Fixture(mockBody, "TestFixture", GameRectangle(30f, 30f, 10f, 10f), attachedToBody = false)
            tree.addFixture(fixture)

            tree.getFixtures(3, 3, outFixtures)

            outFixtures shouldContain fixture
        }

        it("should not find a fixture when querying a far-away region") {
            val mockBody = mockk<Body>()
            val fixture = Fixture(mockBody, "TestFixture", GameRectangle(30f, 30f, 10f, 10f), attachedToBody = false)
            tree.addFixture(fixture)

            tree.getFixtures(50, 50, 60, 60, outFixtures)

            outFixtures shouldNotContain fixture
        }

        // ── Multiple entities ─────────────────────────────────────────────────────────

        it("should find multiple bodies in a range query and exclude distant bodies") {
            val body1 = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)   // grid (1, 1)
            val body2 = Body(BodyType.DYNAMIC, 50f, 50f, 10f, 10f)   // grid (5, 5)
            val body3 = Body(BodyType.DYNAMIC, 500f, 500f, 10f, 10f) // grid (50, 50)

            tree.addBody(body1)
            tree.addBody(body2)
            tree.addBody(body3)

            // query grid (0, 0) – (9, 9) = world [0, 0, 100, 100]
            tree.getBodies(0, 0, 9, 9, outBodies)

            outBodies shouldContain body1
            outBodies shouldContain body2
            outBodies shouldNotContain body3
        }

        it("should find multiple fixtures in a range query and exclude distant fixtures") {
            val mockBody = mockk<Body>()
            val fixture1 = Fixture(mockBody, "F1", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)
            val fixture2 = Fixture(mockBody, "F2", GameRectangle(500f, 500f, 10f, 10f), attachedToBody = false)

            tree.addFixture(fixture1)
            tree.addFixture(fixture2)

            tree.getFixtures(0, 0, 9, 9, outFixtures)

            outFixtures shouldContain fixture1
            outFixtures shouldNotContain fixture2
        }

        // ── Large entities ────────────────────────────────────────────────────────────

        it("should find a large body from a query at any cell it overlaps") {
            // world [0, 0, 120, 100] -> spans grid (0,0) to (11, 9)
            val largeBody = Body(BodyType.STATIC, 0f, 0f, 120f, 100f)
            tree.addBody(largeBody)

            // query from the first cell
            tree.getBodies(0, 0, outBodies)
            outBodies shouldContain largeBody
            outBodies.clear()

            // query from a middle cell
            tree.getBodies(5, 4, outBodies)
            outBodies shouldContain largeBody
            outBodies.clear()

            // query from the last cell the body occupies
            tree.getBodies(11, 9, outBodies)
            outBodies shouldContain largeBody
        }

        it("should not find a large body when querying far outside its bounds") {
            val largeBody = Body(BodyType.STATIC, 0f, 0f, 120f, 100f)
            tree.addBody(largeBody)

            // query grid (50, 50) – (60, 60) = world [500, 500, 610, 610]
            tree.getBodies(50, 50, 60, 60, outBodies)
            outBodies shouldNotContain largeBody
        }

        // ── Boundary handling ─────────────────────────────────────────────────────────

        it("should find bodies on either side of a subdivision boundary in their respective cells") {
            // Two bodies side by side; each should be findable from its own cell
            val body1 = Body(BodyType.DYNAMIC, 0f, 0f, 10f, 10f)    // grid (0, 0)
            val body2 = Body(BodyType.DYNAMIC, 10f, 0f, 10f, 10f)   // grid (1, 0)

            tree.addBody(body1)
            tree.addBody(body2)

            tree.getBodies(0, 0, outBodies)
            outBodies shouldContain body1
            outBodies.clear()

            tree.getBodies(1, 0, outBodies)
            outBodies shouldContain body2
        }

        // ── Negative coordinates ──────────────────────────────────────────────────────

        it("should find a body at negative world coordinates") {
            // world [-50, -50, -40, -40] -> grid cell (-5, -5)
            val body = Body(BodyType.DYNAMIC, -50f, -50f, 10f, 10f)
            tree.addBody(body)

            tree.getBodies(-5, -5, outBodies)
            outBodies shouldContain body
            outBodies.clear()

            // far away in positive territory – should not be found
            tree.getBodies(50, 50, 60, 60, outBodies)
            outBodies shouldNotContain body
        }

        it("should find a fixture at negative world coordinates") {
            val mockBody = mockk<Body>()
            val fixture = Fixture(mockBody, "NegFix", GameRectangle(-80f, -80f, 10f, 10f), attachedToBody = false)
            tree.addFixture(fixture)

            tree.getFixtures(-8, -8, outFixtures)
            outFixtures shouldContain fixture
            outFixtures.clear()

            tree.getFixtures(50, 50, 60, 60, outFixtures)
            outFixtures shouldNotContain fixture
        }

        // ── getObjects ────────────────────────────────────────────────────────────────

        it("should return both bodies and fixtures via getObjects") {
            val body = Body(BodyType.DYNAMIC, 50f, 50f, 10f, 10f)
            val mockBody = mockk<Body>()
            val fixture = Fixture(mockBody, "F", GameRectangle(50f, 50f, 10f, 10f), attachedToBody = false)

            tree.addBody(body)
            tree.addFixture(fixture)

            val out = mutableListOf<Any>()
            tree.getObjects(4, 4, 6, 6, out)

            out shouldContain body
            out shouldContain fixture
        }

        // ── Clear ─────────────────────────────────────────────────────────────────────

        it("should remove all bodies and fixtures after clear") {
            val body = Body(BodyType.DYNAMIC, 10f, 10f, 10f, 10f)
            val mockBody = mockk<Body>()
            val fixture = Fixture(mockBody, "F", GameRectangle(10f, 10f, 10f, 10f), attachedToBody = false)

            tree.addBody(body)
            tree.addFixture(fixture)
            tree.clear()

            tree.getBodies(0, 0, 99, 99, outBodies)
            outBodies.size shouldBe 0

            tree.getFixtures(0, 0, 99, 99, outFixtures)
            outFixtures.size shouldBe 0
        }

        // ── Copy ──────────────────────────────────────────────────────────────────────

        it("copy should contain the same bodies and fixtures as the original") {
            val body = Body(BodyType.DYNAMIC, 50f, 50f, 10f, 10f)
            val mockBody = mockk<Body>()
            val fixture = Fixture(mockBody, "F", GameRectangle(50f, 50f, 10f, 10f), attachedToBody = false)

            tree.addBody(body)
            tree.addFixture(fixture)

            val copy = tree.copy()

            val copyBodies = MutableOrderedSet<IBody>()
            val copyFixtures = MutableOrderedSet<IFixture>()
            copy.getBodies(4, 4, 6, 6, copyBodies)
            copy.getFixtures(4, 4, 6, 6, copyFixtures)

            copyBodies shouldContain body
            copyFixtures shouldContain fixture
        }

        it("clearing the original after copy should not affect the copy") {
            val body = Body(BodyType.DYNAMIC, 50f, 50f, 10f, 10f)
            tree.addBody(body)

            val copy = tree.copy()
            tree.clear()

            val copyBodies = MutableOrderedSet<IBody>()
            copy.getBodies(4, 4, 6, 6, copyBodies)
            copyBodies shouldContain body
        }

        it("adding to the copy after copy should not affect the original") {
            val body1 = Body(BodyType.DYNAMIC, 50f, 50f, 10f, 10f)
            tree.addBody(body1)

            val copy = tree.copy()
            val body2 = Body(BodyType.DYNAMIC, 50f, 50f, 10f, 10f)
            copy.addBody(body2)

            // original should not contain body2
            tree.getBodies(4, 4, 6, 6, outBodies)
            outBodies shouldNotContain body2
        }
    }
})
