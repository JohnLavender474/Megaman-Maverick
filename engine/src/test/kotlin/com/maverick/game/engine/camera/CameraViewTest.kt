package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class CameraViewTest : DescribeSpec({

    fun camera() = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }

    describe("the blended view size") {
        it("moves from the old size to the new size over the turn, and the final size is at once") {
            val rotation = CameraRotation()
            val view = CameraView(camera(), rotation)

            rotation.start(Direction.LEFT, 1f)
            view.viewWidth shouldBe 80f
            view.viewHeight shouldBe 100f
            view.blendedViewWidth shouldBe 100f
            view.blendedViewHeight shouldBe 80f

            rotation.step(0.5f)
            view.blendedViewWidth shouldBe (90f plusOrMinus 0.01f)
            view.blendedViewHeight shouldBe (90f plusOrMinus 0.01f)

            rotation.step(0.5f)
            view.blendedViewWidth shouldBe 80f
            view.blendedViewHeight shouldBe 100f
        }

        it("does not change on a flip") {
            val rotation = CameraRotation()
            val view = CameraView(camera(), rotation)
            rotation.start(Direction.DOWN, 1f)
            rotation.step(0.5f)
            view.blendedViewWidth shouldBe (100f plusOrMinus 0.01f)
        }

        it("ends on a snap") {
            val rotation = CameraRotation()
            val view = CameraView(camera(), rotation)
            rotation.start(Direction.LEFT, 1f)
            rotation.step(0.5f)
            rotation.snap(Direction.UP)
            view.blendedViewWidth shouldBe 100f
        }

        it("continues from the current size when a turn starts during a turn") {
            val rotation = CameraRotation()
            val view = CameraView(camera(), rotation)
            rotation.start(Direction.LEFT, 1f)
            rotation.step(0.5f)
            val size = view.blendedViewWidth
            rotation.start(Direction.UP, 1f)
            view.blendedViewWidth shouldBe (size plusOrMinus 0.01f)
        }
    }

    describe("a camera with no rotation part") {
        it("gives the defaults") {
            val view = CameraView(camera())
            view.direction shouldBe Direction.UP
            view.rotating shouldBe false
            view.viewWidth shouldBe 100f
            view.viewHeight shouldBe 80f
            view.blendedViewWidth shouldBe 100f
            view.blendedViewHeight shouldBe 80f
        }
    }

    describe("the bounds") {
        it("gives the plain rectangle at the camera position") {
            val camera = camera().also { it.position.set(50f, 40f, 0f) }
            val out = CameraView(camera).toGameRectangle(GameRectangle())
            out.getX() shouldBe 0f
            out.getY() shouldBe 0f
            out.getWidth() shouldBe 100f
            out.getHeight() shouldBe 80f
        }
    }
})
