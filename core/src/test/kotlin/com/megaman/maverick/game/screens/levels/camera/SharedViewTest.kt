package com.megaman.maverick.game.screens.levels.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array as GdxArray
import com.maverick.game.engine.camera.CameraController
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.CameraEdgeBlock
import com.megaman.maverick.game.entities.currentHealth
import com.megaman.maverick.game.entities.hazards.ViewEdgeHazard
import com.megaman.maverick.game.entities.state.FocusComponent
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestHealthPlayer
import com.megaman.maverick.game.state.ViewEdgeRule
import com.megaman.maverick.game.state.ViewEdgeRules
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.verify

private class Session(engine: GameEngine = mockk(relaxed = true)) {
    val players = MegaPlayers(2)
    val services = testServices(engine = engine, players = players)
    val first = player(0, 100f, 20f)
    val second = player(1, 300f, 60f)

    private fun player(slot: Int, x: Float, y: Float): TestHealthPlayer {
        val player = TestHealthPlayer(services)
        player.body.setSize(10f, 10f).setCenter(x, y)
        player.addComponent(FocusComponent({ GameRectangle(player.body) }, { player.body.getCenter(Vector2()) }))
        players.register(PlayerSlot(slot), player)
        player.existenceState = EntityExistenceState.LIVE
        return player
    }
}

private class EdgesRig(val rules: ViewEdgeRules, var on: Boolean = true) {
    val engine = mockk<GameEngine>(relaxed = true)
    val session = Session(engine)
    val camera = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val view = CameraView(camera, CameraRotation())
    val center = Vector3(200f, 40f, 0f)
    val room = Rectangle(0f, -200f, 1000f, 400f)
    val edges = CameraViewEdges(
        view, center, session.players, rules, ViewEdgeHazard(session.services).also { it.damage = rules.damage }, { room }
    ) { on }

    fun update(delta: Float = 0.1f) = edges.update(camera, delta)
}

private class TallRig {
    val session = Session()
    val camera = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val rotation = CameraRotation()
    val view = CameraView(camera, rotation)
    val follow = CameraFollow(1f, { 1f })
    val transition = CameraRoomTransition(follow, 20f, 0.1f, 0.1f)
    val room = RectangleMapObject(0f, 0f, 100f, 400f).also { it.name = "tall" }
    val roomBounds = CameraRoomBounds(follow, transition, Vector2(50f, 50f)).also {
        it.gameRooms = GdxArray.with(room)
    }
    val controller = CameraController(camera)
    val stage = CameraLimitStage(view, follow, roomBounds, controller.settledPosition, 100000f)
    val edges = CameraViewEdges(
        view, controller.settledPosition, session.players,
        ViewEdgeRules(bottom = ViewEdgeRule.WARP, delay = 100f),
        ViewEdgeHazard(session.services), { room.rectangle }
    ) { true }
    val focus = PlayersFocus(session.players, view, ViewEdgeRules(bottom = ViewEdgeRule.WARP)) { edges.isHeld(it) }

    init {
        session.first.body.setCenter(50f, 10f)
        session.second.body.setCenter(50f, 10f)
        controller.addMover(roomBounds)
        controller.addMover(transition)
        controller.addMover(follow)
        controller.addMover(stage)
        controller.addMover(CameraRoomClamp(view, stage))
        controller.addEffect(focus)
        controller.addEffect(edges)
        follow.setFocus(camera, focus)
        controller.reset()
        controller.update(1f / 60f)
    }

    fun inView(player: TestHealthPlayer): Boolean {
        val c = player.body.getCenter(Vector2())
        return c.y > camera.position.y - 40f && c.y < camera.position.y + 40f
    }
}

