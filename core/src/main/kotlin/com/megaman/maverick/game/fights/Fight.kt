package com.megaman.maverick.game.fights

import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.state.Routine
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler

class Fight(
    val boss: AbstractBoss,
    private val plan: FightPlan,
    val services: MegaServices,
    val bar: BossHealthHandler,
    val levelMusic: () -> MusicAsset?
) {

    private enum class Phase { INTRO, WAIT, OUTRO, CLOSED }

    val closed: Boolean
        get() = phase == Phase.CLOSED

    // TODO: slot 0 alone decides the flow, as in the old screen branches; co-op rules for a fight are item 047e
    val leadPlayerAlive: Boolean
        get() = services.players.get(PlayerSlot(0))!!.existenceState.liveAfterUpdate

    private val intro = Routine(plan.intro)
    private val outro = Routine(plan.outro)
    private val leases = OrderedMap<FightLease, Lease>()

    private var phase = Phase.INTRO

    fun lease(key: FightLease, lease: Lease) {
        leases.put(key, lease)
    }

    fun release(key: FightLease) {
        leases.remove(key)?.release()
    }

    fun update(delta: Float) {
        if (phase == Phase.INTRO) {
            if (plan.win.isMet(this)) {
                intro.cancel(this)
                releaseLeases()
                phase = Phase.OUTRO
            } else {
                intro.update(this)
                if (intro.finished) phase = Phase.WAIT
            }
        }

        if (phase == Phase.WAIT && plan.win.isMet(this)) phase = Phase.OUTRO

        if (phase == Phase.OUTRO) {
            outro.update(this)
            if (outro.finished) close()
        }

        if (!bar.finished) bar.update(delta)
    }

    fun cancel() {
        intro.cancel(this)
        outro.cancel(this)
        close()
    }

    private fun close() {
        releaseLeases()
        phase = Phase.CLOSED
    }

    private fun releaseLeases() {
        val keys = leases.orderedKeys()
        for (i in keys.size - 1 downTo 0) leases.get(keys[i]).release()
        leases.clear()
    }
}
