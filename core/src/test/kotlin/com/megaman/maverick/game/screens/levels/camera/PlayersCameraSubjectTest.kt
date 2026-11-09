package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.CameraSubject
import com.maverick.game.engine.camera.CameraDrawOffset
import com.maverick.game.engine.camera.CameraLimitStage
import com.maverick.game.engine.camera.CameraRoomBounds
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.IDrawOffsets
import com.maverick.game.engine.world.body.IBody
import com.megaman.maverick.game.entities.state.CameraSubjectComponent
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestHealthPlayer
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private class SubjectRig(slots: Int) {
    val players = MegaPlayers(2)
    val services = testServices(players = players)
    val focusComponents = (0 until slots).map { slot ->
        val player = TestHealthPlayer(services)
        player.body.setSize(10f, 10f).setCenter(100f + 200f * slot, 20f)
        val focus = CameraSubjectComponent({ GameRectangle(player.body) }, { player.body.getCenter(Vector2()) })
        player.addComponent(focus)
        players.register(PlayerSlot(slot), player)
        player.existenceState = EntityExistenceState.LIVE
        focus
    }
    val camera = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val view = CameraView(camera, CameraRotation())
    val follow = CameraFollow(1f, { 1f })
    val roomBounds = CameraRoomBounds(follow, CameraRoomTransition(20f, 0.1f, 0.1f), Vector2(50f, 50f))
    val offsets = object : IDrawOffsets {
        override fun getDrawOffset(body: IBody, out: Vector2): Vector2 =
            if (body === players.get(PlayerSlot(0))!!.body) out.set(-2f, 0f) else out.set(-4f, 0f)
    }
    val drawOffset = CameraDrawOffset(
        view, follow, CameraLimitStage(view, follow, roomBounds, Vector3(), 100f), Vector3(), offsets
    )
    val playersFocus = if (slots > 1) PlayersFocus(players) else null
    val subject = CameraSubject(camera, follow, roomBounds, drawOffset)
    val policy = PlayersCameraSubject(players, playersFocus)
}

class PlayersCameraSubjectTest : DescribeSpec({

    describe("PlayersCameraSubject") {
        it("gives slot 0 as the follow target, the room subject and the draw offset with 1 player") {
            val rig = SubjectRig(1)
            rig.policy.applyTo(rig.subject)
            rig.follow.focus shouldBeSameInstanceAs rig.focusComponents[0]
            rig.roomBounds.subject shouldBeSameInstanceAs rig.focusComponents[0]
            rig.drawOffset.source!!.getDrawOffset(rig.offsets, Vector2()) shouldBe Vector2(-2f, 0f)
        }

        it("follows the group with 2 players, and keeps slot 0 as the room subject") {
            val rig = SubjectRig(2)
            rig.policy.applyTo(rig.subject)
            rig.follow.focus shouldBeSameInstanceAs rig.playersFocus
            rig.roomBounds.subject shouldBeSameInstanceAs rig.focusComponents[0]
            rig.drawOffset.source shouldBeSameInstanceAs rig.playersFocus
        }
    }
})
