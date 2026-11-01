package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.common.enums.Direction
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class RotatableCameraTest : DescribeSpec({

    fun camera() = RotatableCamera(printDebug = false).also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
        it.immediateRotation(Direction.UP)
    }

    describe("the blended view size") {
        it("moves from the old size to the new size over the turn, and the final size is at once") {
            val camera = camera()
            camera.startRotation(Direction.LEFT, 1f)
            camera.viewWidth shouldBe 80f
            camera.blendedViewWidth shouldBe 100f
            camera.blendedViewHeight shouldBe 80f
            camera.update(0.5f)
            camera.blendedViewWidth shouldBe (90f plusOrMinus 0.01f)
            camera.blendedViewHeight shouldBe (90f plusOrMinus 0.01f)
            camera.update(0.5f)
            camera.blendedViewWidth shouldBe 80f
            camera.blendedViewHeight shouldBe 100f
        }

        it("does not change on a flip") {
            val camera = camera()
            camera.startRotation(Direction.DOWN, 1f)
            camera.update(0.5f)
            camera.blendedViewWidth shouldBe (100f plusOrMinus 0.01f)
        }

        it("ends on an immediate rotation") {
            val camera = camera()
            camera.startRotation(Direction.LEFT, 1f)
            camera.update(0.5f)
            camera.immediateRotation(Direction.UP)
            camera.blendedViewWidth shouldBe 100f
        }

        it("continues from the current size when a turn starts during a turn") {
            val camera = camera()
            camera.startRotation(Direction.LEFT, 1f)
            camera.update(0.5f)
            val size = camera.blendedViewWidth
            camera.startRotation(Direction.UP, 1f)
            camera.blendedViewWidth shouldBe (size plusOrMinus 0.01f)
        }
    }
})
