package com.megaman.maverick.game.entities.sensors

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.systems.GameSystem
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.events.EventType
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.setPlayPhase
import com.megaman.maverick.game.events.RoomTransitionEventKeys
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestHealthPlayer
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.state.SessionRules
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class EventRecorder(vararg keys: Any) : IEventListener {
    override val eventKeyMask = objectSetOf(*keys)
    val received = Array<Event>()
    override fun onEvent(event: Event) {
        received.add(event)
    }
}

private const val BOSS_KEY = "${ConstKeys.BOSS}_${ConstKeys.KEY}"
private const val GAUNTLET_KEY = "${ConstKeys.GAUNTLET}_${ConstKeys.KEY}"

class GateTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var first: TestHealthPlayer
    lateinit var second: TestHealthPlayer
    lateinit var recorder: EventRecorder

    beforeEach {
        engine = GameEngine(Array<GameSystem>())
        val players = MegaPlayers(2)
        services = testServices(engine = engine, players = players)
        first = TestHealthPlayer(services).also {
            players.register(PlayerSlot(0), it)
            it.existenceState = EntityExistenceState.LIVE
        }
        second = TestHealthPlayer(services)
        recorder = EventRecorder(EventType.GATE_INIT_OPENING, EventType.NEXT_ROOM_REQ)
        services.events.addListener(recorder)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
    }

    afterEach {
        engine.reset()
        MegaGameEntities.remove(first)
        MegaGameEntities.remove(second)
    }

    fun joinSecond() {
        services.players.register(PlayerSlot(1), second)
        second.existenceState = EntityExistenceState.LIVE
    }

    fun update(gate: Gate, delta: Float = 0.01f, vararg touching: IPlayer) {
        touching.forEach { gate.markTouched(it) }
        gate.getComponent(UpdatablesComponent::class)!!.updatables.values().forEach { it.update(delta) }
        services.events.run()
    }

    fun opened() = recorder.received.any { it.key == EventType.GATE_INIT_OPENING }

    fun spawnGate(vararg keys: Pair<String, String>): Gate {
        val gate = Gate(services)
        val spawnProps = props(
            ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 32f, 96f),
            ConstKeys.ROOM pairTo "next_room"
        )
        keys.forEach { (key, value) -> spawnProps.put(key, value) }
        gate.spawn(spawnProps)
        engine.update(0f)
        return gate
    }

    fun send(key: EventType, props: Properties = Properties()) {
        services.events.submitEvent(Event(key, props))
        services.events.run()
    }

    fun unlockedAndTriggered(gate: Gate): Boolean {
        recorder.received.clear()
        update(gate, touching = arrayOf(first))
        return opened()
    }

    describe("Gate unlock by key") {
        it("unlocks a gate with a boss key on UNLOCK_GATE with that key") {
            val gate = spawnGate(BOSS_KEY to "1")

            send(EventType.UNLOCK_GATE, props(ConstKeys.KEY pairTo "1"))

            unlockedAndTriggered(gate) shouldBe true
        }

        it("unlocks a gate with a gauntlet key on UNLOCK_GATE with that key") {
            val gate = spawnGate(GAUNTLET_KEY to "g0")

            send(EventType.UNLOCK_GATE, props(ConstKeys.KEY pairTo "g0"))

            unlockedAndTriggered(gate) shouldBe true
        }

        it("keeps a gate with a key locked on UNLOCK_GATE with another key") {
            val bossGate = spawnGate(BOSS_KEY to "1")
            val gauntletGate = spawnGate(GAUNTLET_KEY to "g0")

            send(EventType.UNLOCK_GATE, props(ConstKeys.KEY pairTo "2"))

            unlockedAndTriggered(bossGate) shouldBe false
            unlockedAndTriggered(gauntletGate) shouldBe false
        }

        it("locks an unlocked gate with a key again on LEVEL_RESET") {
            val gate = spawnGate(BOSS_KEY to "1")
            send(EventType.UNLOCK_GATE, props(ConstKeys.KEY pairTo "1"))

            send(EventType.LEVEL_RESET)

            unlockedAndTriggered(gate) shouldBe false
        }

        it("leaves a gate with no key unlocked") {
            unlockedAndTriggered(spawnGate()) shouldBe true
        }

        it("throws at spawn when the gate has both a boss key and a gauntlet key") {
            shouldThrow<IllegalStateException> { spawnGate(BOSS_KEY to "1", GAUNTLET_KEY to "g0") }
        }
    }

    describe("Gate opens when every live player touches it") {
        it("opens with 1 player on the first touch, in the same update") {
            val gate = spawnGate()
            update(gate, touching = arrayOf(first))
            opened() shouldBe true
        }

        it("does not open with no touch") {
            val gate = spawnGate()
            update(gate)
            opened() shouldBe false
        }

        it("waits with 2 players until both touch it in the same update") {
            joinSecond()
            val gate = spawnGate()

            update(gate, touching = arrayOf(first))
            opened() shouldBe false
            update(gate, touching = arrayOf(second))
            opened() shouldBe false

            update(gate, touching = arrayOf(first, second))
            opened() shouldBe true
        }

        it("opens for the one live player when the partner is dead") {
            joinSecond()
            val gate = spawnGate()
            update(gate, touching = arrayOf(first))
            opened() shouldBe false

            second.existenceState = EntityExistenceState.UNSPAWNED
            update(gate, touching = arrayOf(first))
            opened() shouldBe true
        }

        it("does not open when only a dying player touched it and no player arrives") {
            joinSecond()
            first.existenceState = EntityExistenceState.UNSPAWNED
            second.existenceState = EntityExistenceState.DESTROY_QUEUED
            val gate = spawnGate()
            update(gate, touching = arrayOf(second))
            opened() shouldBe false
        }

        it("stays shut with no arriving player and no touch") {
            joinSecond()
            first.existenceState = EntityExistenceState.UNSPAWNED
            second.existenceState = EntityExistenceState.UNSPAWNED
            val gate = spawnGate()
            update(gate)
            opened() shouldBe false
        }

        it("counts a partner whose spawn-in has begun, so one player cannot open it alone") {
            joinSecond()
            val gate = spawnGate()

            second.existenceState = EntityExistenceState.SPAWN_QUEUED
            update(gate, touching = arrayOf(first))
            opened() shouldBe false

            second.existenceState = EntityExistenceState.LIVE
            second.setPlayPhase(PlayPhase.PRE_PLAY)
            update(gate, touching = arrayOf(first))
            opened() shouldBe false

            update(gate, touching = arrayOf(first, second))
            opened() shouldBe true
        }

        it("does not count a partner that is out of play") {
            joinSecond()
            second.setPlayPhase(PlayPhase.POST_PLAY)
            val gate = spawnGate()
            update(gate, touching = arrayOf(first))
            opened() shouldBe true
        }

        it("opens a locked gate at once when it is unlocked while a player stands in it") {
            val gate = spawnGate(BOSS_KEY to "1")
            update(gate, touching = arrayOf(first))
            opened() shouldBe false

            send(EventType.UNLOCK_GATE, props(ConstKeys.KEY pairTo "1"))
            update(gate, touching = arrayOf(first))
            opened() shouldBe true
        }
    }

    describe("the room request of an opened gate") {
        fun roomRequestAxisOnly(gate: Gate): Boolean? {
            recorder.received.clear()
            update(gate, touching = arrayOf(first))
            update(gate, delta = 1f)
            val request = recorder.received.first { it.key == EventType.NEXT_ROOM_REQ }
            return request.getProperty(RoomTransitionEventKeys.AXIS_ONLY)
        }

        it("asks for an axis-only pan at a LEFT or RIGHT gate") {
            roomRequestAxisOnly(spawnGate()) shouldBe true
            roomRequestAxisOnly(spawnGate(ConstKeys.DIRECTION to "right")) shouldBe true
        }

        it("asks for the shared point at an UP gate") {
            roomRequestAxisOnly(spawnGate(ConstKeys.DIRECTION to "up")) shouldBe false
        }

        it("asks for the shared point when a horizontal gate has a vertical trans_direction") {
            roomRequestAxisOnly(spawnGate("${ConstKeys.TRANS}_${ConstKeys.DIRECTION}" to "down")) shouldBe false
        }
    }

    describe("the touch area of an UP gate") {
        fun gateFixtureWidthInTiles(rules: SessionRules): Float {
            val ruleEngine = GameEngine(Array<GameSystem>())
            val ruleServices = testServices(engine = ruleEngine, players = MegaPlayers(2), rules = rules)
            val atlas = mockk<TextureAtlas>()
            every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
            every { ruleServices.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
            val gate = Gate(ruleServices)
            gate.spawn(
                props(
                    ConstKeys.BOUNDS pairTo GameRectangle(0f, 0f, 96f, 64f),
                    ConstKeys.ROOM pairTo "next_room",
                    ConstKeys.DIRECTION pairTo "up"
                )
            )
            ruleEngine.update(0f)
            gate.body.preProcess()
            val fixture = gate.body.getFixtures(Array<IFixture>(), FixtureType.GATE).first()
            val width = (fixture.getShape() as GameRectangle).getWidth() / ConstVals.PPM
            ruleEngine.reset()
            return width
        }

        it("covers the full gate width in co-op, where a player waits on the GateBlock") {
            gateFixtureWidthInTiles(GameMode.COOP.rules) shouldBe 3f
        }

        it("keeps the narrow touch area in single-player") {
            gateFixtureWidthInTiles(GameMode.SINGLE.rules) shouldBe 1.5f
        }
    }
})
