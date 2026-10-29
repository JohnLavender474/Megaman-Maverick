package com.megaman.maverick.game.world.contacts.rules

import com.megaman.maverick.game.testServices
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.players.PlayerComponent
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.world.body.FixtureType
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify

class PlayerItemRuleTest : DescribeSpec({

    val body = Body(BodyType.ABSTRACT)

    fun fixture(type: FixtureType, entity: MegaGameEntity): Fixture {
        val fixture = Fixture(body, type)
        fixture.putProperty(ConstKeys.ENTITY, entity)
        return fixture
    }

    lateinit var players: MegaPlayers
    lateinit var megaman: Megaman

    beforeEach {
        players = MegaPlayers(1)
        val game = testServices(engine = mockk<GameEngine>(relaxed = true), players = players)

        megaman = Megaman(game)
        megaman.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
        megaman.addComponent(BehaviorsComponent())
        megaman.addComponent(ControllerComponent())

        players.register(PlayerSlot(0), megaman)
    }

    afterEach { unmockkAll() }

    describe("PlayerItemRule") {
        it("gives the item the player view of the entity in contact") {
            val item = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(ItemEntity::class))

            PlayerItemRule.onBegin(fixture(FixtureType.PLAYER, megaman), fixture(FixtureType.ITEM, item), 0f)

            verify(exactly = 1) { (item as ItemEntity).contactWithPlayer(players.get(PlayerSlot(0))!!) }
        }

        it("does nothing for an entity that is not a player") {
            val other = mockk<MegaGameEntity>(relaxed = true)
            every { other.hasComponent(PlayerComponent::class) } returns false
            val item = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(ItemEntity::class))

            PlayerItemRule.onBegin(fixture(FixtureType.PLAYER, other), fixture(FixtureType.ITEM, item), 0f)

            verify(exactly = 0) { (item as ItemEntity).contactWithPlayer(any()) }
        }
    }
})
