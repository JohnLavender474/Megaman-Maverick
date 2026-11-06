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
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.systems.GameSystem
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private const val BOSS_KEY = "${ConstKeys.BOSS}_${ConstKeys.KEY}"
private const val GAUNTLET_KEY = "${ConstKeys.GAUNTLET}_${ConstKeys.KEY}"

class GateTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices

    beforeEach {
        engine = GameEngine(Array<GameSystem>())
        services = testServices(engine = engine)
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
    }

    afterEach { engine.reset() }

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
        gate.trigger()
        return !gate.isTriggerable()
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

        it("locks an unlocked gate with a key again on PLAYER_SPAWN") {
            val gate = spawnGate(BOSS_KEY to "1")
            send(EventType.UNLOCK_GATE, props(ConstKeys.KEY pairTo "1"))

            send(EventType.PLAYER_SPAWN)

            unlockedAndTriggered(gate) shouldBe false
        }

        it("leaves a gate with no key unlocked") {
            unlockedAndTriggered(spawnGate()) shouldBe true
        }

        it("throws at spawn when the gate has both a boss key and a gauntlet key") {
            shouldThrow<IllegalStateException> { spawnGate(BOSS_KEY to "1", GAUNTLET_KEY to "g0") }
        }
    }
})
