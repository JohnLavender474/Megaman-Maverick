package com.megaman.maverick.game.screens.levels.respawn

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.facing
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.forEach
import com.maverick.game.engine.players.playerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.state.DamageRecoveryComponent
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.setPlayPhase
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.screens.levels.camera.PartnerSpot
import com.megaman.maverick.game.state.SlotRespawnRule

class SlotRespawn(
    private val services: MegaServices,
    private val rule: SlotRespawnRule,
    private val spots: PartnerSpot,
    private val beams: Array<BeamDown>,
    private val spawnProps: (PlayerSlot) -> Properties,
    private val hold: () -> Boolean
) : Updatable, Resettable {

    companion object {
        const val DELAY = 1f
        const val CAP = 3f
    }

    init {
        require(rule != SlotRespawnRule.OFF) { "SlotRespawn needs a slot respawn rule other than OFF" }
    }

    private enum class Stage { NONE, WAIT, SPAWN, BEAM }

    private val players = services.players
    private val stages = Array(players.capacity) { Stage.NONE }
    private val times = FloatArray(players.capacity)
    private val best = Vector2()
    private val fallback = Vector2()

    fun onDeath(player: IPlayer, routineRunning: Boolean, defeatRunning: Boolean) = when {
        rule == SlotRespawnRule.AT_SPAWN -> {
            start(player)
            false
        }
        defeatRunning -> {
            player.setPlayPhase(PlayPhase.POST_PLAY)
            false
        }
        routineRunning -> player.playerSlot == PlayerSlot(0)
        players.anyOtherPresent(player.slot) -> {
            start(player)
            false
        }
        else -> {
            reset()
            player.setPlayPhase(PlayPhase.POST_PLAY)
            true
        }
    }

    private fun start(player: IPlayer) {
        val slot = player.slot
        if (rule.costsLife) {
            services.gameState.removeLife(slot)
            if (services.gameState.livesExhausted()) {
                player.setPlayPhase(PlayPhase.POST_PLAY)
                return
            }
        }
        stages[slot.index] = Stage.WAIT
        times[slot.index] = 0f
        player.setPlayPhase(PlayPhase.PRE_PLAY)
    }

    override fun update(delta: Float) = players.forEach { player ->
        val slot = player.slot.index
        when (stages[slot]) {
            Stage.WAIT -> waitForSpot(player, slot, delta)
            Stage.SPAWN -> if (player.existenceState.liveNow) startBeam(player, slot)
            Stage.BEAM -> beamIn(player, slot, delta)
            Stage.NONE -> {}
        }
    }

    override fun reset() {
        for (i in stages.indices) {
            stages[i] = Stage.NONE
            beams[i].stop()
        }
    }

    private fun waitForSpot(player: IPlayer, slot: Int, delta: Float) {
        if (hold()) return
        times[slot] += delta
        if (times[slot] < DELAY) return

        if (rule == SlotRespawnRule.AT_SPAWN) {
            services.engine.spawn(player, spawnProps(player.slot))
            stages[slot] = Stage.SPAWN
            return
        }

        val partner = partnerOf(player) ?: return
        val ready = spots.find(player, partner, best, fallback)
        val spot = spots.landing(player, ready, times[slot] >= DELAY + CAP, best, fallback) ?: return

        val bounds = GameRectangle().setSize(player.body.getWidth(), player.body.getHeight()).setCenter(spot.x, spot.y)
        services.engine.spawn(
            player, props(
                ConstKeys.BOUNDS pairTo bounds,
                ConstKeys.DIRECTION pairTo services.gravity.direction.name.lowercase(),
                ConstKeys.FACING pairTo partner.facing.name.lowercase()
            )
        )
        stages[slot] = Stage.SPAWN
    }

    private fun startBeam(player: IPlayer, slot: Int) {
        player.setPlayPhase(PlayPhase.PRE_PLAY)
        player.body.physics.gravityOn = false
        player.lock(MegaLockGroup.DAMAGE, MegaLockReason.SPAWN)
        player.lock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPAWN)
        player.lock(MegaLockGroup.MOVEMENT, MegaLockReason.SPAWN)
        stages[slot] = Stage.BEAM
        beams[slot].start(player)
    }

    private fun beamIn(player: IPlayer, slot: Int, delta: Float) {
        beams[slot].update(delta)
        if (!beams[slot].finished) return

        stages[slot] = Stage.NONE
        player.body.physics.gravityOn = true
        player.unlock(MegaLockGroup.DAMAGE, MegaLockReason.SPAWN)
        player.unlock(MegaLockGroup.BEHAVIORS, MegaLockReason.SPAWN)
        player.unlock(MegaLockGroup.MOVEMENT, MegaLockReason.SPAWN)
        player.setPlayPhase(PlayPhase.IN_PLAY)
        if (rule.spawnProtection) player.require(DamageRecoveryComponent::class).start()
        services.audio.playSound(SoundAsset.BEAM_SOUND)
    }

    private fun partnerOf(player: IPlayer): IPlayer? {
        players.forEach { if (it != player && players.isLive(it) && spots.isInView(it)) return it }
        return null
    }
}
