package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.IDrawOffsetSource
import com.maverick.game.engine.camera.CameraDrawOffset
import com.maverick.game.engine.camera.CameraLimitStage
import com.maverick.game.engine.camera.CameraRoomBounds
import com.maverick.game.engine.camera.IRoomSubject
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.camera.CameraController
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.camera.ICameraPart
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.world.IDrawOffsets
import com.maverick.game.engine.world.body.IBody
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

private class OffsetFocus(var x: Float, var y: Float, val drawOffset: Vector2) : IFocusable, IRoomSubject {
    override fun getSubjectBounds() = GameRectangle().setSize(10f, 10f).setCenter(x, y)
    override fun getFocusPosition() = Vector2(x, y)
}

private class SwitchPart(var hold: Boolean = false, var moveX: Float = 0f) : ICameraPart {
    override fun update(camera: Camera, delta: Float): Boolean {
        camera.position.x += moveX
        return hold
    }
}

private class DrawOffsetRig {
    val camera = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val view = CameraView(camera, CameraRotation())
    val focus = OffsetFocus(200f, 40f, Vector2(-2f, -1f))
    val follow = CameraFollow(1f, { 1f })
    val transition = CameraRoomTransition(20f, 0.1f, 0.1f)
    val roomBounds = CameraRoomBounds(follow, transition, Vector2(50f, 50f)).also {
        it.gameRooms = Array.with(RectangleMapObject(0f, 0f, 400f, 80f).also { room -> room.name = "a" })
    }
    val before = SwitchPart()
    val after = SwitchPart()
    val controller = CameraController(camera)
    val stage = CameraLimitStage(view, follow, roomBounds, controller.settledPosition, 100f)
    val noOffsets = object : IDrawOffsets {
        override fun getDrawOffset(body: IBody, out: Vector2): Vector2 = out.setZero()
    }
    val drawOffset = CameraDrawOffset(view, follow, stage, controller.settledPosition, noOffsets)

    init {
        controller.addMover(roomBounds)
        controller.addMover(transition)
        controller.addMover(before)
        controller.addMover(follow)
        controller.addMover(after)
        controller.addMover(stage)
        controller.addMover(CameraRoomClamp(view, stage))
        controller.addMover(CameraRoomScan(roomBounds))
        controller.addEffect(drawOffset)
        drawOffset.source = IDrawOffsetSource { _, out -> out.set(focus.drawOffset) }
        follow.setFocus(camera, focus.also { roomBounds.subject = it })
        controller.reset()
        controller.update(0.1f)
        controller.update(0.1f)
    }
}

class CameraDrawOffsetTest : DescribeSpec({

    describe("CameraDrawOffset") {
        it("is the focus draw offset in an open room") {
            val rig = DrawOffsetRig()
            rig.drawOffset.offset.x shouldBe (-2f plusOrMinus 1e-4f)
        }

        it("is 0 on an axis that the room pins") {
            val rig = DrawOffsetRig()
            rig.drawOffset.offset.y shouldBe 0f
        }

        it("is 0 at a room edge") {
            val rig = DrawOffsetRig()
            rig.focus.x = 20f
            rig.controller.update(0.1f)
            rig.camera.position.x shouldBe 50f
            rig.drawOffset.offset.x shouldBe 0f
        }

        it("is only the part inside the room near a room edge") {
            val rig = DrawOffsetRig()
            rig.focus.x = 51f
            rig.controller.update(0.1f)
            rig.drawOffset.offset.x shouldBe (-1f plusOrMinus 1e-4f)
        }

        it("is 0 when a mover before the follow holds the camera") {
            val rig = DrawOffsetRig()
            rig.before.hold = true
            rig.controller.update(0.1f)
            rig.drawOffset.offset.x shouldBe 0f
        }

        it("is 0 when a mover after the follow moves that axis") {
            val rig = DrawOffsetRig()
            rig.after.moveX = 5f
            rig.controller.update(0.1f)
            rig.drawOffset.offset.x shouldBe 0f
        }

        it("is 0 in smooth follow") {
            val rig = DrawOffsetRig()
            rig.follow.startSmoothFollow()
            rig.controller.update(0.1f)
            rig.drawOffset.offset.x shouldBe 0f
        }

        it("clears the follow record each frame") {
            val rig = DrawOffsetRig()
            rig.follow.followedX.isNaN() shouldBe true
            rig.follow.followedY.isNaN() shouldBe true
        }
    }
})
