package com.megaman.maverick.game.screens.levels.respawn

import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.locks.LocksComponent
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.playPhase
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.SecondPlayerEntity
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

private const val DEATH_FRAMES = 3
private const val BEAM_FRAMES = 4
private val LEVEL_MUSIC = MusicAsset.MM3_SNAKE_MAN_MUSIC

private class Respawn(livesLeft: Boolean = true) {

    var frame = 0
    var runs = 0L
    val log = ArrayList<String>()

    private val players = MegaPlayers(capacity = 2)
    private val services = testServices(engine = mockk<GameEngine>(relaxed = true), players = players)

    val lead = SecondPlayerEntity(services).also {
        it.addComponent(LocksComponent(MegaLockGroup.entries.size))
        it.addComponent(PlayPhaseComponent(PlayPhase.IN_PLAY))
        it.existenceState = EntityExistenceState.LIVE
        players.register(PlayerSlot(0), it)
    }

    val runQueue = Queue<() -> Unit>()
    val gameState = mockk<GameState>(relaxed = true).also { every { it.livesExhausted() } returns !livesLeft }

    val game = mockk<MegamanMaverickGame>(relaxed = true).also {
        every { it.players } returns players
        every { it.runQueue } returns runQueue
        every { it.gameState } returns gameState
        every { it.clock.paused } returns false
        every { it.events.submitEvent(any()) } answers { log.add("$frame:${firstArg<Event>().key}") }
        every { it.events.runCount } answers { runs }
        every { it.audio.playMusic(any(), any()) } answers { log.add("$frame:MUSIC") }
        every { it.setCurrentScreen(any(), any()) } answers { log.add("$frame:SCREEN") }
    }

    private var deathUpdates = 0
    val deathFade = mockk<DeathFadeOut>(relaxed = true).also {
        every { it.init() } answers { deathUpdates = 0 }
        every { it.update(any()) } answers { if (firstArg<Float>() > 0f) deathUpdates++ }
        every { it.finished } answers { deathUpdates >= DEATH_FRAMES }
    }

    private var beamUpdates = BEAM_FRAMES
    val beam = mockk<BeamInView>(relaxed = true).also {
        every { it.init() } answers { beamUpdates = 0 }
        every { it.update(any()) } answers { beamUpdates++ }
        every { it.finished } answers { beamUpdates >= BEAM_FRAMES }
    }

    val routine = RespawnRoutine(
        RespawnContext(game, deathFade, beam, resetLevel = { log.add("$frame:RESET") }, levelMusic = { LEVEL_MUSIC })
    )

    fun eventsRun() {
        runs++
    }

    fun frames(count: Int) = repeat(count) {
        frame++
        routine.update(0.1f)
        eventsRun()
    }

    fun deathEvent() {
        routine.startAtDeath()
    }
}

private fun Respawn.dieInRun() {
    deathEvent()
    eventsRun()
}