class SharedViewTest : DescribeSpec({

    describe("a climber in a tall room while the low player is held") {
        it("stays in view on every frame, and the low player is held") {
            val rig = TallRig()
            repeat(240) {
                rig.session.second.body.translate(0f, 1.5f)
                rig.controller.update(1f / 60f)
                rig.inView(rig.session.second) shouldBe true
            }
            rig.edges.isHeld(rig.session.first) shouldBe true
        }
    }

    describe("PlayersFocus") {
        it("gives the mean of the players in the world") {
            val session = Session()
            val focus = PlayersFocus(session.players)
            focus.getFocusPosition() shouldBe Vector2(200f, 40f)
        }

        it("leaves out a player that is not in the world") {
            val session = Session()
            session.second.existenceState = EntityExistenceState.UNSPAWNED
            val focus = PlayersFocus(session.players)
            focus.getFocusPosition() shouldBe Vector2(100f, 20f)
        }

        it("gives slot 0 when no player is in the world") {
            val session = Session()
            session.first.existenceState = EntityExistenceState.UNSPAWNED
            session.second.existenceState = EntityExistenceState.UNSPAWNED
            val focus = PlayersFocus(session.players)
            focus.getFocusPosition() shouldBe Vector2(100f, 20f)
        }

        it("gives the bounds of slot 0, so the room rules keep slot 0") {
            val session = Session()
            val focus = PlayersFocus(session.players)
            focus.getFocusBounds().getCenter(Vector2()) shouldBe Vector2(100f, 20f)
        }

        it("eases from the old midpoint to the new one over the ease time when the group changes") {
            val session = Session()
            val focus = PlayersFocus(session.players)
            val camera = OrthographicCamera()
            focus.getFocusPosition() shouldBe Vector2(200f, 40f)

            session.second.existenceState = EntityExistenceState.UNSPAWNED
            focus.getFocusPosition() shouldBe Vector2(200f, 40f)

            focus.update(camera, PlayersFocus.EASE_DURATION / 2f)
            val half = focus.getFocusPosition()
            half.x shouldBe (150f).plusOrMinus(0.01f)
            half.y shouldBe (30f).plusOrMinus(0.01f)

            focus.update(camera, PlayersFocus.EASE_DURATION / 2f)
            focus.getFocusPosition() shouldBe Vector2(100f, 20f)
        }

        it("leaves out an excluded player, so the camera follows the others") {
            val session = Session()
            val focus = PlayersFocus(session.players) { it == session.second }
            focus.getFocusPosition() shouldBe Vector2(100f, 20f)
        }

        it("eases when the midpoint jumps by more than a tile in one read") {
            val session = Session()
            val focus = PlayersFocus(session.players)
            focus.getFocusPosition() shouldBe Vector2(200f, 40f)

            session.second.body.setCenter(500f, 60f)
            focus.getFocusPosition() shouldBe Vector2(200f, 40f)

            focus.update(OrthographicCamera(), PlayersFocus.EASE_DURATION)
            focus.getFocusPosition() shouldBe Vector2(300f, 40f)
        }

        it("keeps the player far from a leave edge in view when the players are more than a view apart") {
            val session = Session()
            val view = CameraView(OrthographicCamera().also {
                it.viewportWidth = 100f
                it.viewportHeight = 80f
            }, CameraRotation())
            val focus = PlayersFocus(session.players, view, ViewEdgeRules(bottom = ViewEdgeRule.WARP))
            session.first.body.setCenter(100f, 0f)
            session.second.body.setCenter(100f, 200f)
            focus.reset()

            val y = focus.getFocusPosition().y
            (y + 40f >= 205f) shouldBe true
            (y - 40f <= 195f) shouldBe true
        }

        it("keeps the same player on an axis with a leave edge on both sides") {
            val session = Session()
            val view = CameraView(OrthographicCamera().also {
                it.viewportWidth = 100f
                it.viewportHeight = 80f
            }, CameraRotation())
            val rules = ViewEdgeRules(left = ViewEdgeRule.WARP, right = ViewEdgeRule.WARP)
            val focus = PlayersFocus(session.players, view, rules)
            session.first.body.setCenter(170f, 40f)
            session.second.body.setCenter(250f, 40f)
            focus.reset()
            focus.getFocusPosition()

            session.first.body.setCenter(0f, 40f)
            session.second.body.setCenter(260f, 40f)
            val x = focus.getFocusPosition().x
            (x - 50f <= -5f) shouldBe true
        }

        it("does not ease after a reset") {
            val session = Session()
            val focus = PlayersFocus(session.players)
            focus.getFocusPosition()

            focus.reset()
            session.second.existenceState = EntityExistenceState.UNSPAWNED
            focus.getFocusPosition() shouldBe Vector2(100f, 20f)
        }
    }

    describe("viewEdgeInsideRoom") {
        val room = Rectangle(0f, 0f, 400f, 80f)

        it("gives a view edge that is inside the room") {
            viewEdgeInsideRoom(Vector3(150f, 40f, 0f), 100f, 80f, room, Direction.LEFT) shouldBe 100f
            viewEdgeInsideRoom(Vector3(150f, 40f, 0f), 100f, 80f, room, Direction.RIGHT) shouldBe 200f
        }

        it("gives nothing at a room edge, so the room rules act there") {
            viewEdgeInsideRoom(Vector3(50f, 40f, 0f), 100f, 80f, room, Direction.LEFT).shouldBeNull()
            viewEdgeInsideRoom(Vector3(350f, 40f, 0f), 100f, 80f, room, Direction.RIGHT).shouldBeNull()
            viewEdgeInsideRoom(Vector3(150f, 40f, 0f), 100f, 80f, room, Direction.UP).shouldBeNull()
            viewEdgeInsideRoom(Vector3(150f, 40f, 0f), 100f, 80f, room, Direction.DOWN).shouldBeNull()
        }
    }

    describe("CameraViewEdges") {
        val noWalls = ViewEdgeRules(left = ViewEdgeRule.NONE, right = ViewEdgeRule.NONE)

        it("kills a player only after the delay out of view") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.KILL, delay = 0.25f))
            rig.session.second.body.setCenter(400f, 40f)

            rig.update()
            rig.update()
            rig.session.second.currentHealth shouldNotBe 0
            rig.update()
            rig.session.second.currentHealth shouldBe 0
            rig.session.first.currentHealth shouldNotBe 0
        }

        it("starts the delay again when the player comes back into view") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.KILL, delay = 0.25f))
            rig.session.second.body.setCenter(400f, 40f)
            rig.update()
            rig.update()

            rig.session.second.body.setCenter(240f, 40f)
            rig.update()
            rig.session.second.body.setCenter(400f, 40f)
            rig.update()
            rig.update()
            rig.session.second.currentHealth shouldNotBe 0
        }

        it("warps a player beside a partner in view, toward the view center, with the damage") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.WARP, damage = 3))
            val health = rig.session.second.currentHealth
            rig.session.first.body.setCenter(200f, 40f)
            rig.session.second.body.setCenter(400f, 40f)

            rig.update()

            rig.session.second.body.getCenter(Vector2()) shouldBe Vector2(232f, 40f)
            rig.session.second.currentHealth shouldBe health - 3
            rig.edges.isHeld(rig.session.second) shouldBe false
        }

        it("holds an out player in place until the delay ends, then warps it") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.WARP, delay = 0.25f))
            rig.session.first.body.setCenter(200f, 40f)
            rig.session.second.body.setCenter(400f, 40f)

            rig.update()
            rig.edges.isHeld(rig.session.second) shouldBe true
            rig.session.second.body.physics.gravityOn shouldBe false
            rig.session.second.body.setCenter(400f, 0f)
            rig.update()
            rig.session.second.body.getCenter(Vector2()) shouldBe Vector2(400f, 40f)

            rig.update()
            rig.edges.isHeld(rig.session.second) shouldBe false
            rig.session.second.body.physics.gravityOn shouldBe true
            rig.session.second.body.getCenter(Vector2()) shouldBe Vector2(232f, 40f)
        }

        it("releases a held player where it is when the rules turn off") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.WARP, delay = 5f))
            rig.session.first.body.setCenter(200f, 40f)
            rig.session.second.body.setCenter(400f, 40f)
            rig.update()

            rig.on = false
            rig.update()
            rig.edges.isHeld(rig.session.second) shouldBe false
            rig.session.second.body.physics.collisionOn shouldBe true
            rig.session.second.body.getCenter(Vector2()) shouldBe Vector2(400f, 40f)
        }

        it("bounces a player at the bottom edge back up, with the damage once, then iframes") {
            val rig = EdgesRig(noWalls.copy(bottom = ViewEdgeRule.BOUNCE, damage = 2))
            val health = rig.session.second.currentHealth
            rig.session.second.body.setCenter(240f, -2f)

            rig.update()
            rig.session.second.body.getY() shouldBe 0f
            (rig.session.second.body.physics.velocity.y > 0f) shouldBe true

            rig.session.second.body.setY(-5f)
            rig.update()
            rig.session.second.currentHealth shouldBe health - 2
        }

        it("puts no wall while a player is outside it, so no player is locked out") {
            val rig = EdgesRig(ViewEdgeRules())
            MegaEntityFactory.init(rig.session.services)
            try {
                rig.session.second.body.setCenter(400f, 40f)
                rig.update()
                verify(exactly = 0) { rig.engine.spawn(any<CameraEdgeBlock>(), any()) }

                rig.session.second.body.setCenter(240f, 40f)
                rig.update()
                verify(exactly = 1) { rig.engine.spawn(any<CameraEdgeBlock>(), any()) }
            } finally {
                MegaEntityFactory.reset()
            }
        }

        it("holds only one of two players out at the same edge, and warps nobody onto an out partner") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.WARP))
            rig.session.first.body.setCenter(420f, 40f)
            rig.session.second.body.setCenter(400f, 40f)
            rig.update()
            rig.edges.isHeld(rig.session.first) shouldBe true
            rig.edges.isHeld(rig.session.second) shouldBe false
            rig.session.first.body.getCenter(Vector2()) shouldBe Vector2(420f, 40f)
            rig.session.first.currentHealth shouldNotBe 0
            rig.session.second.currentHealth shouldNotBe 0
        }

        it("kills and releases a held player when its partner is no longer live") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.WARP, delay = 5f))
            rig.session.first.body.setCenter(200f, 40f)
            rig.session.second.body.setCenter(400f, 40f)
            rig.update()
            rig.edges.isHeld(rig.session.second) shouldBe true
            rig.session.second.currentHealth shouldNotBe 0

            rig.session.first.existenceState = EntityExistenceState.UNSPAWNED
            rig.update()
            rig.edges.isHeld(rig.session.second) shouldBe false
            rig.session.second.currentHealth shouldBe 0
        }

        it("does not hold, or kill, a player that is alone past a warp edge") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.WARP, delay = 5f))
            rig.session.first.existenceState = EntityExistenceState.UNSPAWNED
            rig.session.second.body.setCenter(400f, 40f)
            rig.update()
            rig.update()
            rig.edges.isHeld(rig.session.second) shouldBe false
            rig.session.second.currentHealth shouldNotBe 0
        }

        it("keeps a held player alive while its partner is live, also out of view") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.WARP, top = ViewEdgeRule.NONE, delay = 5f))
            rig.session.first.body.setCenter(200f, 40f)
            rig.session.second.body.setCenter(400f, 40f)
            rig.update()
            rig.session.first.body.setCenter(200f, 300f)
            rig.update()
            rig.edges.isHeld(rig.session.second) shouldBe true
            rig.session.second.currentHealth shouldNotBe 0
        }

        it("does nothing when it is off") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.KILL), on = false)
            rig.session.second.body.setCenter(400f, 40f)
            rig.update()
            rig.session.second.currentHealth shouldNotBe 0
        }

        it("does nothing for a player out of view past a room edge") {
            val rig = EdgesRig(noWalls.copy(right = ViewEdgeRule.KILL))
            rig.room.set(0f, -200f, 250f, 400f)
            rig.session.second.body.setCenter(400f, 40f)
            rig.update()
            rig.session.second.currentHealth shouldNotBe 0
        }
    }
})
