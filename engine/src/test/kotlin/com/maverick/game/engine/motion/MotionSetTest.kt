package com.maverick.game.engine.motion

import com.badlogic.gdx.math.Vector2
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class MotionSetTest : DescribeSpec({

    fun IMotion.valuesAfter(steps: Int): List<Vector2?> = (0 until steps).map {
        update(0.1f)
        getMotionValue(Vector2())
    }

    describe("SineWave.set") {
        it("moves like a new sine wave from the new origin and keeps its position vector") {
            val position = Vector2()
            val sine = SineWave(position, 1f, 2f, 3f)
            sine.valuesAfter(5)

            sine.set(Vector2(10f, 20f), 4f, 5f, 6f) shouldBeSameInstanceAs sine

            sine.position shouldBeSameInstanceAs position
            position shouldBe Vector2(10f, 20f)
            sine.valuesAfter(5) shouldBe SineWave(Vector2(10f, 20f), 4f, 5f, 6f).valuesAfter(5)
        }
    }

    describe("Trajectory.set") {
        it("moves like a new trajectory of the new text") {
            val trajectory = Trajectory("1,0,0.2;0,1,0.2", 2)
            trajectory.valuesAfter(3)

            trajectory.set("0,-1,0.1;3,3,0.3")

            trajectory.getMotionValue(Vector2()) shouldBe Trajectory("0,-1,0.1;3,3,0.3", 2).getMotionValue(Vector2())
            trajectory.valuesAfter(7) shouldBe Trajectory("0,-1,0.1;3,3,0.3", 2).valuesAfter(7)
        }

        it("starts again from the first definition when the text is the same") {
            val trajectory = Trajectory("1,0,0.2;0,1,0.2", 1)
            trajectory.valuesAfter(3)

            trajectory.set("1,0,0.2;0,1,0.2")

            trajectory.getMotionValue(Vector2()) shouldBe Vector2(1f, 0f)
            trajectory.valuesAfter(6) shouldBe Trajectory("1,0,0.2;0,1,0.2", 1).valuesAfter(6)
        }
    }

    describe("RotatingLine.set") {
        it("turns like a new rotating line with the new values") {
            val line = RotatingLine(Vector2(1f, 1f), 3f, 90f, 45f)
            line.scaleX = 2f
            line.valuesAfter(4)

            line.set(Vector2(5f, 6f), 2f, 30f, 10f) shouldBeSameInstanceAs line

            val fresh = RotatingLine(Vector2(5f, 6f), 2f, 30f, 10f)
            line.getMotionValue(Vector2()) shouldBe fresh.getMotionValue(Vector2())
            line.valuesAfter(4) shouldBe fresh.valuesAfter(4)
            line.scaleX shouldBe 1f
        }
    }
})
