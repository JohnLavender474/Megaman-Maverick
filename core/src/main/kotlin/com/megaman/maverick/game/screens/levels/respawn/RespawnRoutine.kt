package com.megaman.maverick.game.screens.levels.respawn

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.state.Routine
import com.maverick.game.engine.state.Step
import com.maverick.game.engine.common.extensions.gdxArrayOf

class RespawnRoutine(private val context: RespawnContext) {

    companion object {
        const val TAG = "RespawnRoutine"

        private val AFTER_DEFEAT = listOf<Step<RespawnContext>>(
            WaitAndFadeOut, CheckLives, AwaitEventsRun, BeginRespawn, BeamUntilEventsRun, ResetLevel, BeamIn, Ready
        )
        private val AFTER_BEGIN_RESPAWN = listOf<Step<RespawnContext>>(BeamUntilEventsRun, ResetLevel, BeamIn, Ready)
    }

    val finished: Boolean
        get() = routine?.finished ?: true

    val defeatRunning: Boolean
        get() = !finished && !context.respawnBegun

    private var routine: Routine<RespawnContext>? = null
    private var deathWaits = false

    fun startAtDeath() {
        GameLogger.debug(TAG, "startAtDeath(): finished=$finished")
        val waitForRespawn = !finished && context.respawnBegun
        defeat(context)
        if (waitForRespawn) deathWaits = true else start(AFTER_DEFEAT)
    }

    fun startAtRespawn() {
        GameLogger.debug(TAG, "startAtRespawn()")
        beginRespawn(context)
        start(AFTER_BEGIN_RESPAWN)
    }

    fun update(delta: Float) {
        context.delta = delta
        routine?.update(context)

        if (deathWaits && finished) {
            GameLogger.debug(TAG, "update(): the respawn is done, start the death that waited")
            deathWaits = false
            start(AFTER_DEFEAT)
        }
    }

    fun cancel() {
        GameLogger.debug(TAG, "cancel()")
        routine?.cancel(context)
        routine = null
        deathWaits = false
    }

    private fun start(steps: List<Step<RespawnContext>>) {
        routine?.cancel(context)
        routine = Routine(gdxArrayOf(*steps.toTypedArray()))
    }
}
