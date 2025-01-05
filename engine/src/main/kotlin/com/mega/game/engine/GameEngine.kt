package com.mega.game.engine

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.*
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.systems.GameSystem

class GameEngine(
    var systems: Array<GameSystem> = Array(),
    var onQueueToSpawn: ((IGameEntity) -> Unit)? = null,
    var onQueueToDestroy: ((IGameEntity) -> Unit)? = null
) : Updatable, Resettable, Disposable {

    internal class EntitiesToSpawn {

        private val queue = Queue<GamePair<IGameEntity, Properties>>()
        private val set = ObjectSet<IGameEntity>()

        internal fun add(entity: IGameEntity, spawnProps: Properties) =
            if (contains(entity)) false
            else {
                queue.addLast(entity pairTo spawnProps)
                set.add(entity)
                true
            }

        internal fun contains(entity: IGameEntity) = set.contains(entity)

        internal fun isEmpty() = queue.isEmpty

        internal fun poll(): GamePair<IGameEntity, Properties> {
            val pair = queue.removeFirst()
            set.remove(pair.first)
            return pair
        }

        internal fun clear() {
            queue.clear()
            set.clear()
        }
    }

    var updating = false
        private set

    private val entities = MutableOrderedSet<IGameEntity>()

    private val entitiesToSpawn = EntitiesToSpawn()
    private val entitiesToKill = SimpleQueueSet<IGameEntity>()

    private var reset = false
    private var disposed = false

    fun contains(entity: IGameEntity, containedIfQueuedToSpawn: Boolean = false) =
        entities.contains(entity) || (containedIfQueuedToSpawn && entitiesToSpawn.contains(entity))

    fun spawn(entity: IGameEntity, spawnProps: Properties = Properties()) = if (entity.canSpawn(spawnProps)) {
        val queued = entitiesToSpawn.add(entity, spawnProps)
        if (queued) onQueueToSpawn?.invoke(entity)
        queued
    } else false

    private fun spawnNow(entity: IGameEntity, spawnProps: Properties) {
        entities.add(entity)
        if (!entity.initialized) initialize(entity)
        entity.onSpawn(spawnProps)
        updateSystemMembershipsFor(entity)
        entity.spawned = true
    }

    private fun initialize(entity: IGameEntity) {
        entity.init()
        entity.initialized = true
    }

    fun destroy(entity: IGameEntity): Boolean {
        val shouldBeQueued = entitiesToKill.add(entity)
        if (shouldBeQueued) onQueueToDestroy?.invoke(entity)
        return shouldBeQueued
    }

    private fun destroyNow(entity: IGameEntity) {
        entities.remove(entity)

        systems.forEach { s -> s.remove(entity) }

        entity.components.forEach { it.value.reset() }
        entity.onDestroy()
        entity.spawned = false
    }

    fun updateSystemMembershipsFor(entity: IGameEntity) =
        systems.forEach { system -> if (system.qualifies(entity)) system.add(entity) else system.remove(entity) }

    override fun update(delta: Float) {
        if (disposed) throw IllegalStateException("Cannot update game engine after it has been disposed")

        updating = true

        while (!entitiesToSpawn.isEmpty()) {
            val (entity, spawnProps) = entitiesToSpawn.poll()
            spawnNow(entity, spawnProps)
        }

        while (!entitiesToKill.isEmpty()) {
            val entity = entitiesToKill.remove()
            destroyNow(entity)
        }

        systems.forEach { it.update(delta) }

        updating = false

        if (reset) reset()
    }

    override fun reset() {
        reset = when {
            updating -> true
            else -> {
                entities.filter { it.spawned }.forEach { destroyNow(it) }
                entities.clear()

                entitiesToSpawn.clear()
                entitiesToKill.clear()

                systems.forEach { it.reset() }

                false
            }
        }
    }

    override fun dispose() {
        entities.filter { it.spawned }.forEach { destroyNow(it) }
        entities.clear()

        entitiesToSpawn.clear()
        entitiesToKill.clear()

        systems.forEach { it.reset() }
        systems.forEach { it.dispose() }

        disposed = true
    }
}
