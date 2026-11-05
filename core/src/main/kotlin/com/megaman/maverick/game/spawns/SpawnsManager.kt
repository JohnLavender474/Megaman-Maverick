package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class SpawnsManager(private val spawn: (MegaGameEntity, Properties) -> Boolean) : Updatable, Resettable {

    companion object {
        const val TAG = "SpawnsManager"
    }

    private val spawners = Array<ISpawner>()

    fun setSpawners(spawners: Array<ISpawner>) {
        this.spawners.clear()
        this.spawners.addAll(spawners)
    }

    override fun update(delta: Float) {
        val iter = spawners.iterator()

        while (iter.hasNext()) {
            val spawner = iter.next()

            if (spawner.shouldBeCulled(delta)) {
                spawner.reset()
                iter.remove()
                continue
            }

            spawner.track(delta)

            if (spawner.shouldTest(delta) && spawner.test(delta)) {
                val newSpawn = spawner.get()!!
                if (!spawn(newSpawn.entity, newSpawn.properties)) spawner.onSpawnRejected()

                if (!spawner.respawnable) {
                    spawner.reset()
                    iter.remove()
                }
            }
        }
    }

    fun rearm() = spawners.forEach { it.rearm() }

    override fun reset() {
        spawners.forEach { it.reset() }
        spawners.clear()
    }
}
