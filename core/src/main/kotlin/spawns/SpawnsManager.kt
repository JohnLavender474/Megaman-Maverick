package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable

/**
 * Manages the spawning of entities. This class is responsible for updating all of the [ISpawner]s
 * and adding the [Spawn]s to the [getSpawnsAndClear] list. This class is also responsible for
 * culling the [ISpawner]s that should no longer be considered for spawning.
 *
 * @see Updatable
 * @see Resettable
 */
class SpawnsManager : Updatable, Resettable {

    companion object {
        const val TAG = "SpawnsManager"
    }

    internal val spawns = Array<Spawn>()
    internal val spawners = Array<ISpawner>()

    /**
     * Returns an array of the [Spawn]s that were spawned since the last update, and then clears the
     * array. This method should be called once per frame.
     *
     * @return the [Spawn]s that were spawned since the last update.
     */
    fun getSpawnsAndClear(): Array<Spawn> {
        val spawnsToReturn = Array(spawns)
        spawns.clear()
        return spawnsToReturn
    }

    /**
     * Sets the [ISpawner]s to manage.
     *
     * @param spawners the [ISpawner]s to manage.
     * @see ISpawner
     */
    fun setSpawners(spawners: Array<ISpawner>) {
        this.spawners.clear()
        this.spawners.addAll(spawners)
    }

    /**
     * Updates the [ISpawner]s and adds the [Spawn]s to the [getSpawnsAndClear] list. Also culls the
     * [ISpawner]s that should no longer be considered for spawning. This method should be called once
     * per frame. This method should be called before the [getSpawnsAndClear] method.
     *
     * @param delta the time in seconds since the last update.
     */
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

    /**
     * Clears the [ISpawner]s and [Spawn]s.
     */
    override fun reset() {
        spawners.clear()
        spawns.clear()
    }
}
