package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable

class SpawnsManager : Updatable, Resettable {

    companion object {
        const val TAG = "SpawnsManager"
    }

    internal val spawns = Array<Spawn>()
    internal val spawners = Array<ISpawner>()

    fun getSpawnsAndClear(): Array<Spawn> {
        val spawnsToReturn = Array(spawns)
        spawns.clear()
        return spawnsToReturn
    }

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

            if (spawner.test(delta)) {
                val spawn = spawner.get()
                spawns.add(spawn)

                if (!spawner.respawnable) {
                    spawner.reset()
                    iter.remove()
                }
            }
        }
    }

    override fun reset() {
        spawners.clear()
        spawns.clear()
    }
}
