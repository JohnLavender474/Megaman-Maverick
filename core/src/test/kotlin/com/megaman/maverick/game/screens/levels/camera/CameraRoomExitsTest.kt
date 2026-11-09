package com.megaman.maverick.game.screens.levels.camera

import com.maverick.game.engine.camera.CameraRoomBounds
import com.maverick.game.engine.camera.IRoomSubject
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array as GdxArray
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.playerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.CameraEdgeBlock
import com.megaman.maverick.game.entities.state.CameraSubjectComponent
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestHealthPlayer
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

private fun room(name: String, x: Float, y: Float, w: Float, h: Float) =
    RectangleMapObject(x, y, w, h).also { it.name = name }

private class ExitsRig(var on: Boolean = true) {
    val engine = mockk<GameEngine>(relaxed = true)
    val players = MegaPlayers(2)
    val services = testServices(engine = engine, players = players)
    val first = player(0, 50f, 20f)
    val second = player(1, 60f, 20f)
    val held = BooleanArray(2)

    val start = room("start", 0f, 0f, 100f, 80f)
    val next = room("next", 100f, 0f, 300f, 80f)
    val camera = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val follow = CameraFollow(1f, { 1f })
    val transition = CameraRoomTransition(20f, 0.1f, 0.1f)
    val roomBounds = CameraRoomBounds(follow, transition, Vector2(50f, 50f)).also {
        it.gameRooms = GdxArray.with(start, next)
    }
    val exits = CameraRoomExits(players, roomBounds, { on }) { held[it.playerSlot!!.index] }

    init {
        MegaEntityFactory.init(services)
        follow.setFocus(camera, object : IFocusable, IRoomSubject {
            override fun getSubjectBounds() = GameRectangle(first.body)
            override fun getFocusPosition() = first.body.getCenter(Vector2())
        }.also { roomBounds.subject = it })
        roomBounds.reset()
        roomBounds.update(camera, 0f)
    }

    private fun player(slot: Int, x: Float, y: Float): TestHealthPlayer {
        val player = TestHealthPlayer(services)
        player.body.setSize(10f, 10f).setCenter(x, y)
        player.addComponent(CameraSubjectComponent({ GameRectangle(player.body) }, { player.body.getCenter(Vector2()) }))
        players.register(PlayerSlot(slot), player)
        player.existenceState = EntityExistenceState.LIVE
        return player
    }

    fun atRightExit(player: TestHealthPlayer) {
        player.body.setX(100f - player.body.getWidth())
    }

    fun update() = exits.update(camera, 0.1f)
}

class CameraRoomExitsTest : DescribeSpec({

    afterEach { MegaEntityFactory.reset() }

    describe("findSideExits") {
        val out = GdxArray<SideExit>()

        it("finds a room that touches the right edge, with the shared span") {
            val a = room("a", 0f, 0f, 100f, 80f)
            val b = room("b", 100f, 40f, 100f, 200f)
            findSideExits(a, GdxArray.with(a, b), out) shouldBe 1
            out[0].side shouldBe Direction.RIGHT
            out[0].next shouldBe b
            out[0].edge shouldBe 100f
            out[0].low shouldBe 40f
            out[0].high shouldBe 80f
        }

        it("finds a room that touches the left edge") {
            val a = room("a", 100f, 0f, 100f, 80f)
            val b = room("b", 0f, 0f, 100f, 80f)
            findSideExits(a, GdxArray.with(a, b), out) shouldBe 1
            out[0].side shouldBe Direction.LEFT
            out[0].edge shouldBe 100f
        }

        it("finds no exit across a gap or with no shared span") {
            val a = room("a", 0f, 0f, 100f, 80f)
            val gap = room("gap", 164f, 0f, 100f, 80f)
            val above = room("above", 100f, 80f, 100f, 80f)
            findSideExits(a, GdxArray.with(a, gap, above), out) shouldBe 0
        }

        it("finds two rooms on one side") {
            val a = room("a", 0f, 0f, 100f, 160f)
            val low = room("low", 100f, 0f, 100f, 80f)
            val high = room("high", 100f, 80f, 100f, 80f)
            findSideExits(a, GdxArray.with(a, low, high), out) shouldBe 2
            out[0].next shouldBe low
            out[1].next shouldBe high
        }
    }

    describe("CameraRoomExits") {
        it("puts a wall outside the exit with 2 live players") {
            val rig = ExitsRig()
            val props = slot<Properties>()
            rig.update()
            verify(exactly = 1) { rig.engine.spawn(any<CameraEdgeBlock>(), capture(props)) }
            val bounds = props.captured.get(ConstKeys.BOUNDS, GameRectangle::class)!!
            bounds.getX() shouldBe 100f
            bounds.getY() shouldBe 0f
            bounds.getHeight() shouldBe 80f
        }

        it("puts no wall with 1 live player, and none when off") {
            val rig = ExitsRig()
            rig.second.existenceState = EntityExistenceState.UNSPAWNED
            rig.update()
            rig.second.existenceState = EntityExistenceState.LIVE
            rig.held[1] = true
            rig.update()
            rig.held[1] = false
            rig.on = false
            rig.update()
            verify(exactly = 0) { rig.engine.spawn(any<CameraEdgeBlock>(), any()) }
        }

        it("puts no wall on an exit that a gate covers") {
            val rig = ExitsRig()
            rig.exits.gates.add(Rectangle(84f, 0f, 32f, 48f))
            rig.update()
            verify(exactly = 0) { rig.engine.spawn(any<CameraEdgeBlock>(), any()) }
        }

        it("starts the transition only when every live player is at the exit") {
            val rig = ExitsRig()
            rig.atRightExit(rig.second)
            rig.update()
            rig.transition.transitioning shouldBe false

            rig.atRightExit(rig.first)
            rig.update()
            rig.transition.transitioning shouldBe true
            rig.roomBounds.currentGameRoom shouldBe rig.next
        }

        it("counts a player past the edge as at the exit") {
            val rig = ExitsRig()
            rig.second.body.setX(130f)
            rig.atRightExit(rig.first)
            rig.update()
            rig.transition.transitioning shouldBe true
        }

        it("leaves a held player out: one live player gets no wall, and the wall comes back after") {
            val rig = ExitsRig()
            rig.atRightExit(rig.first)
            rig.held[1] = true
            rig.update()
            rig.transition.transitioning shouldBe false

            rig.held[1] = false
            clearMocks(rig.engine, answers = false)
            rig.update()
            verify(exactly = 1) { rig.engine.spawn(any<CameraEdgeBlock>(), any()) }
        }

        it("does not start when a player at the exit is outside the shared span") {
            val rig = ExitsRig()
            rig.atRightExit(rig.first)
            rig.atRightExit(rig.second)
            rig.second.body.setCenter(rig.second.body.getCenter(Vector2()).x, 200f)
            rig.update()
            rig.transition.transitioning shouldBe false
        }
    }
})
