package com.megaman.maverick.game.world.contacts

import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.players.nonPlayer
import com.megaman.maverick.game.players.registeredPlayer
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class MegaContactFilterTest : DescribeSpec({

    val body = Body(BodyType.ABSTRACT)
    val filter = MegaContactFilter()

    fun fixture(type: FixtureType, entity: MegaGameEntity): Fixture {
        val fixture = Fixture(body, type)
        fixture.putProperty(ConstKeys.ENTITY, entity)
        return fixture
    }

    listOf(FixtureType.GATE, FixtureType.CART).forEach { type ->
        describe("a $type contact") {
            it("stays with a player") {
                filter.filter(fixture(FixtureType.FEET, registeredPlayer()), fixture(type, nonPlayer())) shouldBe true
            }

            it("stays with a player as the second fixture") {
                filter.filter(fixture(type, nonPlayer()), fixture(FixtureType.FEET, registeredPlayer())) shouldBe true
            }

            it("is dropped with no player") {
                filter.filter(fixture(FixtureType.FEET, nonPlayer()), fixture(type, nonPlayer())) shouldBe false
            }
        }
    }
})
