package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.maverick.game.engine.common.enums.Direction
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class CameraRotationTest : DescribeSpec({

    describe("a turn") {
        it("changes the direction at the start and the angle over the time") {
            val rotation = CameraRotation()
            rotation.start(Direction.LEFT, 1f)
            rotation.direction shouldBe Direction.LEFT
            rotation.rotating shouldBe true
            rotation.angle shouldBe 0f

            rotation.step(0.5f)
            rotation.angle shouldBe (45f plusOrMinus 0.01f)

            rotation.step(0.5f)
            rotation.angle shouldBe (90f plusOrMinus 0.01f)
            rotation.rotating shouldBe false
        }

        it("takes the shortest way") {
            val rotation = CameraRotation()
            rotation.start(Direction.RIGHT, 1f)
            rotation.step(1f)
            rotation.angle shouldBe (-90f plusOrMinus 0.01f)
        }

        it("calls the end callback once, on the last step") {
            var calls = 0
            val rotation = CameraRotation { calls++ }
            rotation.start(Direction.LEFT, 1f)
            rotation.step(0.5f)
            calls shouldBe 0
            rotation.step(0.5f)
            calls shouldBe 1
            rotation.step(0.5f)
            calls shouldBe 1
        }

        it("does nothing with no turn") {
            val rotation = CameraRotation()
            rotation.step(1f)
            rotation.angle shouldBe 0f
        }

        it("does not reset with the controller") {
            val controller = CameraController(OrthographicCamera())
            val rotation = CameraRotation()
            controller.addEffect(rotation)
            rotation.snap(Direction.LEFT)
            controller.reset()
            rotation.direction shouldBe Direction.LEFT
        }
    }

    describe("the controller") {
        it("writes the orientation after the parts update") {
            val camera = OrthographicCamera()
            val controller = CameraController(camera)
            val rotation = CameraRotation()
            controller.addEffect(rotation)

            rotation.start(Direction.LEFT, 1f)
            controller.update(1f)

            camera.up.x shouldBe (1f plusOrMinus 0.0001f)
            camera.up.y shouldBe (0f plusOrMinus 0.0001f)
        }

        it("writes a snap at once with applyOrientation") {
            val camera = OrthographicCamera()
            val controller = CameraController(camera)
            val rotation = CameraRotation()
            controller.addEffect(rotation)

            rotation.snap(Direction.LEFT)
            controller.applyOrientation()

            camera.up.x shouldBe (1f plusOrMinus 0.0001f)
        }

        it("does not write with no rotation part") {
            val camera = OrthographicCamera()
            camera.up.set(5f, 5f, 5f)
            val controller = CameraController(camera)
            controller.update(1f)
            controller.applyOrientation()
            camera.up.x shouldBe 5f
        }
    }

    describe("a turn during a turn") {
        it("keeps the time that passed in the first turn") {
            val rotation = CameraRotation()
            rotation.start(Direction.LEFT, 1f)
            rotation.step(0.5f)
            val angle = rotation.angle

            rotation.start(Direction.UP, 1f)
            rotation.rotating shouldBe true
            rotation.angle shouldBe angle

            rotation.step(0.5f)
            rotation.rotating shouldBe false
            rotation.angle shouldBe (0f plusOrMinus 0.01f)
        }
    }

    describe("the snap") {
        it("ends a turn at once, with no end callback") {
            var calls = 0
            val rotation = CameraRotation { calls++ }
            rotation.start(Direction.LEFT, 1f)
            rotation.step(0.5f)

            rotation.snap(Direction.UP)

            rotation.direction shouldBe Direction.UP
            rotation.angle shouldBe 0f
            rotation.rotating shouldBe false
            calls shouldBe 0
        }
    }

    describe("sideways") {
        it("moves from 0 to 1 over a turn from up to left") {
            val rotation = CameraRotation()
            rotation.sideways shouldBe 0f
            rotation.start(Direction.LEFT, 1f)
            rotation.sideways shouldBe 0f
            rotation.step(0.5f)
            rotation.sideways shouldBe (0.5f plusOrMinus 0.01f)
            rotation.step(0.5f)
            rotation.sideways shouldBe 1f
        }

        it("does not change on a flip") {
            val rotation = CameraRotation()
            rotation.start(Direction.DOWN, 1f)
            rotation.step(0.5f)
            rotation.sideways shouldBe 0f
        }
    }
})
