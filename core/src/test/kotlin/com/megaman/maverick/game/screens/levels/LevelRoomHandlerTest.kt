package com.megaman.maverick.game.screens.levels

import com.maverick.game.engine.camera.IRoomSubject
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.camera.CameraController
import com.maverick.game.engine.camera.CameraFollow
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.camera.IFocusable
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.GravitySwitchAura
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.RoomTransitionEventKeys
import com.megaman.maverick.game.players.SecondPlayerEntity
import com.maverick.game.engine.camera.CameraLimitStage
import com.maverick.game.engine.camera.CameraRoomBounds
import com.megaman.maverick.game.screens.levels.camera.CameraRoomClamp
import com.megaman.maverick.game.screens.levels.camera.CameraRoomScan
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject

private class Focus : IFocusable, IRoomSubject {
    override fun getSubjectBounds() = GameRectangle().setSize(10f, 10f).setCenter(100f, 40f)
    override fun getFocusPosition() = Vector2(100f, 40f)
}

private class Keys : IEventListener {
    val keys = ArrayList<Any>()
    val events = ArrayList<Event>()
    override val eventKeyMask = ObjectSet<Any>().apply { EventType.entries.forEach { add(it) } }
    override fun onEvent(event: Event) {
        keys.add(event.key)
        events.add(event)
    }
}

private class Rig(next: RectangleMapObject) {
    val services = testServices()
    val keys = Keys().also { services.events.addListener(it) }
    val camera = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val rotation = CameraRotation()
    val view = CameraView(camera, rotation)
    val follow = CameraFollow(1f, { 1f })
    val transition = CameraRoomTransition(20f, 0f, 0.01f)
    val first = RectangleMapObject(0f, 0f, 200f, 80f).also { it.name = "first" }
    val roomBounds = CameraRoomBounds(follow, transition, Vector2(50f, 50f))
    val behaviors = ArrayList<Boolean>()
    val player = mockk<IPlayer>(relaxed = true).also {
        every { it.getComponent(BodyComponent::class) } returns BodyComponent(Body(BodyType.ABSTRACT))
    }
    val handler = LevelRoomHandler(services, roomBounds, transition, { player }) {
        behaviors.add(it)
    }
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
        roomBounds.gameRooms = Array.with(first, next)
        follow.setFocus(camera, Focus().also { roomBounds.subject = it })
        roomBounds.reset()
        controller.update(0.01f)
    }

    fun runTransition(name: String, axisOnly: Boolean = false, delta: Float = 0.02f) {
        roomBounds.transitionToRoom(camera, name, Direction.RIGHT, axisOnly)
        repeat(4) { controller.update(delta) }
        services.events.run()
    }
}

private fun next(key: String? = null, value: String? = null) =
    RectangleMapObject(200f, 0f, 200f, 80f).also {
        it.name = "next"
        if (key != null) it.properties.put(key, value)
    }

class LevelRoomHandlerTest : DescribeSpec({

    describe("the end of a room transition") {
        it("turns the controller on when the room has no event") {
            val rig = Rig(next())
            rig.runTransition("next")
            rig.keys.keys shouldContain EventType.TURN_CONTROLLER_ON
            rig.behaviors shouldBe listOf(false, true)
        }

        it("puts the axis-only value of the transition on the begin and continue events") {
            val transitionKeys = listOf(EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS)

            val gathered = Rig(next())
            gathered.runTransition("next", axisOnly = true, delta = 0.004f)
            val gatheredEvents = gathered.keys.events.filter { it.key in transitionKeys }
            gatheredEvents.map { it.key } shouldContain EventType.CONTINUE_ROOM_TRANS
            gatheredEvents.map { it.getProperty(RoomTransitionEventKeys.AXIS_ONLY) }.toSet() shouldBe setOf(true)

            val shared = Rig(next())
            shared.runTransition("next", delta = 0.004f)
            val sharedEvents = shared.keys.events.filter { it.key in transitionKeys }
            sharedEvents.map { it.key } shouldContain EventType.CONTINUE_ROOM_TRANS
            sharedEvents.map { it.getProperty(RoomTransitionEventKeys.AXIS_ONLY) }.toSet() shouldBe setOf(false)
        }

        it("stops the run of a player that is not Megaman") {
            val rig = Rig(next())
            val player = SecondPlayerEntity(rig.services)
            player.addComponent(RunComponent())
            rig.services.players.register(PlayerSlot(0), player)
            player.running = true

            rig.runTransition("next")

            player.running shouldBe false
        }

        it("enters the boss room for event=boss") {
            val rig = Rig(next("event", "boss"))
            rig.runTransition("next")
            rig.keys.keys shouldContain EventType.ENTER_BOSS_ROOM
            rig.keys.keys shouldNotContain EventType.TURN_CONTROLLER_ON
        }

        it("starts the victory for event=success") {
            val rig = Rig(next("event", "success"))
            rig.runTransition("next")
            rig.keys.keys shouldContain EventType.VICTORY_EVENT
            rig.keys.keys shouldNotContain EventType.TURN_CONTROLLER_ON
        }

        it("sets the gravity for a megaman_direction room") {
            mockkObject(MegaEntityFactory)
            every { MegaEntityFactory.fetch(GravitySwitchAura::class) } returns mockk(relaxed = true)
            try {
                val rig = Rig(next("megaman_direction", "down"))
                rig.runTransition("next")
                rig.services.gravity.direction shouldBe Direction.DOWN
            } finally {
                unmockkObject(MegaEntityFactory)
            }
        }
    }
})
