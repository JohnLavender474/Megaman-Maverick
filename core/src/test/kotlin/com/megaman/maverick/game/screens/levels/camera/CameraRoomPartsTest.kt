package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.camera.CameraController
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraFollowMode
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private class Focus(var x: Float, var y: Float) : IFocusable {
    override fun getFocusBounds() = GameRectangle().setSize(10f, 10f).setCenter(x, y)
    override fun getFocusPosition() = Vector2(x, y)
}

private fun room(name: String, x: Float, y: Float, width: Float, height: Float) =
    RectangleMapObject(x, y, width, height).also { it.name = name }

private class Rig(direction: Direction, rooms: Array<RectangleMapObject>, focusX: Float, focusY: Float) {
    val camera = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val rotation = CameraRotation().also { it.snap(direction) }
    val view = CameraView(camera, rotation)
    val focus = Focus(focusX, focusY)
    val follow = CameraFollow(1f, { 1f })
    val transition = CameraRoomTransition(follow, 20f, 0.1f, 0.1f)
    val roomBounds = CameraRoomBounds(follow, transition, Vector2(50f, 50f)).also { it.gameRooms = rooms }
    val controller = CameraController(camera).also {
        it.addMover(roomBounds)
        it.addMover(transition)
        it.addMover(follow)
        val limitStage = CameraLimitStage(view, follow, roomBounds, it.settledPosition, 1f)
        it.addMover(limitStage)
        it.addMover(CameraRoomClamp(view, limitStage))
        it.addMover(CameraRoomScan(roomBounds))
    }

    init {
        follow.setFocus(camera, focus)
        roomBounds.reset()
        controller.update(0.01f)
    }
}

class CameraRoomPartsTest : DescribeSpec({

    describe("CameraRoomBounds and CameraRoomClamp") {
        it("finds the room at a reset, and clamps the camera into it") {
            val rig = Rig(Direction.UP, Array.with(room("a", 0f, 0f, 200f, 200f)), 10f, 190f)
            rig.roomBounds.currentGameRoom!!.name shouldBe "a"
            rig.controller.update(0.01f)
            rig.camera.position.x shouldBe 50f
            rig.camera.position.y shouldBe 160f
        }

        it("swaps the viewport size when the camera is on its side") {
            val rig = Rig(Direction.LEFT, Array.with(room("a", 0f, 0f, 200f, 200f)), 10f, 190f)
            rig.controller.update(0.01f)
            rig.camera.position.x shouldBe 40f
            rig.camera.position.y shouldBe 150f
        }

        it("follows x only and stays in the prior room when the focus is in no room") {
            val rig = Rig(
                Direction.UP,
                Array.with(room("a", 0f, 0f, 200f, 200f), room("b", 300f, 0f, 200f, 200f)),
                100f, 100f
            )
            rig.focus.x = 250f
            rig.controller.update(0.01f)
            rig.roomBounds.currentGameRoom.shouldBeNull()
            rig.roomBounds.priorGameRoom!!.name shouldBe "a"
            rig.follow.mode shouldBe CameraFollowMode.X_ONLY
            rig.camera.position.x shouldBe 150f
        }

        it("starts a transition when the focus overlaps another room") {
            val rig = Rig(
                Direction.UP,
                Array.with(room("a", 0f, 0f, 200f, 200f), room("b", 200f, 0f, 200f, 200f)),
                100f, 100f
            )
            rig.focus.x = 199f
            rig.controller.update(0.01f)
            rig.transition.transitioning shouldBe true
            rig.roomBounds.currentGameRoom!!.name shouldBe "b"
            rig.roomBounds.priorGameRoom!!.name shouldBe "a"
        }

        it("a reset cancels a transition") {
            val rig = Rig(
                Direction.UP,
                Array.with(room("a", 0f, 0f, 200f, 200f), room("b", 200f, 0f, 200f, 200f)),
                100f, 100f
            )
            rig.roomBounds.transitionToRoom(rig.camera, "b", Direction.RIGHT) shouldBe true
            rig.roomBounds.reset()
            rig.controller.update(0.01f)
            rig.transition.transitioning shouldBe false
            rig.roomBounds.currentGameRoom!!.name shouldBe "a"
        }
    }
})
