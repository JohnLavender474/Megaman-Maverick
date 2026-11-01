package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.special.Cart
import com.megaman.maverick.game.players.nonPlayer
import com.megaman.maverick.game.players.registeredPlayer
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FeetCartRuleTest : DescribeSpec({

    val body = Body(BodyType.ABSTRACT)

    fun fixture(type: FixtureType, entity: MegaGameEntity): Fixture {
        val fixture = Fixture(body, type)
        fixture.putProperty(ConstKeys.ENTITY, entity)
        return fixture
    }

    describe("FeetCartRule") {
        it("sets the owner and the sense for a player, and removes them at the end") {
            val player = registeredPlayer()
            val cart = mockk<Cart>(relaxed = true)
            val feet = fixture(FixtureType.FEET, player)
            val cartFixture = fixture(FixtureType.CART, cart)

            FeetCartRule.onBegin(feet, cartFixture, 0f)

            verify(exactly = 1) { cart.owner = player }
            player.body.isSensing(BodySense.TOUCHING_CART) shouldBe true
            player.body.getProperty(ConstKeys.CART) shouldBe cart

            FeetCartRule.onEnd(feet, cartFixture, 0f)

            verify(exactly = 1) { cart.owner = null }
            player.body.isSensing(BodySense.TOUCHING_CART) shouldBe false
            player.body.hasProperty(ConstKeys.CART) shouldBe false
        }

        it("sets neither the owner nor the sense for another entity") {
            val other = nonPlayer()
            val cart = mockk<Cart>(relaxed = true)

            FeetCartRule.onBegin(fixture(FixtureType.FEET, other), fixture(FixtureType.CART, cart), 0f)

            verify(exactly = 0) { cart.owner = any() }
            other.body.isSensing(BodySense.TOUCHING_CART) shouldBe false
            other.body.hasProperty(ConstKeys.CART) shouldBe false
        }

        it("clears the owner at the end for another entity") {
            val cart = mockk<Cart>(relaxed = true)

            FeetCartRule.onEnd(fixture(FixtureType.FEET, nonPlayer()), fixture(FixtureType.CART, cart), 0f)

            verify(exactly = 1) { cart.owner = null }
        }
    }
})
