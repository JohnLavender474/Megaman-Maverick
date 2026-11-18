package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private class Subject : IFocusable, IRoomSubject {
    override fun getSubjectBounds() = GameRectangle().setSize(10f, 10f).setCenter(50f, 40f)
    override fun getFocusPosition() = Vector2(50f, 40f)
}

private fun room(name: String, x: Float, y: Float) = RectangleMapObject(x, y, 100f, 80f).also { it.name = name }

class CameraRoomBoundsTest : DescribeSpec({

    fun boundsWith(vararg rooms: RectangleMapObject): Pair<CameraRoomBounds, OrthographicCamera> {
        val camera = OrthographicCamera().also {
            it.viewportWidth = 100f
            it.viewportHeight = 80f
        }
        val follow = CameraFollow(1f, { 1f })
        val bounds = CameraRoomBounds(follow, CameraRoomTransition(20f, 0f, 0.01f), Vector2(50f, 50f))
        bounds.gameRooms = Array.with(*rooms)
        follow.setFocus(camera, Subject().also { bounds.subject = it })
        bounds.reset()
        bounds.update(camera, 0f)
        return bounds to camera
    }

    describe("transitionToRoom with axisOnly") {
        it("throws, naming the room, when the direction from the room centers is vertical") {
            val (bounds, camera) = boundsWith(room("start", 0f, 0f), room("above", 0f, 80f))

            val error = shouldThrow<IllegalArgumentException> {
                bounds.transitionToRoom(camera, "above", axisOnly = true)
            }

            error.message!! shouldContain "above"
            error.message!! shouldContain "room centers"
            bounds.currentGameRoom!!.name shouldBe "start"
        }

        it("starts a horizontal axis-only transition") {
            val (bounds, camera) = boundsWith(room("start", 0f, 0f), room("right", 100f, 0f))

            bounds.transitionToRoom(camera, "right", Direction.RIGHT, axisOnly = true) shouldBe true
            bounds.currentGameRoom!!.name shouldBe "right"
        }
    }
})
