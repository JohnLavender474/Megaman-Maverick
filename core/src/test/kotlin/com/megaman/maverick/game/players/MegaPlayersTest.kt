package com.megaman.maverick.game.players

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.damage.IDamager
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.playerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

private class TestPlayerEntity(game: MegamanMaverickGame) : MegaGameEntity(game), IDirectional {
    override var direction = Direction.UP
    override fun getType() = EntityType.MEGAMAN
}

class MegaPlayersTest : DescribeSpec({

    lateinit var game: MegamanMaverickGame

    beforeEach {
        game = mockk(relaxed = true)
        every { game.engine } returns mockk<GameEngine>(relaxed = true)
    }

    fun playerEntity(withBody: Boolean = true) = TestPlayerEntity(game).also {
        if (withBody) it.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
        it.addComponent(ControllerComponent())
    }

    describe("MegaPlayers") {
        it("has a capacity of 1 by default") {
            MegaPlayers().capacity shouldBe 1
        }

        it("registers an entity with no other parts, and asPlayer gives it back") {
            val entity = TestPlayerEntity(game)

            MegaPlayers().register(PlayerSlot(0), entity) shouldBeSameInstanceAs entity

            entity.asPlayer shouldBeSameInstanceAs entity
            entity.playerSlot shouldBe PlayerSlot(0)
            TestPlayerEntity(game).asPlayer shouldBe null
            "not an entity".asPlayer shouldBe null
            null.asPlayer shouldBe null
        }

        it("fails to register in a slot that already has a player, and leaves the entity unbound") {
            val players = MegaPlayers()
            players.register(PlayerSlot(0), playerEntity())
            val second = playerEntity()

            shouldThrow<IllegalStateException> { players.register(PlayerSlot(0), second) }

            second.getComponent(ControllerComponent::class)!!.input shouldBe null
        }

        it("binds the slot's input frame to the entity's controller component") {
            val players = MegaPlayers(2)
            val entity0 = players.register(PlayerSlot(0), playerEntity())
            val entity1 = players.register(PlayerSlot(1), playerEntity())
            val frame = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)

            players.inputs[PlayerSlot(1)] = frame

            entity1.input shouldBe frame
            entity0.input shouldBe InputFrame.RELEASED
        }

        it("targetFor gives the first player with a body") {
            val players = MegaPlayers(2)
            players.register(PlayerSlot(0), playerEntity(withBody = false))
            val withBody = players.register(PlayerSlot(1), playerEntity())

            players.targetFor(TestPlayerEntity(game)) shouldBeSameInstanceAs withBody
        }

        it("targetFor gives the player in slot 0, also when it is not present") {
            val players = MegaPlayers()
            val entity = players.register(PlayerSlot(0), playerEntity())

            entity.existenceState.liveNow shouldBe false

            players.targetFor(TestPlayerEntity(game)) shouldBeSameInstanceAs entity
        }

        it("tells the damage signal's listeners in the order they connected, with the player and the damager") {
            val players = MegaPlayers()
            val player = players.register(PlayerSlot(0), playerEntity())
            val damager = mockk<IDamager>()
            val calls = ArrayList<String>()
            val first: (PlayerDamaged) -> Unit = {
                calls.add("first")
                it.player shouldBeSameInstanceAs player
                it.damager shouldBeSameInstanceAs damager
            }
            players.playerDamaged.connect(first)
            players.playerDamaged.connect { calls.add("second") }

            players.onPlayerDamaged(player, damager)
            players.playerDamaged.disconnect(first)
            players.onPlayerDamaged(player, damager)

            calls shouldBe listOf("first", "second", "second")
        }
    }

    describe("WorldGravity") {
        it("starts up, and set stores the direction and sets it on every registered player") {
            val players = MegaPlayers(2)
            val entity0 = playerEntity()
            val entity1 = playerEntity()
            players.register(PlayerSlot(0), entity0)
            players.register(PlayerSlot(1), entity1)
            val gravity = WorldGravity(players)

            gravity.direction shouldBe Direction.UP

            gravity.set(Direction.DOWN)

            gravity.direction shouldBe Direction.DOWN
            entity0.direction shouldBe Direction.DOWN
            entity1.direction shouldBe Direction.DOWN
        }
    }
})
