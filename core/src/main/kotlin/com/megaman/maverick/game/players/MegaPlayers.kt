package com.megaman.maverick.game.players

import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.hasBody
import com.maverick.game.engine.events.Signal
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.Players
import com.maverick.game.engine.players.any
import com.maverick.game.engine.players.forEach
import com.maverick.game.engine.players.playerSlot
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.input.PlayerInputs
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.playPhase

class MegaPlayers(capacity: Int = 1, val inputs: PlayerInputs = PlayerInputs(capacity)) :
    Players<MegaGameEntity>(capacity) {

    private val settings = Array(capacity) { PlayerSettings() }

    fun settings(slot: PlayerSlot) = settings[slot.index]

    val menuInput get() = inputs[PlayerSlot(0)]

    override fun register(slot: PlayerSlot, entity: MegaGameEntity): MegaGameEntity {
        super.register(slot, entity)
        // TODO: unregistering a player does not unbind its input source; players who drop out are a co-op rule for
        //  item 047
        entity.getComponent(ControllerComponent::class)?.input = InputSource { inputs[slot] }
        return entity
    }

    // TODO: this is the default for every entity, and a target can flip between 2 players at almost equal distance.
    //  Targeting should not be a service-wide function, and targetPlayer should not exist on every entity: each entity
    //  that needs a target should own its rule as a policy (see "Targeting design" in
    //  docs/mega-refactor/items/029-player-contract/NOTES.md); item 112
    fun targetFor(seeker: MegaGameEntity): MegaGameEntity {
        val first = firstLive() ?: return firstWithBody()
        if (!seeker.hasBody || !anyLive { it != first }) return first

        val center = seeker.body.getCenter()
        var nearest: MegaGameEntity = first
        var nearestDistance = center.dst2(first.body.getCenter())
        forEachLive {
            val distance = center.dst2(it.body.getCenter())
            if (distance < nearestDistance) {
                nearest = it
                nearestDistance = distance
            }
        }
        return nearest
    }

    fun isLive(player: MegaGameEntity) = player.hasBody && player.existenceState.liveNow && player.inPlay

    fun firstLive(): MegaGameEntity? {
        forEach { if (isLive(it)) return it }
        return null
    }

    fun anyOtherPresent(slot: PlayerSlot): Boolean {
        forEach {
            if (it.playerSlot != slot && it.existenceState.liveNow && it.playPhase != PlayPhase.POST_PLAY) return true
        }
        return false
    }

    inline fun forEachLive(action: (MegaGameEntity) -> Unit) {
        val noneLive = firstLive() == null
        forEach { if (noneLive || isLive(it)) action(it) }
    }

    // TODO: the other players.any sites, for example the scanners of Cactus and FireDispensenator, still count a dead
    //  slot 1 at its last position; this gives a false positive, not a stuck state (item 047e)
    inline fun anyLive(predicate: (MegaGameEntity) -> Boolean): Boolean {
        val noneLive = firstLive() == null
        return any { (noneLive || isLive(it)) && predicate(it) }
    }

    private fun firstWithBody(): MegaGameEntity {
        forEach { if (it.hasBody) return it }
        return get(PlayerSlot(0))!!
    }

    val playerDamaged = Signal<PlayerDamaged>()
    private val playerDamagedPayload = PlayerDamaged()

    fun onPlayerDamaged(player: MegaGameEntity, damager: IDamagerEntity) {
        playerDamagedPayload.player = player
        playerDamagedPayload.damager = damager
        playerDamaged.emit(playerDamagedPayload)
    }
}
