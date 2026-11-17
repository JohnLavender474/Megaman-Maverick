package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CameraRoomTransitionTest : DescribeSpec({

    fun camera() = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }

    val next = Rectangle(100f, 0f, 100f, 80f)

    describe("axisOnly") {
        it("is false for a start that does not give it") {
            val transition = CameraRoomTransition(20f, 0f, 0.01f)
            transition.start(camera(), next, Direction.RIGHT, Vector2(50f, 40f))
            transition.axisOnly shouldBe false
        }

        it("is set by the start and cleared when the pan ends") {
            val camera = camera()
            val transition = CameraRoomTransition(20f, 0f, 0.01f)
            transition.start(camera, next, Direction.RIGHT, Vector2(50f, 40f), axisOnly = true)
            transition.axisOnly shouldBe true

            var axisOnlyAtEnd: Boolean? = null
            transition.onEnd = { axisOnlyAtEnd = transition.axisOnly }
            repeat(4) { transition.update(camera, 0.02f) }

            transition.transitioning shouldBe false
            axisOnlyAtEnd shouldBe false
            transition.axisOnly shouldBe false
        }

        it("throws for a vertical or undirected axis-only start, and does not start the pan") {
            listOf(Direction.UP, Direction.DOWN, null).forEach { direction ->
                val transition = CameraRoomTransition(20f, 0f, 0.01f)
                shouldThrow<IllegalArgumentException> {
                    transition.start(camera(), next, direction, Vector2(50f, 40f), axisOnly = true)
                }
                transition.transitioning shouldBe false
                transition.axisOnly shouldBe false
            }
        }

        it("allows a vertical start that is not axis-only") {
            val transition = CameraRoomTransition(20f, 0f, 0.01f)
            transition.start(camera(), next, Direction.UP, Vector2(50f, 40f))
            transition.transitioning shouldBe true
        }

        it("takes the value of the latest start, as when a gate starts a pan during a gathered pan") {
            val transition = CameraRoomTransition(20f, 0f, 0.01f)
            transition.start(camera(), next, Direction.RIGHT, Vector2(50f, 40f), axisOnly = true)
            transition.start(camera(), next, Direction.RIGHT, Vector2(50f, 40f))
            transition.axisOnly shouldBe false
        }

        it("is cleared by cancel") {
            val transition = CameraRoomTransition(20f, 0f, 0.01f)
            transition.start(camera(), next, Direction.RIGHT, Vector2(50f, 40f), axisOnly = true)
            transition.cancel()
            transition.axisOnly shouldBe false
        }
    }
})
