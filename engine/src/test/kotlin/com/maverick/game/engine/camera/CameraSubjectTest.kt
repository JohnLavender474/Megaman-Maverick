package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.world.IDrawOffsets
import com.maverick.game.engine.world.body.IBody
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class CameraSubjectTest : DescribeSpec({

    describe("CameraSubject") {
        it("sets the follow target, the room subject and the draw offset source together") {
            val camera = OrthographicCamera().also {
                it.viewportWidth = 100f
                it.viewportHeight = 80f
            }
            val view = CameraView(camera, CameraRotation())
            val follow = CameraFollow(1f, { 1f })
            val roomBounds = CameraRoomBounds(follow, CameraRoomTransition(20f, 0.1f, 0.1f), Vector2(50f, 50f))
            val offsets = object : IDrawOffsets {
                override fun getDrawOffset(body: IBody, out: Vector2): Vector2 = out.setZero()
            }
            val drawOffset = CameraDrawOffset(
                view, follow, CameraLimitStage(view, follow, roomBounds, Vector3(), 100f), Vector3(), offsets
            )
            val target = object : IFocusable {
                override fun getFocusPosition() = Vector2(30f, 40f)
            }
            val room = object : IRoomSubject {
                override fun getSubjectBounds() = GameRectangle(0f, 0f, 10f, 10f)
            }
            val draw = IDrawOffsetSource { _, out -> out.set(1f, 2f) }

            CameraSubject(camera, follow, roomBounds, drawOffset).set(target, room, draw)

            follow.focus shouldBeSameInstanceAs target
            roomBounds.subject shouldBeSameInstanceAs room
            drawOffset.source shouldBeSameInstanceAs draw
            camera.position.x shouldBe 30f
        }
    }
})
