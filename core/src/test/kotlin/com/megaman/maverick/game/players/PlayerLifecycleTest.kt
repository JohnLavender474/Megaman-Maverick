package com.megaman.maverick.game.players

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.behaviors.IBehavior
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.locks.isLocked
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.screens.levels.tiledmap.TiledMapLoadResult
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.state.PostActionMomentumComponent
import com.megaman.maverick.game.entities.state.RoomTransitionCarryComponent
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify

private class EventRecorder(vararg keys: Any) : IEventListener {
    override val eventKeyMask = objectSetOf(*keys)
    val received = Array<Event>()
    override fun onEvent(event: Event) {
        received.add(event)
    }
}

class PlayerLifecycleTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var first: TestHealthPlayer
    lateinit var second: TestHealthPlayer

    beforeEach {
        engine = mockk(relaxed = true)
        val slots = MegaPlayers(2)
        val services = testServices(engine = engine, players = slots)
        first = TestHealthPlayer(services)
        second = TestHealthPlayer(services)
        slots.register(PlayerSlot(0), first)
        slots.register(PlayerSlot(1), second)
        first.existenceState = EntityExistenceState.LIVE
    }

    afterEach { unmockkAll() }

    describe("playerSpawnStart") {
        fun spawnProps(direction: String = ConstKeys.UP) = props(
            ConstKeys.BOUNDS pairTo GameRectangle(100f, 200f, 32f, 32f),
            ConstKeys.FACING pairTo ConstKeys.LEFT,
            ConstKeys.DIRECTION pairTo direction
        )

        beforeEach { first.addComponent(TeleportableComponent()) }

        it("places the body on the bottom center of the spawn bounds") {
            first.playerSpawnStart(spawnProps())

            first.body.getX() shouldBe 100f
            first.body.getY() shouldBe 200f
        }

        it("sets the facing, the world gravity and the full health") {
            first.setHealth(1)
            first.playerSpawnStart(spawnProps(direction = "down"))

            first.facing shouldBe Facing.LEFT
            first.services.gravity.direction shouldBe Direction.DOWN
            first.getCurrentHealth() shouldBe first.getMaxHealth()
        }

        it("clears the movement and damage locks and adds the listener") {
            first.lock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)
            first.lock(MegaLockGroup.DAMAGE, MegaLockReason.TELEPORT)

            first.playerSpawnStart(spawnProps())

            first.isLocked(MegaLockGroup.MOVEMENT) shouldBe false
            first.isLocked(MegaLockGroup.DAMAGE) shouldBe false
            first.services.events.isListener(first) shouldBe true
        }

        it("starts the body velocity at zero with gravity and collision on") {
            first.body.physics.velocity.set(5f, 5f)
            first.body.physics.gravityOn = false

            first.playerSpawnStart(spawnProps())

            first.body.physics.velocity.isZero shouldBe true
            first.body.physics.gravityOn shouldBe true
            first.body.physics.collisionOn shouldBe true
        }
    }

    describe("playerSpawnEnd") {
        it("submits no level reset for any slot, because the respawn routine sends it") {
            val recorder = EventRecorder(EventType.LEVEL_RESET)
            val events = first.services.events
            events.addListener(recorder)

            first.playerSpawnEnd()
            second.playerSpawnEnd()
            events.run()

            recorder.received.size shouldBe 0
        }
    }

    describe("playerOnDestroy") {
        it("removes the listener and submits PLAYER_JUST_DIED with the slot") {
            val recorder = EventRecorder(EventType.PLAYER_JUST_DIED)
            val events = second.services.events
            events.addListener(recorder)
            events.addListener(second)

            second.playerOnDestroy()
            events.run()

            events.isListener(second) shouldBe false
            recorder.received.size shouldBe 1
            recorder.received[0].getProperty(PlayerEventKeys.SLOT) shouldBe PlayerSlot(1)
        }
    }

    describe("playerOnRoomTransition") {
        beforeEach {
            first.addComponent(RoomTransitionCarryComponent(0.1f) { false })
            first.addComponent(PostActionMomentumComponent(0.1f, 1f, { 1f }, { true }, { false }, { false }))
        }

        fun transition(key: EventType, position: Vector2? = null) =
            Event(key, if (position != null) props(ConstKeys.POSITION pairTo position) else props())

        it("centers the body on the path and turns gravity off") {
            first.playerOnRoomTransition(transition(EventType.BEGIN_ROOM_TRANS, Vector2(50f, 60f)))
            first.body.getCenter(Vector2()) shouldBe Vector2(50f, 60f)
            first.body.physics.gravityOn shouldBe false

            first.playerOnRoomTransition(transition(EventType.CONTINUE_ROOM_TRANS, Vector2(70f, 60f)))
            first.body.getCenter(Vector2()) shouldBe Vector2(70f, 60f)
        }

        it("turns gravity on at the end, except while climbing") {
            first.playerOnRoomTransition(transition(EventType.END_ROOM_TRANS))
            first.body.physics.gravityOn shouldBe true

            val behaviors = BehaviorsComponent()
            val climb = mockk<IBehavior>(relaxed = true)
            every { climb.isActive() } returns true
            behaviors.addBehavior(BehaviorType.CLIMBING, climb)
            first.addComponent(behaviors)

            first.playerOnRoomTransition(transition(EventType.END_ROOM_TRANS))
            first.body.physics.gravityOn shouldBe false
        }

        it("throws for an event that is not a room transition") {
            shouldThrow<IllegalArgumentException> { first.playerOnRoomTransition(transition(EventType.LEVEL_RESET)) }
        }
    }

    describe("killPlayerIfOutsideLevel") {
        beforeEach {
            val map = TiledMap()
            map.properties.put(ConstKeys.WIDTH, 10)
            map.properties.put(ConstKeys.HEIGHT, 8)
            first.services.levelState.tiledMapLoadResult = TiledMapLoadResult(map, ObjectMap(), 0, 0, 0, 0)
        }

        it("keeps a player inside the level") {
            first.body.setPosition(64f, 64f)
            first.killPlayerIfOutsideLevel()
            verify(exactly = 0) { engine.destroy(first) }
        }

        it("destroys a player 1.5 tiles past an edge") {
            first.body.setPosition(64f, -48.5f)
            first.killPlayerIfOutsideLevel()
            verify(exactly = 1) { engine.destroy(first) }
        }
    }
})
