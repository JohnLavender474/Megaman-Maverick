package com.megaman.maverick.game.players

import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.setPlayPhase
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlayerDeathEventTest : DescribeSpec({

    val players = MegaPlayers(2)
    val services = testServices(players = players)
    val first = TestHealthPlayer(services).also {
        players.register(PlayerSlot(0), it)
        it.existenceState = EntityExistenceState.LIVE
    }
    val second = TestHealthPlayer(services).also {
        players.register(PlayerSlot(1), it)
        it.existenceState = EntityExistenceState.LIVE
    }

    fun deathOf(slot: Int) = Event(EventType.PLAYER_JUST_DIED, props(PlayerEventKeys.SLOT pairTo PlayerSlot(slot)))

    describe("isDeathOf") {
        it("is true only for the player whose slot died") {
            deathOf(1).isDeathOf(second) shouldBe true
            deathOf(1).isDeathOf(first) shouldBe false
        }
    }

    describe("anyOtherPresent") {
        it("is true while another slot is in the level, and false when only the dead slot is left") {
            players.anyOtherPresent(PlayerSlot(1)) shouldBe true
            first.existenceState = EntityExistenceState.UNSPAWNED
            players.anyOtherPresent(PlayerSlot(1)) shouldBe false
            players.anyOtherPresent(PlayerSlot(0)) shouldBe true
            first.existenceState = EntityExistenceState.LIVE
        }

        it("counts a player that beams in, and not one that the defeat ended") {
            first.setPlayPhase(PlayPhase.PRE_PLAY)
            players.anyOtherPresent(PlayerSlot(1)) shouldBe true
            first.setPlayPhase(PlayPhase.POST_PLAY)
            players.anyOtherPresent(PlayerSlot(1)) shouldBe false
            first.setPlayPhase(PlayPhase.IN_PLAY)
        }
    }
})
