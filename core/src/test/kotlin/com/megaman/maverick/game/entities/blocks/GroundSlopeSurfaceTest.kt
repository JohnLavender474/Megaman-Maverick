package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GamePolygon
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.hasBodyLabel
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk

class GroundSlopeSurfaceTest : DescribeSpec({

    fun spawned(x: Float, y: Float, width: Float, height: Float, side: String): GroundSlope {
        val slope = GroundSlope(testServices(engine = mockk<GameEngine>(relaxed = true)))
        slope.init()
        slope.onSpawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle(x, y, width, height),
                ConstKeys.SIDE pairTo side
            )
        )
        return slope
    }

    describe("a shallow run 8 x 2 tiles that rises to the right") {
        val slope = spawned(0f, 0f, 256f, 64f, ConstKeys.RIGHT)

        it("starts one step above the low floor") {
            slope.surfaceAt(0f) shouldBe 2f
            slope.surfaceAt(7.9f) shouldBe 2f
        }

        it("rises 2 px for each 8 px of x") {
            slope.surfaceAt(8f) shouldBe 4f
            slope.surfaceAt(128f) shouldBe 34f
        }

        it("reaches the top on the last step and at the high end") {
            slope.surfaceAt(248f) shouldBe 64f
            slope.surfaceAt(256f) shouldBe 64f
        }

        it("clamps an x outside the run to the run ends") {
            slope.surfaceAt(-10f) shouldBe 2f
            slope.surfaceAt(300f) shouldBe 64f
        }
    }

    describe("a shallow run that rises to the left") {
        val slope = spawned(100f, 50f, 256f, 64f, ConstKeys.LEFT)

        it("is the mirror of the run that rises to the right") {
            slope.surfaceAt(356f) shouldBe 52f
            slope.surfaceAt(348f) shouldBe 54f
            slope.surfaceAt(100f) shouldBe 114f
        }
    }

    describe("a steep run 4 x 2 tiles") {
        it("rises 2 px for each 4 px of x, to the right") {
            val slope = spawned(0f, 0f, 128f, 64f, ConstKeys.RIGHT)
            slope.surfaceAt(0f) shouldBe 2f
            slope.surfaceAt(4f) shouldBe 4f
            slope.surfaceAt(124f) shouldBe 64f
        }

        it("rises 2 px for each 4 px of x, to the left") {
            val slope = spawned(0f, 0f, 128f, 64f, ConstKeys.LEFT)
            slope.surfaceAt(128f) shouldBe 2f
            slope.surfaceAt(124f) shouldBe 4f
            slope.surfaceAt(0f) shouldBe 64f
        }
    }

    describe("a ratio that is not 1/4 or 1/2") {
        it("uses the nearer ratio") {
            val slope = spawned(0f, 0f, 128f, 50f, ConstKeys.RIGHT)
            slope.surfaceAt(4f) shouldBe 4f
            slope.surfaceAt(128f) shouldBe 50f
        }
    }

    describe("the block fixture and the label") {
        it("is a triangle under the surface, on the high side") {
            val slope = spawned(0f, 0f, 256f, 64f, ConstKeys.RIGHT)
            val shape = slope.blockFixture.getShape().shouldBeInstanceOf<GamePolygon>()
            shape.contains(250f, 10f) shouldBe true
            shape.contains(10f, 60f) shouldBe false
        }

        it("is a triangle under the surface, on the left high side") {
            val slope = spawned(0f, 0f, 256f, 64f, ConstKeys.LEFT)
            val shape = slope.blockFixture.getShape().shouldBeInstanceOf<GamePolygon>()
            shape.contains(10f, 10f) shouldBe true
            shape.contains(250f, 60f) shouldBe false
        }

        it("has the slope label after the spawn") {
            spawned(0f, 0f, 256f, 64f, ConstKeys.RIGHT).body.hasBodyLabel(BodyLabel.SLOPE) shouldBe true
        }
    }

    describe("the shapes") {
        val slope = spawned(0f, 0f, 256f, 64f, ConstKeys.RIGHT)
        val shapes = slope.getComponent(DrawableShapesComponent::class)!!

        it("draws nothing outside the debug view, so the map tiles show the slope") {
            shapes.prodShapeSuppliers.mapNotNull { it() } shouldBe emptyList()
        }

        it("draws the triangle as a line in the debug view") {
            val drawn = shapes.debugShapeSuppliers.mapNotNull { it() }
            drawn shouldContain slope.blockFixture
            slope.blockFixture.getShape().shouldBeInstanceOf<GamePolygon>()
            slope.blockFixture.drawingShapeType shouldBe ShapeRenderer.ShapeType.Line
        }
    }
})
