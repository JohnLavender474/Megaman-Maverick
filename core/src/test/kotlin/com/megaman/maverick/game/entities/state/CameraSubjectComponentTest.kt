package com.megaman.maverick.game.entities.state

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class CameraSubjectComponentTest : DescribeSpec({

    describe("CameraSubjectComponent") {
        it("gives the values of its hooks") {
            val bounds = GameRectangle(0f, 0f, 2f, 2f)
            val position = Vector2(3f, 4f)
            val component = CameraSubjectComponent({ bounds }, { position })

            component.getSubjectBounds() shouldBeSameInstanceAs bounds
            component.getFocusPosition() shouldBeSameInstanceAs position
        }

        it("calls the hooks on each read") {
            var x = 1f
            val component = CameraSubjectComponent({ GameRectangle(x, 0f, 2f, 2f) }, { Vector2(x, 0f) })

            component.getSubjectBounds().getX() shouldBe 1f
            component.getFocusPosition().x shouldBe 1f

            x = 5f

            component.getSubjectBounds().getX() shouldBe 5f
            component.getFocusPosition().x shouldBe 5f
        }
    }
})
