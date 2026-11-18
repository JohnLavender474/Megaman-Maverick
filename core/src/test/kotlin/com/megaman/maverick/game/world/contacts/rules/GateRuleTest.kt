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
        it("marks the touching player on the gate at the begin and each continue of a contact") {
            val gate = mockk<Gate>(relaxed = true)
            val player = registeredPlayer()

            GateRule.onBegin(fixture(FixtureType.SIDE, player), fixture(FixtureType.GATE, gate), 0f)
            GateRule.onContinue(fixture(FixtureType.FEET, player), fixture(FixtureType.GATE, gate), 0f)

            verify(exactly = 2) { gate.markTouched(player) }
        }

        it("marks nothing for another entity") {
            val gate = mockk<Gate>(relaxed = true)

            GateRule.onBegin(fixture(FixtureType.SIDE, nonPlayer()), fixture(FixtureType.GATE, gate), 0f)
            GateRule.onContinue(fixture(FixtureType.SIDE, nonPlayer()), fixture(FixtureType.GATE, gate), 0f)

            verify(exactly = 0) { gate.markTouched(any()) }
        }
    }
})
