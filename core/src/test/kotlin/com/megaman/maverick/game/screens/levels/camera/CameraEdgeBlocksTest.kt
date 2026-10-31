package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.camera.CameraController
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraLimits
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.ICameraLimitRule
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private class EdgeFocus(var x: Float, var y: Float) : IFocusable {
    override fun getFocusBounds() = GameRectangle().setSize(10f, 10f).setCenter(x, y)
    override fun getFocusPosition() = Vector2(x, y)
}

private class EdgeRule(val maxX: Float?, val minX: Float?, val edge: Direction) : ICameraLimitRule {
    override fun apply(target: CameraLimits) {
        maxX?.let { target.x.setMax(it) }
        minX?.let { target.x.setMin(it) }
        target.blockedEdges.add(edge)
    }
}

private class EdgeRig(rule: ICameraLimitRule) {
    val camera = RotatableCamera(printDebug = false).also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val follow = CameraFollow(1f, { 1f })
    val transition = CameraRoomTransition(follow, 20f, 0.1f, 0.1f)
    val roomBounds = CameraRoomBounds(follow, transition, Vector2(50f, 50f)).also {
        it.gameRooms = Array.with(RectangleMapObject(0f, 0f, 400f, 80f).also { room -> room.name = "a" })
    }
    val controller = CameraController(camera)
    val stage = CameraLimitStage(follow, roomBounds, controller.settledPosition, 100000f).also { it.addRule(rule) }

    init {
        controller.addMover(roomBounds)
        controller.addMover(transition)
        controller.addMover(follow)
        controller.addMover(stage)
        controller.addMover(CameraRoomClamp(stage))
        follow.setFocus(camera, EdgeFocus(150f, 40f))
        controller.reset()
        repeat(3) { controller.update(0.1f) }
    }
}

class CameraEdgeBlocksTest : DescribeSpec({

    describe("CameraLimitStage.edgeLimit") {
        it("gives the limit of an edge that a rule blocks") {
            EdgeRig(EdgeRule(200f, null, Direction.RIGHT)).stage.edgeLimit(Direction.RIGHT) shouldBe 200f
            EdgeRig(EdgeRule(null, 50f, Direction.LEFT)).stage.edgeLimit(Direction.LEFT) shouldBe 50f
        }

        it("gives nothing for an edge that no rule blocks") {
            EdgeRig(EdgeRule(200f, null, Direction.RIGHT)).stage.edgeLimit(Direction.LEFT).shouldBeNull()
        }

        it("gives nothing at a room edge, so that the player can reach the next room") {
            EdgeRig(EdgeRule(400f, null, Direction.RIGHT)).stage.edgeLimit(Direction.RIGHT).shouldBeNull()
            EdgeRig(EdgeRule(900f, null, Direction.RIGHT)).stage.edgeLimit(Direction.RIGHT).shouldBeNull()
        }
    }

    describe("cameraEdgeBlockBounds") {
        fun bounds(direction: Direction) =
            cameraEdgeBlockBounds(GameRectangle(), direction, 200f, 100f, 40f, 100f, 80f, 10f)

        it("places a block outside the view, with its inner side on the edge") {
            bounds(Direction.RIGHT).getX() shouldBe 200f
            bounds(Direction.LEFT).getMaxX() shouldBe 200f
            bounds(Direction.UP).getY() shouldBe 200f
            bounds(Direction.DOWN).getMaxY() shouldBe 200f
        }

        it("covers the whole view edge") {
            bounds(Direction.RIGHT).getHeight() shouldBe 100f
            bounds(Direction.UP).getWidth() shouldBe 120f
        }
    }
})
