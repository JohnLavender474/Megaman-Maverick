package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.camera.CameraController
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraLimits
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.CameraShake
import com.maverick.game.engine.camera.ICameraLimitRule
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private class LimitFocus(var x: Float, var y: Float) : IFocusable {
    override fun getFocusBounds() = GameRectangle().setSize(10f, 10f).setCenter(x, y)
    override fun getFocusPosition() = Vector2(x, y)
}

private class MaxX(var value: Float? = null) : ICameraLimitRule {
    override fun apply(target: CameraLimits) {
        value?.let { target.x.set(target.x.min, it) }
    }
}

private class LimitRig(direction: Direction = Direction.UP, withZones: Boolean = false) {
    val camera = RotatableCamera(printDebug = false).also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
        it.immediateRotation(direction)
    }
    val focus = LimitFocus(50f, 40f)
    val follow = CameraFollow(1f, { 1f })
    val transition = CameraRoomTransition(follow, 20f, 0.1f, 0.1f)
    val roomBounds = CameraRoomBounds(follow, transition, Vector2(50f, 50f)).also {
        it.gameRooms = Array.with(
            RectangleMapObject(0f, 0f, 400f, 80f).also { room -> room.name = "a" },
            RectangleMapObject(400f, 0f, 200f, 80f).also { room -> room.name = "b" }
        )
    }
    val rule = MaxX()
    val shake = CameraShake(1f)
    var held = false
    val controller = CameraController(camera)
    val stage = CameraLimitStage(follow, roomBounds, controller.settledPosition, 100f, { held }).also {
        it.addRule(rule)
        if (withZones) it.addRule(CameraZones(follow))
    }

    init {
        controller.addMover(roomBounds)
        controller.addMover(transition)
        controller.addMover(follow)
        controller.addMover(stage)
        controller.addMover(CameraRoomClamp(stage))
        controller.addMover(CameraRoomScan(roomBounds))
        controller.addEffect(shake)
        follow.setFocus(camera, focus)
        controller.reset()
        controller.update(0.1f)
        controller.update(0.1f)
    }
}

class CameraLimitStageTest : DescribeSpec({

    describe("CameraLimitStage") {
        it("takes the room with no rule") {
            val rig = LimitRig()
            rig.stage.limits.x.max shouldBe 400f
        }

        it("eases to a rule's target when the focus stays in view") {
            val rig = LimitRig()
            rig.rule.value = 300f
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 300f
            rig.rule.value = 110f
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 110f
            rig.rule.value = 300f
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 120f
        }

        it("snaps when the focus is out of view after the move") {
            val rig = LimitRig()
            rig.rule.value = 200f
            rig.controller.update(0.1f)
            rig.rule.value = 390f
            rig.focus.x = 330f
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 390f
            rig.camera.position.x shouldBe 330f
        }

        it("snaps on a reset") {
            val rig = LimitRig()
            rig.rule.value = 120f
            rig.controller.update(0.1f)
            rig.rule.value = 300f
            rig.controller.reset()
            rig.controller.update(0.1f)
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 300f
        }

        it("does not ease during a hold") {
            val rig = LimitRig()
            rig.held = true
            rig.rule.value = 120f
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 400f
            rig.held = false
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 120f
        }

        it("never goes past the room, and reads the view size of a rotated camera") {
            val rig = LimitRig(Direction.LEFT)
            rig.rule.value = 900f
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 400f
            rig.camera.viewWidth shouldBe 80f
            rig.camera.viewHeight shouldBe 100f
        }

        it("does not read a shaken position") {
            val rig = LimitRig()
            rig.rule.value = 110f
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 110f
            rig.rule.value = 120f
            rig.shake.startShake(1f, 0.05f, 30f, 0f)
            rig.controller.update(0.1f)
            rig.controller.update(0.1f)
            rig.stage.limits.x.max shouldBe 120f
        }

        it("snaps on a room change") {
            val rig = LimitRig()
            rig.rule.value = 500f
            rig.focus.x = 397f
            rig.controller.update(0.01f)
            while (rig.transition.transitioning) rig.controller.update(0.01f)
            rig.controller.update(0.01f)
            rig.stage.limits.x.min shouldBe 400f
            rig.stage.limits.x.max shouldBe 500f
        }

        it("gives the same result with a zone part with no zones as with no zone part") {
            val without = LimitRig()
            val with = LimitRig(withZones = true)
            for (x in floatArrayOf(50f, 120f, 300f, 395f, 300f, 397f, 450f, 500f, 395f)) {
                without.focus.x = x
                with.focus.x = x
                repeat(60) {
                    without.controller.update(0.02f)
                    with.controller.update(0.02f)
                    with.camera.position.x shouldBe without.camera.position.x
                    with.camera.position.y shouldBe without.camera.position.y
                    with.stage.limits.x.min shouldBe without.stage.limits.x.min
                    with.stage.limits.x.max shouldBe without.stage.limits.x.max
                }
            }
        }
    }
})
