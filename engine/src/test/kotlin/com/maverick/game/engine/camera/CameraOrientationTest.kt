package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class CameraOrientationTest : DescribeSpec({

    describe("write") {
        it("sets the up vector from the angle, as a rotation of the plain camera does") {
            val camera = OrthographicCamera()
            val plain = OrthographicCamera().also { it.rotate(90f) }

            CameraOrientation(camera).write(90f)

            camera.up.x shouldBe (plain.up.x plusOrMinus 0.0001f)
            camera.up.y shouldBe (plain.up.y plusOrMinus 0.0001f)
        }

        it("is absolute, so a write of 0 gives the default orientation") {
            val camera = OrthographicCamera()
            val orientation = CameraOrientation(camera)
            orientation.write(90f)
            orientation.write(0f)
            camera.up.x shouldBe 0f
            camera.up.y shouldBe 1f
        }

        it("does not write when the angle did not change") {
            val camera = OrthographicCamera()
            val orientation = CameraOrientation(camera)
            orientation.write(90f)
            camera.up.set(5f, 5f, 5f)
            orientation.write(90f)
            camera.up.x shouldBe 5f
        }
    }
})
