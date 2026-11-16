package com.megaman.maverick.game.weapons

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.time.Timer
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues

open class WeaponState(cooldown: Float) : Updatable {

    companion object {
        const val TAG = "WeaponState"
    }

    val cooldown = Timer(cooldown)
    var ammo = MegamanValues.MAX_WEAPON_AMMO
    val spawned = OrderedMap<MegaChargeStatus, OrderedSet<MegaGameEntity>>()

    init {
        MegaChargeStatus.entries.forEach { spawned.put(it, OrderedSet()) }
    }

    open fun canFire(stat: MegaChargeStatus) = true

    fun getSpawnedCount(stats: Iterable<MegaChargeStatus>): Int {
        var count = 0
        for (stat in stats) count += spawned[stat]?.size ?: 0
        return count
    }

    fun getAllSpawned(out: Array<MegaGameEntity>): Array<MegaGameEntity> {
        spawned.values().forEach { set -> set.forEach { out.add(it) } }
        return out
    }

    fun clearAllSpawned() = spawned.values().forEach { set -> set.clear() }

    fun addSpawned(stat: MegaChargeStatus, entity: MegaGameEntity) {
        spawned[stat].add(entity)
        GameLogger.debug(TAG, "addSpawned(): stat=$stat, entity=$entity, spawned=$spawned")
    }

    private fun cullAllDead() = spawned.values().forEach { set ->
        val iter = set.iterator()
        while (iter.hasNext) {
            val entity = iter.next()
            if (!entity.existenceState.liveAfterUpdate) {
                iter.remove()
                GameLogger.debug(TAG, "cullAllDead(): culled entity=$entity")
            }
        }
    }

    override fun update(delta: Float) {
        cullAllDead()
        cooldown.update(delta)
    }

    override fun toString() =
        "WeaponState{cooldown=${UtilMethods.roundFloat(cooldown.getRatio() * 100, 2)}%,ammo=$ammo}"
}
