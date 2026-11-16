package com.megaman.maverick.game.levels

import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.PlayerCharacter
import com.megaman.maverick.game.players.TestMegaPlayer
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify

private class RewardTestPlayer(services: MegaServices, override val character: PlayerCharacter) :
    TestMegaPlayer(services)

class WeaponRewardListenerTest : DescribeSpec({

    lateinit var state: GameState
    lateinit var levelState: LevelState
    lateinit var players: MegaPlayers

    fun register(slot: Int, character: PlayerCharacter) {
        val services = testServices(engine = mockk<GameEngine>(relaxed = true))
        players.register(PlayerSlot(slot), RewardTestPlayer(services, character))
    }

    beforeEach {
        state = GameState()
        levelState = spyk(LevelState())
        players = MegaPlayers(capacity = 2)
        state.addListener(WeaponRewardListener(levelState, players))
    }

    afterEach { unmockkAll() }

    describe("a newly completed level") {
        it("sets exactly the rewards of the slot-0 character") {
            register(0, PlayerCharacter.MEGAMAN)

            state.addLevelCompleted(LevelDefinition.MOON_MAN)

            levelState.weaponsAttained!!.toList() shouldBe listOf(MegamanWeapons.MOON_SCYTHES, MegamanWeapons.RUSH_JET)
        }

        it("sets the rewards once, with the same value, when 2 Megamans play") {
            register(0, PlayerCharacter.MEGAMAN)
            register(1, PlayerCharacter.MEGAMAN)

            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            verify(exactly = 1) { levelState.weaponsAttained = any() }
            levelState.weaponsAttained!!.toList() shouldBe listOf(MegamanWeapons.NEEDLE_SPIN)
        }

        it("sets no reward when the slot-0 character's loadout has none for the level") {
            register(0, PlayerCharacter.X)

            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            levelState.weaponsAttained shouldBe null
        }

        it("does not set them again for a level that is already completed") {
            register(0, PlayerCharacter.MEGAMAN)
            state.addLevelCompleted(LevelDefinition.DESERT_MAN)
            levelState.weaponsAttained = null

            state.addLevelCompleted(LevelDefinition.DESERT_MAN)

            levelState.weaponsAttained shouldBe null
        }
    }
})
