package com.megaman.maverick.game.screens.levels.respawn

import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.players.forEach
import com.maverick.game.engine.players.playerSlot
import com.maverick.game.engine.state.Step
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.setPlayPhase
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.screens.ScreenEnum

fun defeat(context: RespawnContext) {
    val game = context.game
    val lead = context.lead

    game.players.forEach { if (it != lead) game.engine.destroy(it) }

    game.audio.unsetMusic()
    game.audio.stopAllLoopingSounds()

    game.gameState.removeLife(lead.playerSlot!!)

    context.respawnBegun = false

    context.deathFade.init()
    game.events.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))
    lead.body.physics.gravityOn = false
    lead.setPlayPhase(PlayPhase.POST_PLAY)
    game.audio.playSound(SoundAsset.DEFEAT_SOUND)

    if (game.clock.paused) game.resume()
}

fun beginRespawn(context: RespawnContext) {
    val game = context.game

    context.beam.init()

    game.players.forEach {
        it.setPlayPhase(PlayPhase.PRE_PLAY)
        it.lock(MegaLockGroup.DAMAGE, MegaLockReason.SPAWN)
        it.body.physics.gravityOn = false
        it.lock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPAWN)
    }

    game.events.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))

    game.levelState.megamanBeam = true

    context.eventsRunMark = game.events.runCount
    context.respawnBegun = true
}

object WaitAndFadeOut : Step<RespawnContext> {

    override fun isFinished(context: RespawnContext): Boolean {
        context.deathFade.update(context.delta)
        return context.deathFade.finished
    }

    override fun end(context: RespawnContext, interrupted: Boolean) {
        if (interrupted) return
        context.game.events.submitEvent(
            Event(EventType.PLAYER_DONE_DYIN, props(PlayerEventKeys.SLOT pairTo context.lead.playerSlot!!))
        )
    }
}

object CheckLives : Step<RespawnContext> {

    override fun begin(context: RespawnContext) {
        val game = context.game

        val music = context.levelMusic()

        context.gameOver = game.gameState.livesExhausted()
        if (context.gameOver) {
            game.runQueue.addLast {
                music?.let { game.audio.playMusic(it, it.loop) }
                game.setCurrentScreen(ScreenEnum.GAME_OVER_SCREEN.name)
                game.gameState.resetLives()
            }
            return
        }

        music?.let { game.audio.playMusic(it, it.loop) }
        context.deathFade.setToEnd()
    }

    override fun isFinished(context: RespawnContext) = !context.gameOver
}

object AwaitEventsRun : Step<RespawnContext> {

    override fun begin(context: RespawnContext) {
        context.eventsRunMark = context.game.events.runCount
    }

    override fun isFinished(context: RespawnContext) = context.game.events.runCount > context.eventsRunMark
}

object BeginRespawn : Step<RespawnContext> {

    override fun begin(context: RespawnContext) = beginRespawn(context)

    override fun isFinished(context: RespawnContext) = true
}

object BeamUntilEventsRun : Step<RespawnContext> {

    override fun isFinished(context: RespawnContext): Boolean {
        if (context.game.events.runCount > context.eventsRunMark) return true
        context.beam.update(context.delta)
        return false
    }
}

object ResetLevel : Step<RespawnContext> {

    override fun begin(context: RespawnContext) = context.resetLevel()

    override fun isFinished(context: RespawnContext) = true
}

object BeamIn : Step<RespawnContext> {

    override fun isFinished(context: RespawnContext): Boolean {
        context.beam.update(context.delta)
        return context.beam.finished
    }
}

object Ready : Step<RespawnContext> {

    override fun begin(context: RespawnContext) {
        val game = context.game

        game.players.forEach {
            it.body.physics.gravityOn = true
            it.unlock(MegaLockGroup.DAMAGE, MegaLockReason.SPAWN)
            it.setPlayPhase(PlayPhase.IN_PLAY)
            it.unlock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPAWN)
        }

        game.events.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
        game.events.submitEvent(
            Event(EventType.PLAYER_READY, props(PlayerEventKeys.SLOT pairTo context.lead.playerSlot!!))
        )

        game.audio.playSound(SoundAsset.BEAM_SOUND)

        game.levelState.megamanBeam = false
    }

    override fun isFinished(context: RespawnContext) = true
}
