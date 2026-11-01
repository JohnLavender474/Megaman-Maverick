package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.sensors.Gate
import com.megaman.maverick.game.players.nonPlayer
import com.megaman.maverick.game.players.registeredPlayer
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GateRuleTest : DescribeSpec({

    val body = Body(BodyType.ABSTRACT)

    fun fixture(type: FixtureType, entity: MegaGameEntity): Fixture {
        val fixture = Fixture(body, type)
        fixture.putProperty(ConstKeys.ENTITY, entity)
        return fixture
    }

    describe("GateRule") {
        it("triggers a triggerable gate for a player") {
            val gate = mockk<Gate>(relaxed = true)
            every { gate.isTriggerable() } returns true

            GateRule.onBegin(fixture(FixtureType.SIDE, registeredPlayer()), fixture(FixtureType.GATE, gate), 0f)

            verify(exactly = 1) { gate.trigger() }
        }

        it("does not trigger the gate for a player if the gate is not triggerable") {
            val gate = mockk<Gate>(relaxed = true)
            every { gate.isTriggerable() } returns false

            GateRule.onBegin(fixture(FixtureType.SIDE, registeredPlayer()), fixture(FixtureType.GATE, gate), 0f)

            verify(exactly = 0) { gate.trigger() }
        }

        it("does not trigger the gate for another entity") {
            val gate = mockk<Gate>(relaxed = true)
            every { gate.isTriggerable() } returns true

            GateRule.onBegin(fixture(FixtureType.SIDE, nonPlayer()), fixture(FixtureType.GATE, gate), 0f)

            verify(exactly = 0) { gate.trigger() }
        }
    }
})
