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
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.MusicAsset
import com.maverick.game.engine.common.objects.Properties
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

private val LEVEL_MUSIC = MusicAsset.MMX6_BOSS_FIGHT_MUSIC

private class StageFight(end: Boolean? = null) {

    val received = ArrayList<Any>()
    val receivedEvents = ArrayList<Event>()
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

    private val players = MegaPlayers(capacity = 2)
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

    val second = SecondPlayerEntity(services).also {
        it.addComponent(LocksComponent(MegaLockGroup.entries.size))
        it.existenceState = EntityExistenceState.LIVE
        players.register(PlayerSlot(1), it)
    }

    private val mapProps = Properties().also { if (end != null) it.put(ConstKeys.END, end) }

    val fight = Fight(boss, FightPlans.find("BigAssMaverickRobot", mapProps)!!, services, bar) { LEVEL_MUSIC }

    init {
        services.events.addListener(object : IEventListener {
            override val eventKeyMask = objectSetOf<Any>(
                EventType.TURN_CONTROLLER_ON,
                EventType.TURN_CONTROLLER_OFF,
                EventType.VICTORY_EVENT,
                EventType.INTERMEDIATE_BOSS_DEAD
            )

            override fun onEvent(event: Event) {
                received.add(event.key)
                receivedEvents.add(event)
            }
        })
    }

    val damageLocked: Boolean
        get() = lead.getComponent(LocksComponent::class)!!.isLocked(MegaLockGroup.DAMAGE)

    val secondDamageLocked: Boolean
        get() = second.getComponent(LocksComponent::class)!!.isLocked(MegaLockGroup.DAMAGE)

    fun count(key: EventType) = received.count { it == key }

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

        it("sends VICTORY_EVENT with the boss, whatever event the boss gives") {
            val stage = StageFight()
            every { stage.boss.getEventOnDefeated() } returns EventType.INTERMEDIATE_BOSS_DEAD
            stage.throughIntro()

            stage.defeatAndDestroyBoss(leadDiesFirst = false)

            val victory = stage.receivedEvents.single { it.key == EventType.VICTORY_EVENT }
            victory.getProperty(ConstKeys.BOSS) shouldBe stage.boss
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

        it("does not resume, with end=true and with no end") {
            listOf(true, null).forEach { end ->
                val stage = StageFight(end)
                stage.throughIntro()

                stage.defeatAndDestroyBoss(leadDiesFirst = false)

                stage.count(EventType.VICTORY_EVENT) shouldBe 1
                stage.received shouldNotContain EventType.INTERMEDIATE_BOSS_DEAD
                stage.received shouldNotContain EventType.TURN_CONTROLLER_ON
                stage.damageLocked shouldBe true
                stage.secondDamageLocked shouldBe true
                verify(exactly = 0) { stage.audio.playMusic(any(), any()) }
            }
        }
    }

    describe("the mid-level boss plan at the defeat") {
        it("opens the way and resumes once when slot 0 is alive, and stays open until the boss is gone") {
            val stage = StageFight(end = false)
            stage.throughIntro()

            stage.defeatAndDestroyBoss(leadDiesFirst = false)

            stage.count(EventType.INTERMEDIATE_BOSS_DEAD) shouldBe 1
            stage.receivedEvents.single { it.key == EventType.INTERMEDIATE_BOSS_DEAD }
                .getProperty(ConstKeys.BOSS) shouldBe stage.boss
            stage.received shouldNotContain EventType.VICTORY_EVENT
            stage.count(EventType.TURN_CONTROLLER_ON) shouldBe 1
            stage.damageLocked shouldBe false
            stage.secondDamageLocked shouldBe false
            verify(exactly = 1) { stage.audio.playMusic(LEVEL_MUSIC, true) }
            verify(exactly = 1) { stage.audio.playMusic(any(), any()) }
            stage.fight.closed shouldBe false

            stage.bossState = EntityExistenceState.UNSPAWNED
            stage.tick()

            stage.fight.closed shouldBe true
            stage.count(EventType.INTERMEDIATE_BOSS_DEAD) shouldBe 1
            stage.count(EventType.TURN_CONTROLLER_ON) shouldBe 1
            verify(exactly = 1) { stage.audio.playMusic(any(), any()) }
        }

        it("sends the exit event in the tick after the boss destroy is queued, while the fight is open") {
            val stage = StageFight(end = false)
            stage.throughIntro()
            stage.bossDefeated = true
            stage.tick()
            stage.received shouldNotContain EventType.INTERMEDIATE_BOSS_DEAD

            stage.bossState = EntityExistenceState.DESTROY_QUEUED
            stage.tick()

            stage.count(EventType.INTERMEDIATE_BOSS_DEAD) shouldBe 1
            stage.fight.closed shouldBe false
        }

        it("does nothing, and keeps the fight open until a cancel, when slot 0 dies in the same frame as the boss") {
            val stage = StageFight(end = false)
            stage.throughIntro()

            stage.defeatAndDestroyBoss(leadDiesFirst = true)
            stage.bossState = EntityExistenceState.UNSPAWNED
            stage.tick()

            stage.received shouldNotContain EventType.INTERMEDIATE_BOSS_DEAD
            stage.received shouldNotContain EventType.TURN_CONTROLLER_ON
            verify(exactly = 0) { stage.audio.playMusic(any(), any()) }
            stage.fight.closed shouldBe false

            stage.fight.cancel()

            stage.fight.closed shouldBe true
            stage.received shouldNotContain EventType.INTERMEDIATE_BOSS_DEAD
        }
    }
})
