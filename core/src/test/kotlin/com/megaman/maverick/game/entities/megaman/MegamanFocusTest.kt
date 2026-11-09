package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.megaman.components.defineCameraSubjectComponent
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.unmockkAll

class MegamanFocusTest : DescribeSpec({

    afterEach { unmockkAll() }

    fun focusAt(direction: Direction): Vector2 {
        val megaman = Megaman(testServices(engine = mockk<GameEngine>(relaxed = true)))
        val body = Body(BodyType.DYNAMIC)
        body.setSize(10f, 20f)
        body.setPosition(100f, 200f)
        body.direction = direction
        megaman.addComponent(BodyComponent(body))
        return megaman.defineCameraSubjectComponent().getFocusPosition()
    }

    describe("the focus position") {
        it("is half a body height past the bottom center with gravity down") {
            val focus = focusAt(Direction.UP)

            focus.x shouldBe 105f
            focus.y shouldBe 224f
        }

        it("is half a body height below the top center with the body upside down") {
            val focus = focusAt(Direction.DOWN)

            focus.x shouldBe 105f
            focus.y shouldBe 196f
        }

        it("is half a body height left of the right center with the body turned left") {
            val focus = focusAt(Direction.LEFT)

            focus.x shouldBe 91f
            focus.y shouldBe 210f
        }

        it("is half a body height right of the left center with the body turned right") {
            val focus = focusAt(Direction.RIGHT)

            focus.x shouldBe 119f
            focus.y shouldBe 210f
        }
    }
})
