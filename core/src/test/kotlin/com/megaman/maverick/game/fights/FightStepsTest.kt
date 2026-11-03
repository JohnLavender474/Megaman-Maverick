package com.megaman.maverick.game.fights

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.systems.GameSystem
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.SecondPlayerEntity
import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

private class StageFight {

    val received = ArrayList<Any>()
    val audio = mockk<MegaAudioManager>(relaxed = true)
    val bar = mockk<BossHealthHandler>(relaxed = true).also { every { it.finished } returns true }

    var bossDefeated = false
    var bossState = EntityExistenceState.LIVE
    val boss = mockk<AbstractBoss>(relaxed = true).also {
        every { it.ready } returns true
        every { it.defeated } answers { bossDefeated }
        every { it.existenceState } answers { bossState }
        every { it.getEventOnDefeated() } returns EventType.VICTORY_EVENT
    }

    private val players = MegaPlayers()
    private val services: MegaServices = testServices(
        engine = mockk<GameEngine>(relaxed = true).also { every { it.systems } returns Array<GameSystem>() },
        audio = audio,
        players = players
    )

    val lead = SecondPlayerEntity(services).also {
        it.addComponent(LocksComponent(MegaLockGroup.entries.size))
        it.existenceState = EntityExistenceState.LIVE
        players.register(PlayerSlot(0), it)
    }

    val fight = Fight(boss, FightPlans.find("BigAssMaverickRobot")!!, services, bar)

    init {
        services.events.addListener(object : IEventListener {
            override val eventKeyMask = objectSetOf<Any>(
                EventType.TURN_CONTROLLER_ON, EventType.TURN_CONTROLLER_OFF, EventType.VICTORY_EVENT
            )

            override fun onEvent(event: Event) {
                received.add(event.key)
            }
        })
    }

    val damageLocked: Boolean
        get() = lead.getComponent(LocksComponent::class)!!.isLocked(MegaLockGroup.DAMAGE)

    fun tick() {
        fight.update()
        fight.services.events.run()
    }

    fun throughIntro() {
        tick()
        received shouldContain EventType.TURN_CONTROLLER_ON
        received.clear()
    }

    fun defeatAndDestroyBoss(leadDiesFirst: Boolean) {
        if (leadDiesFirst) lead.existenceState = EntityExistenceState.DESTROY_QUEUED
        bossDefeated = true
        bossState = EntityExistenceState.DESTROY_QUEUED
        tick()
        tick()
    }
}

class FightStepsTest : DescribeSpec({

    describe("the stage boss plan at the defeat") {
        it("locks, stops the music, hides the bar and ends the level when slot 0 is alive") {
            val stage = StageFight()
            stage.throughIntro()

            stage.defeatAndDestroyBoss(leadDiesFirst = false)

            stage.received shouldContain EventType.TURN_CONTROLLER_OFF
            stage.received shouldContain EventType.VICTORY_EVENT
            stage.damageLocked shouldBe true
            verify(exactly = 1) { stage.audio.unsetMusic() }
            verify(exactly = 1) { stage.bar.unset() }
            stage.fight.closed shouldBe true
        }

        it("does nothing, and keeps the fight open, when slot 0 dies in the same frame as the boss") {
            val stage = StageFight()
            stage.throughIntro()

            stage.defeatAndDestroyBoss(leadDiesFirst = true)

            stage.received shouldNotContain EventType.TURN_CONTROLLER_OFF
            stage.received shouldNotContain EventType.VICTORY_EVENT
            stage.damageLocked shouldBe false
            verify(exactly = 0) { stage.audio.unsetMusic() }
            verify(exactly = 0) { stage.bar.unset() }
            stage.fight.closed shouldBe false
        }

        it("releases the bar once when the level reset cancels that open fight") {
            val stage = StageFight()
            stage.throughIntro()
            stage.defeatAndDestroyBoss(leadDiesFirst = true)

            stage.fight.cancel()

            verify(exactly = 1) { stage.bar.unset() }
            stage.received shouldNotContain EventType.VICTORY_EVENT
            stage.fight.closed shouldBe true
        }
    }
})
