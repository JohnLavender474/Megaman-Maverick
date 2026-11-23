package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.controller.ControllerComponent
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.hasBody
import com.maverick.game.engine.events.Signal
import com.maverick.game.engine.input.InputSource
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.players.Players
import com.maverick.game.engine.players.any
import com.maverick.game.engine.players.forEach
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.input.PlayerInputs
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.playPhase

class MegaPlayers(capacity: Int = 1, val inputs: PlayerInputs = PlayerInputs(capacity)) :
    Players<IPlayer>(capacity) {

    private val settings = Array(capacity) { PlayerSettings() }
    private val botSlots = BooleanArray(capacity)

    fun settings(slot: PlayerSlot) = settings[slot.index]

    fun setBot(slot: PlayerSlot) {
        botSlots[slot.index] = true
    }

    // TODO: a slot agent (BotPlayerSlotAgent) replaces this flag, and a bot death becomes free (item 178e)
    fun isBot(slot: PlayerSlot) = botSlots[slot.index]

    val menuInput get() = inputs[PlayerSlot(0)]

    override fun register(slot: PlayerSlot, entity: IPlayer): IPlayer {
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
    fun targetFor(seeker: MegaGameEntity): IPlayer {
        if (!seeker.hasBody) return firstLive() ?: firstWithBody()
        return nearestLiveTo(seeker.body.getCenter())
    }

    fun nearestLiveTo(point: Vector2): IPlayer {
        val first = firstLive() ?: return firstWithBody()
        if (!anyLive { it != first }) return first

        var nearest: IPlayer = first
        var nearestDistance = point.dst2(first.body.getCenter())
        forEachLive {
            val distance = point.dst2(it.body.getCenter())
            if (distance < nearestDistance) {
                nearest = it
                nearestDistance = distance
            }
        }
        return nearest
    }

    fun isLive(player: IPlayer) = player.hasBody && player.existenceState.liveNow && player.inPlay

    fun firstLive(): IPlayer? {
        forEach { if (isLive(it)) return it }
        return null
    }

    fun anyOtherPresent(slot: PlayerSlot): Boolean {
        forEach {
            if (it.slot != slot && it.existenceState.liveNow && it.playPhase != PlayPhase.POST_PLAY) return true
        }
        return false
    }

    inline fun forEachLive(action: (IPlayer) -> Unit) {
        val noneLive = firstLive() == null
        forEach { if (noneLive || isLive(it)) action(it) }
    }

    inline fun anyLive(predicate: (IPlayer) -> Boolean): Boolean {
        val noneLive = firstLive() == null
        return any { (noneLive || isLive(it)) && predicate(it) }
    }

    private fun firstWithBody(): IPlayer {
        forEach { if (it.hasBody) return it }
        return get(PlayerSlot(0))!!
    }

    val playerDamaged = Signal<PlayerDamaged>()
    private val playerDamagedPayload = PlayerDamaged()

    fun onPlayerDamaged(player: IPlayer, damager: IDamagerEntity) {
        playerDamagedPayload.player = player
        playerDamagedPayload.damager = damager
        playerDamaged.emit(playerDamagedPayload)
    }
}
