package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private class TestFocus(var x: Float, var y: Float) : IFocusable {
    override fun getFocusBounds() = GameRectangle().setSize(10f, 10f).setCenter(x, y)
    override fun getFocusPosition() = Vector2(x, y)
}

private class TestPart(val log: MutableList<String>, val name: String, val hold: Boolean = false) :
    ICameraPart {
    override fun update(camera: Camera, delta: Float): Boolean {
        log.add(name)
        return hold
    }
}

private abstract class TestPartBase : ICameraPart {
    override fun update(camera: Camera, delta: Float) = false
}

class CameraPartsTest : DescribeSpec({

    fun camera() = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }

    describe("CameraAxisLimit") {
        it("keeps a view inside a range") {
            val limit = CameraAxisLimit().also { it.set(0f, 100f) }
            limit.clamp(50f, 20f) shouldBe 50f
            limit.clamp(-5f, 20f) shouldBe 10f
            limit.clamp(200f, 20f) shouldBe 90f
        }

        it("lets min win in a range smaller than the view") {
            val limit = CameraAxisLimit().also { it.set(0f, 10f) }
            limit.clamp(50f, 20f) shouldBe 10f
            limit.clamp(-50f, 20f) shouldBe 10f
        }

        it("has no limit by default") {
            CameraAxisLimit().clamp(123f, 20f) shouldBe 123f
        }

        it("eases a narrowing edge from the view, not from the old limit") {
            val limit = CameraAxisLimit().also { it.set(0f, 1000f) }
            val target = CameraAxisLimit().also { it.set(0f, 300f) }
            limit.easeTo(target, 100f, 100f, 10f)
            limit.max shouldBe 300f
            val close = CameraAxisLimit().also { it.set(0f, 180f) }
            limit.set(0f, 1000f)
            limit.easeTo(close, 100f, 100f, 10f)
            limit.max shouldBe 180f
            val far = CameraAxisLimit().also { it.set(0f, 100f) }
            limit.set(0f, 1000f)
            limit.easeTo(far, 100f, 100f, 10f)
            limit.max shouldBe 140f
        }

        it("eases a widening edge by one step, and stops at the target") {
            val limit = CameraAxisLimit().also { it.set(100f, 200f) }
            val target = CameraAxisLimit().also { it.set(95f, 500f) }
            limit.easeTo(target, 150f, 100f, 10f)
            limit.min shouldBe 95f
            limit.max shouldBe 210f
        }

        it("cuts a range to the outer range") {
            val limit = CameraAxisLimit().also { it.set(-50f, 500f) }
            limit.resolve(20f, 0f, 100f)
            limit.min shouldBe 0f
            limit.max shouldBe 100f
        }

        it("turns a pin into a range of one view size, cut to the outer range") {
            val limit = CameraAxisLimit().also { it.setPin(50f) }
            limit.resolve(20f, 0f, 100f)
            limit.min shouldBe 40f
            limit.max shouldBe 60f
            val atEdge = CameraAxisLimit().also { it.setPin(95f) }
            atEdge.resolve(20f, 0f, 100f)
            atEdge.max shouldBe 100f
        }

        it("takes a pinned target at once, with no ease") {
            val target = CameraAxisLimit().also {
                it.setPin(500f)
                it.resolve(20f, 0f, 1000f)
            }
            val limit = CameraAxisLimit().also { it.set(0f, 100f) }
            limit.easeTo(target, 50f, 20f, 1f)
            limit.min shouldBe 490f
            limit.max shouldBe 510f
        }

        it("copies a limit") {
            val limit = CameraAxisLimit().also { it.set(CameraAxisLimit().also { other -> other.set(1f, 2f) }) }
            limit.min shouldBe 1f
            limit.max shouldBe 2f
        }
    }

    describe("CameraController") {
        it("keeps the position after the movers, with no effect in it") {
            val camera = camera()
            val controller = CameraController(camera)
            controller.addMover(object : TestPartBase() {
                override fun update(camera: Camera, delta: Float): Boolean {
                    camera.position.x = 10f
                    return false
                }
            })
            controller.addEffect(object : TestPartBase() {
                override fun update(camera: Camera, delta: Float): Boolean {
                    camera.position.x = 15f
                    return false
                }
            })
            controller.update(0.1f)
            controller.settledPosition.x shouldBe 10f
            camera.position.x shouldBe 15f
        }

        it("resets each part that is resettable, and no other part") {
            val log = ArrayList<String>()
            val controller = CameraController(camera())
            controller.addMover(object : TestPartBase(), Resettable {
                override fun reset() {
                    log.add("mover")
                }
            })
            controller.addEffect(object : TestPartBase(), Resettable {
                override fun reset() {
                    log.add("effect")
                }
            })
            controller.addEffect(TestPart(log, "plain"))
            controller.reset()
            log shouldBe listOf("mover", "effect")
        }

        it("runs the movers in order, then the effects") {
            val log = ArrayList<String>()
            val controller = CameraController(camera())
            controller.addMover(TestPart(log, "a"))
            controller.addMover(TestPart(log, "b"))
            controller.addEffect(TestPart(log, "c"))
            controller.update(0.1f)
            log shouldBe listOf("a", "b", "c")
        }

        it("ends the movers at a mover that holds the camera, and still runs the effects") {
            val log = ArrayList<String>()
            val controller = CameraController(camera())
            controller.addMover(TestPart(log, "a", hold = true))
            controller.addMover(TestPart(log, "b"))
            controller.addEffect(TestPart(log, "c"))
            controller.update(0.1f)
            log shouldBe listOf("a", "c")
        }

        it("finds a part by type, or null if the part is omitted") {
            val controller = CameraController(camera())
            val shake = CameraShake(1f)
            controller.addEffect(shake)
            controller.getPart(CameraShake::class)!! shouldBeSameInstanceAs shake
            controller.getPart(CameraFollow::class).shouldBeNull()
        }

        it("works with only a follow part") {
            val camera = camera()
            val follow = CameraFollow(1f, { 1f })
            follow.setFocus(camera, TestFocus(0f, 0f))
            val controller = CameraController(camera)
            controller.addMover(follow)
            (follow.focus as TestFocus).x = 50f
            controller.update(0.1f)
            camera.position.x shouldBe 50f
        }
    }

    describe("CameraFollow") {
        it("clamps x to the limits and leaves y free") {
            val camera = camera()
            val follow = CameraFollow(1f, { 1f }, minX = 10f, maxX = 20f)
            follow.setFocus(camera, TestFocus(5f, 500f))
            follow.update(camera, 0.1f)
            camera.position.x shouldBe 10f
            camera.position.y shouldBe 500f
        }

        it("follows only x in the x-only mode, and nothing in the off mode") {
            val camera = camera()
            val focus = TestFocus(0f, 0f)
            val follow = CameraFollow(1f, { 1f })
            follow.setFocus(camera, focus)
            focus.x = 30f
            focus.y = 40f
            follow.mode = CameraFollowMode.X_ONLY
            follow.update(camera, 0.1f)
            camera.position.x shouldBe 30f
            camera.position.y shouldBe 0f
            focus.x = 60f
            follow.mode = CameraFollowMode.OFF
            follow.update(camera, 0.1f)
            camera.position.x shouldBe 30f
        }

        it("moves part of the way in a smooth follow, and the stop rule ends it near the focus") {
            val camera = camera()
            val focus = TestFocus(0f, 0f)
            val follow = CameraFollow(1f, { 0.5f })
            follow.setFocus(camera, focus)
            focus.x = 100f
            follow.startSmoothFollow()
            follow.update(camera, 0.1f)
            camera.position.x shouldBe 50f
            follow.stopRule.update(camera, 0.1f)
            follow.smooth shouldBe true
            focus.x = 50.5f
            follow.stopRule.update(camera, 0.1f)
            follow.smooth shouldBe false
        }
    }

    describe("CameraRoomTransition") {
        fun transition(follow: CameraFollow, log: MutableList<String>) =
            CameraRoomTransition(follow, 20f, 0.2f, 0.4f).also {
                it.onBegin = { log.add("begin") }
                it.onContinue = { log.add("continue") }
                it.onEnd = { log.add("end") }
            }

        it("waits the delay, pans to the target, then ends in a later frame") {
            val camera = camera()
            camera.position.set(50f, 40f, 0f)
            val follow = CameraFollow(1f, { 1f })
            follow.setFocus(camera, TestFocus(50f, 40f))
            val log = ArrayList<String>()
            val transition = transition(follow, log)

            transition.start(camera, Rectangle(200f, 0f, 100f, 80f), Direction.RIGHT)
            transition.transitioning shouldBe true

            transition.update(camera, 0.1f) shouldBe true
            camera.position.x shouldBe 50f
            transition.update(camera, 0.1f) shouldBe true
            camera.position.x shouldBe (100f plusOrMinus 0.01f)

            transition.update(camera, 0.2f)
            camera.position.x shouldBe (200f plusOrMinus 0.01f)
            transition.update(camera, 0.2f)
            camera.position.x shouldBe (250f plusOrMinus 0.01f)
            transition.transitioning shouldBe true

            transition.update(camera, 0.1f) shouldBe true
            transition.transitioning shouldBe false
            log shouldBe listOf("begin", "continue", "continue", "continue", "end")

            transition.update(camera, 0.1f) shouldBe false
        }

        it("gives the focus target by the direction, and no interpolation when idle") {
            val camera = camera()
            val follow = CameraFollow(1f, { 1f })
            follow.setFocus(camera, TestFocus(50f, 40f))
            val transition = transition(follow, ArrayList())

            transition.getTransitionInterpolation(Vector2()).shouldBeNull()

            transition.start(camera, Rectangle(200f, 0f, 100f, 80f), Direction.LEFT)
            repeat(2) { transition.update(camera, 0.1f) }
            transition.update(camera, 0.4f)
            transition.getTransitionInterpolation(Vector2())!!.x shouldBe (280f plusOrMinus 0.01f)
        }
    }

    describe("CameraShake") {
        it("moves the camera each interval, then stops") {
            val camera = camera()
            val shake = CameraShake(2f)
            shake.startShake(1f, 0.1f, 1f, 0f)
            shake.update(camera, 0.1f)
            camera.position.x shouldBe 2f
            shake.update(camera, 0.1f)
            camera.position.x shouldBe 0f
            shake.stop()
            shake.update(camera, 0.1f)
            camera.position.x shouldBe 0f
            shake.isFinished shouldBe true
        }
    }
})
