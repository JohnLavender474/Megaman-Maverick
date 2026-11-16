package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.entities.EntityExistenceState
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.focus
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.PreciousGemCluster
import com.megaman.maverick.game.weapons.WeaponState
import kotlin.math.min

class MoonScythesState(cooldown: Float) : WeaponState(cooldown) {

    private companion object {
        val OLDEST_FIRST = Comparator<MoonScythe> { o1, o2 -> sortOrder(o1).compareTo(sortOrder(o2)) }

        fun sortOrder(scythe: MoonScythe) =
            if (scythe.existenceState == EntityExistenceState.LIVE) scythe.spawnOrder else Long.MAX_VALUE
    }

    val maxWaitTime = Timer(1f).setToEnd()
    private val outArray = Array<MegaGameEntity>()
    private val countedScythes = Array<MoonScythe>()

    override fun canFire(stat: MegaChargeStatus): Boolean {
        if (maxWaitTime.isFinished()) return true
        val count = getSpawnedCount(MegaChargeStatus.entries)
        GameLogger.debug(TAG, "MoonScythesState.canFire(): count=${count}")
        return count <= MegamanValues.MAX_MOONS_BEFORE_SHOOT_AGAIN
    }

    fun fadeExtraScythes() {
        outArray.clear()
        getAllSpawned(outArray).forEach { entity ->
            val scythe = entity as MoonScythe
            val existence = scythe.existenceState
            if (existence.liveAfterUpdate && (existence != EntityExistenceState.LIVE || !scythe.fading))
                countedScythes.add(scythe)
        }
        val extraCount = countedScythes.size - MegamanValues.MAX_MOON_SCYTHES
        if (extraCount > 0) {
            countedScythes.sort(OLDEST_FIRST)
            for (i in 0 until extraCount) countedScythes[i].fading = true
        }
        countedScythes.clear()
    }

    override fun update(delta: Float) {
        super.update(delta)
        maxWaitTime.update(delta)
    }
}

class PreciousGuardState(private val megaman: Megaman, cooldown: Float) : WeaponState(cooldown) {

    val maxCooldownTimer = Timer(1f).setToEnd()

    val cost: Int
        get() = if (spawned[MegaChargeStatus.NOT_CHARGED].size > 0) 0 else 6

    override fun canFire(stat: MegaChargeStatus): Boolean {
        if (spawned[MegaChargeStatus.NOT_CHARGED].size > 0) {
            val cluster = spawned.get(MegaChargeStatus.NOT_CHARGED).first() as PreciousGemCluster
            return cluster.gems.values().none { it.released }
        }
        return maxCooldownTimer.isFinished() && getSpawnedCount(MegaChargeStatus.entries) == 0
    }

    private fun updateCluster(cluster: PreciousGemCluster, delta: Float) {
        val iter = cluster.gems.keys().iterator()
        while (iter.hasNext) {
            val gem = iter.next()
            if (!gem.existenceState.liveAfterUpdate) iter.remove()
        }
        if (!cluster.existenceState.liveNow) return
        cluster.origin.lerp(
            megaman.focus.getFocusPosition(),
            min(1f, MegamanValues.SHIELD_GEM_LERP * ConstVals.PPM * delta)
        )
        if (cluster.gems.values().none { it.released }) cluster.body.setCenter(cluster.origin)
    }

    override fun update(delta: Float) {
        super.update(delta)
        spawned.forEach { entry -> entry.value.forEach { updateCluster(it as PreciousGemCluster, delta) } }
        maxCooldownTimer.update(delta)
        val spawnedIter = spawned.values().iterator()
        while (spawnedIter.hasNext) {
            val clustersIter = spawnedIter.next().iterator()
            while (clustersIter.hasNext) {
                val cluster = clustersIter.next() as PreciousGemCluster
                if (cluster.gems.values().any { it.released }) clustersIter.remove()
            }
        }
    }
}

class RodentClawsState(cooldown: Float) : WeaponState(cooldown) {

    var slashIndex = 0
        private set
    val keySlashIndex: Int
        get() = if (slashIndex == 0) 1 else slashIndex

    private val resetTimer = Timer(0.75f).setToEnd()
    private val refillTimer = Timer(0.125f)
    private val refillDelay = Timer(0.3f).setToEnd()

    fun nextSlash(comboBroken: Boolean) {
        slashIndex = if (slashIndex >= 3 || resetTimer.isFinished() || comboBroken) 1 else slashIndex + 1
        resetTimer.reset()
        refillDelay.reset()
    }

    override fun update(delta: Float) {
        super.update(delta)
        resetTimer.update(delta)
        refillDelay.update(delta)
        if (refillDelay.isFinished() && ammo < MegamanValues.MAX_WEAPON_AMMO) {
            refillTimer.update(delta)
            if (refillTimer.isFinished()) {
                ammo += 1
                refillTimer.reset()
            }
        }
    }
}