class RespawnRoutineTest : DescribeSpec({

    describe("RespawnRoutine at a death") {
        it("keeps the frames of the old event chain") {
            val r = Respawn()

            r.dieInRun()
            r.frames(DEATH_FRAMES + BEAM_FRAMES + 3)

            val d = DEATH_FRAMES
            r.log shouldBe listOf(
                "0:TURN_CONTROLLER_OFF",
                "$d:PLAYER_DONE_DYIN",
                "$d:MUSIC",
                "${d + 1}:TURN_CONTROLLER_OFF",
                "${d + 2}:RESET",
                "${d + 2}:LEVEL_RESET",
                "${d + BEAM_FRAMES}:TURN_CONTROLLER_ON",
                "${d + BEAM_FRAMES}:PLAYER_READY"
            )
            r.routine.finished shouldBe true
        }

        it("does the defeat at once, in the run of the death event") {
            val r = Respawn()

            r.deathEvent()

            verify(exactly = 1) { r.gameState.removeLife(PlayerSlot(0)) }
            r.lead.playPhase shouldBe PlayPhase.POST_PLAY
            r.routine.finished shouldBe false
        }

        it("puts the lead in play at ready") {
            val r = Respawn()

            r.dieInRun()
            r.frames(DEATH_FRAMES + BEAM_FRAMES)

            r.lead.playPhase shouldBe PlayPhase.IN_PLAY
        }

        it("goes to game over with no respawn step, and plays the music in the switch") {
            val r = Respawn(livesLeft = false)

            r.dieInRun()
            r.frames(DEATH_FRAMES + BEAM_FRAMES + 3)

            r.log shouldBe listOf("0:TURN_CONTROLLER_OFF", "$DEATH_FRAMES:PLAYER_DONE_DYIN")
            r.routine.finished shouldBe false
            r.runQueue.size shouldBe 1

            r.runQueue.removeLast().invoke()
            r.log.takeLast(2) shouldBe listOf("${r.frame}:MUSIC", "${r.frame}:SCREEN")
            verify(exactly = 1) { r.game.setCurrentScreen(ScreenEnum.GAME_OVER_SCREEN.name, any()) }
            verify(exactly = 1) { r.gameState.resetLives() }
        }

        it("sends no event when cancelled") {
            val r = Respawn()

            r.dieInRun()
            r.frames(1)
            r.routine.cancel()
            r.frames(DEATH_FRAMES + BEAM_FRAMES)

            r.log shouldBe listOf("0:TURN_CONTROLLER_OFF")
            r.routine.finished shouldBe true
        }

        it("lets a death during the beam-in wait for ready, as the old handlers did") {
            val r = Respawn()

            r.routine.startAtRespawn()
            r.frames(2)
            r.dieInRun()
            r.frames(BEAM_FRAMES - 2 + DEATH_FRAMES)

            r.log shouldBe listOf(
                "0:TURN_CONTROLLER_OFF",
                "2:RESET",
                "2:LEVEL_RESET",
                "2:TURN_CONTROLLER_OFF",
                "$BEAM_FRAMES:TURN_CONTROLLER_ON",
                "$BEAM_FRAMES:PLAYER_READY",
                "${BEAM_FRAMES + DEATH_FRAMES}:PLAYER_DONE_DYIN",
                "${BEAM_FRAMES + DEATH_FRAMES}:MUSIC"
            )
        }
    }

    describe("RespawnRoutine and a second death during the death steps") {
        it("starts the death steps again, with one fade out and one respawn, as the old handler did") {
            val r = Respawn()

            r.dieInRun()
            r.frames(1)
            r.dieInRun()
            r.frames(DEATH_FRAMES + BEAM_FRAMES + 3)

            r.log.count { it.endsWith("PLAYER_DONE_DYIN") } shouldBe 1
            r.log.count { it.endsWith(":RESET") } shouldBe 1
            r.log.count { it.endsWith(":LEVEL_RESET") } shouldBe 1
            r.log.filter { it.endsWith("PLAYER_DONE_DYIN") } shouldBe listOf("${1 + DEATH_FRAMES}:PLAYER_DONE_DYIN")
        }
    }

    describe("RespawnRoutine at the level start") {
        it("from a menu, resets in the first events run, as the old spawn handler did") {
            val r = Respawn()

            r.routine.startAtRespawn()
            r.eventsRun()
            r.frames(BEAM_FRAMES + 2)

            r.log shouldBe listOf(
                "0:TURN_CONTROLLER_OFF",
                "1:RESET",
                "1:LEVEL_RESET",
                "$BEAM_FRAMES:TURN_CONTROLLER_ON",
                "$BEAM_FRAMES:PLAYER_READY"
            )
            verify(exactly = 0) { r.gameState.removeLife(any()) }
        }

        it("from a launch with no events run before the first frame, resets one frame later") {
            val r = Respawn()

            r.routine.startAtRespawn()
            r.frames(BEAM_FRAMES + 2)

            r.log shouldBe listOf(
                "0:TURN_CONTROLLER_OFF",
                "2:RESET",
                "2:LEVEL_RESET",
                "$BEAM_FRAMES:TURN_CONTROLLER_ON",
                "$BEAM_FRAMES:PLAYER_READY"
            )
        }
    }
})
