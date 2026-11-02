package com.megaman.maverick.game.players

import com.megaman.maverick.game.testServices
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.controller.buttons.ButtonStatus
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.input
import com.maverick.game.engine.input.InputFrame
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.playerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.controllers.SelectButtonAction
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

private class TestPlayerEntity(services: MegaServices) : MegaGameEntity(services), IDirectional {
    override var direction = Direction.UP
    override fun getType() = EntityType.MEGAMAN
}

class MegaPlayersTest : DescribeSpec({

    lateinit var services: MegaServices

    beforeEach {
        services = testServices(engine = mockk<GameEngine>(relaxed = true))
    }

    fun playerEntity(withBody: Boolean = true) = TestPlayerEntity(services).also {
        if (withBody) it.addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
        it.addComponent(ControllerComponent())
    }

    describe("MegaPlayers") {
        it("has a capacity of 1 by default") {
            MegaPlayers().capacity shouldBe 1
        }

        it("registers an entity with no other parts, and asPlayer gives it back") {
            val entity = TestPlayerEntity(services)

            MegaPlayers().register(PlayerSlot(0), entity) shouldBeSameInstanceAs entity

            entity.asPlayer shouldBeSameInstanceAs entity
            entity.playerSlot shouldBe PlayerSlot(0)
            TestPlayerEntity(services).asPlayer shouldBe null
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

        it("targetFor gives the first player with a body when no player is live") {
            val players = MegaPlayers(2)
            players.register(PlayerSlot(0), playerEntity(withBody = false))
            val withBody = players.register(PlayerSlot(1), playerEntity())

            players.targetFor(TestPlayerEntity(services)) shouldBeSameInstanceAs withBody
        }

        it("targetFor gives the player in slot 0, also when it is not present") {
            val players = MegaPlayers()
            val entity = players.register(PlayerSlot(0), playerEntity())

            entity.existenceState.liveNow shouldBe false

            players.targetFor(TestPlayerEntity(services)) shouldBeSameInstanceAs entity
        }

        describe("targetFor with 2 players") {
            lateinit var players: MegaPlayers
            lateinit var left: MegaGameEntity
            lateinit var right: MegaGameEntity

            fun livePlayer(slot: Int, x: Float): MegaGameEntity {
                val entity = playerEntity()
                entity.getComponent(BodyComponent::class)!!.body.setPosition(x, 0f)
                entity.existenceState = EntityExistenceState.LIVE
                return players.register(PlayerSlot(slot), entity)
            }

            fun seekerAt(x: Float) = TestPlayerEntity(services).also {
                it.addComponent(BodyComponent(Body(BodyType.DYNAMIC, x, 0f, 0f, 0f)))
            }

            beforeEach {
                players = MegaPlayers(2)
                left = livePlayer(0, 0f)
                right = livePlayer(1, 100f)
            }

            it("gives the nearest live player") {
                players.targetFor(seekerAt(10f)) shouldBeSameInstanceAs left
                players.targetFor(seekerAt(90f)) shouldBeSameInstanceAs right
            }

            it("gives the lower slot on a tie") {
                players.targetFor(seekerAt(50f)) shouldBeSameInstanceAs left
            }

            it("skips a dead player") {
                left.existenceState = EntityExistenceState.UNSPAWNED
                players.targetFor(seekerAt(10f)) shouldBeSameInstanceAs right
            }

            it("skips a player who is not in play") {
                left.addComponent(PlayPhaseComponent(PlayPhase.PRE_PLAY))
                players.targetFor(seekerAt(10f)) shouldBeSameInstanceAs right
            }

            it("gives the first live player to a seeker with no body") {
                players.targetFor(TestPlayerEntity(services)) shouldBeSameInstanceAs left
                left.existenceState = EntityExistenceState.UNSPAWNED
                players.targetFor(TestPlayerEntity(services)) shouldBeSameInstanceAs right
            }

            it("gives slot 0 when no player is live") {
                left.existenceState = EntityExistenceState.UNSPAWNED
                right.existenceState = EntityExistenceState.UNSPAWNED
                players.targetFor(seekerAt(90f)) shouldBeSameInstanceAs left
            }

            it("anyLive and forEachLive skip a dead player while the other player is live") {
                left.existenceState = EntityExistenceState.UNSPAWNED
                val visited = ArrayList<MegaGameEntity>()

                players.forEachLive { visited.add(it) }

                visited shouldBe listOf(right)
                players.anyLive { it == left } shouldBe false
                players.anyLive { it == right } shouldBe true
            }

            it("anyLive and forEachLive skip a player who is not in play while the other player is live") {
                left.addComponent(PlayPhaseComponent(PlayPhase.POST_PLAY))
                val visited = ArrayList<MegaGameEntity>()

                players.forEachLive { visited.add(it) }

                visited shouldBe listOf(right)
                players.anyLive { it == left } shouldBe false
            }

            it("anyLive and forEachLive visit every player when no player is live") {
                left.existenceState = EntityExistenceState.UNSPAWNED
                right.addComponent(PlayPhaseComponent(PlayPhase.PRE_PLAY))
                val visited = ArrayList<MegaGameEntity>()

                players.forEachLive { visited.add(it) }

                visited shouldBe listOf(left, right)
                players.anyLive { it == left } shouldBe true
            }
        }

        it("targetFor with 1 live player gives that player and does not read the body of the seeker") {
            val players = MegaPlayers(2)
            val entity = playerEntity().also { it.existenceState = EntityExistenceState.LIVE }
            players.register(PlayerSlot(0), entity)

            players.targetFor(TestPlayerEntity(services)) shouldBeSameInstanceAs entity
        }

        it("tells the damage signal's listeners in the order they connected, with the player and the damager") {
            val players = MegaPlayers()
            val player = players.register(PlayerSlot(0), playerEntity())
            val damager = mockk<IDamagerEntity>()
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

    describe("MegaPlayers.settings") {
        it("starts as NEXT_WEAPON and keeps a value that is set") {
            val settings = MegaPlayers().settings(PlayerSlot(0))
            settings.selectButtonAction shouldBe SelectButtonAction.NEXT_WEAPON

            settings.selectButtonAction = SelectButtonAction.AIR_DASH
            settings.selectButtonAction shouldBe SelectButtonAction.AIR_DASH

            settings.selectButtonAction = SelectButtonAction.START
            settings.selectButtonAction shouldBe SelectButtonAction.START
        }

        it("has one settings object per slot, and a change to one does not change the other") {
            val players = MegaPlayers(2)
            val first = players.settings(PlayerSlot(0))
            val second = players.settings(PlayerSlot(1))

            first shouldNotBeSameInstanceAs second

            first.selectButtonAction = SelectButtonAction.AIR_DASH

            second.selectButtonAction shouldBe SelectButtonAction.NEXT_WEAPON
        }
    }

    describe("MegaPlayers.menuInput") {
        it("gives the frame of slot 0, not of slot 1") {
            val players = MegaPlayers(2)
            val first = InputFrame.RELEASED.with(MegaControllerButton.A, ButtonStatus.JUST_PRESSED)
            val second = InputFrame.RELEASED.with(MegaControllerButton.B, ButtonStatus.JUST_RELEASED)

            players.inputs[PlayerSlot(0)] = first
            players.inputs[PlayerSlot(1)] = second

            players.menuInput shouldBe first
        }

        it("is RELEASED before any write, also with no registered player") {
            val players = MegaPlayers()

            players.get(PlayerSlot(0)) shouldBe null
            players.menuInput shouldBe InputFrame.RELEASED
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
