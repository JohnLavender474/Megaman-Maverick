package com.megaman.maverick.game.players

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.players.PlayerNotRegisteredException
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.isPlayer
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk
import io.mockk.unmockkAll

class PlayerRosterTest : DescribeSpec({

    lateinit var players: MegaPlayers
    lateinit var roster: PlayerRoster
    lateinit var megaman: TestHealthPlayer
    lateinit var x: TestHealthPlayer
    lateinit var second: TestHealthPlayer

    val slot0 = PlayerSlot(0)
    val megamanLevel = setOf(PlayerCharacter.MEGAMAN)
    val xLevel = setOf(PlayerCharacter.X)

    beforeEach {
        players = MegaPlayers(2)
        val services = testServices(engine = mockk<GameEngine>(relaxed = true), players = players)
        megaman = TestHealthPlayer(services)
        x = TestHealthPlayer(services)
        second = TestHealthPlayer(services)

        players.register(slot0, megaman)
        roster = PlayerRoster(players)
        roster.add(megaman, PlayerCharacter.MEGAMAN)
        roster.add(x, PlayerCharacter.X)
        roster.add(second)
    }

    afterEach { unmockkAll() }

    describe("putInSlot0") {
        it("puts X in slot 0 on an X level and leaves Megaman with no slot") {
            roster.putInSlot0(xLevel, 1)

            players.get(slot0)!! shouldBeSameInstanceAs x
            megaman.isPlayer shouldBe false
            megaman.asPlayer shouldBe null
            shouldThrow<PlayerNotRegisteredException> { megaman.slot }
        }

        it("puts Megaman back in slot 0 on a Megaman level") {
            roster.putInSlot0(xLevel, 1)
            roster.putInSlot0(megamanLevel, 1)

            players.get(slot0)!! shouldBeSameInstanceAs megaman
            x.isPlayer shouldBe false
        }

        it("changes nothing when slot 0 already holds the character") {
            roster.putInSlot0(megamanLevel, 1)

            players.get(slot0)!! shouldBeSameInstanceAs megaman
        }

        it("throws on an X level with 2 players") {
            shouldThrow<IllegalStateException> { roster.putInSlot0(xLevel, 2) }
            players.get(slot0)!! shouldBeSameInstanceAs megaman
        }

        it("allows 2 players on a Megaman level") {
            roster.putInSlot0(megamanLevel, 2)

            players.get(slot0)!! shouldBeSameInstanceAs megaman
        }

        it("throws when the player in slot 0 is in the engine") {
            megaman.existenceState = EntityExistenceState.LIVE

            shouldThrow<IllegalStateException> { roster.putInSlot0(xLevel, 1) }
            players.get(slot0)!! shouldBeSameInstanceAs megaman
        }

        it("throws for a level with more than 1 character") {
            shouldThrow<IllegalArgumentException> {
                roster.putInSlot0(setOf(PlayerCharacter.MEGAMAN, PlayerCharacter.X), 1)
            }
        }
    }

    describe("forEach") {
        it("gives every entity in the roster, also the ones with no slot") {
            val seen = mutableListOf<Any>()
            roster.forEach { seen.add(it) }

            seen shouldBe listOf(megaman, x, second)
        }
    }
})
