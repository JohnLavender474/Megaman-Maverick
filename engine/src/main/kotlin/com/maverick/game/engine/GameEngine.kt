package com.maverick.game.engine

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.Queue
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.objects.*
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem

class GameEngine(
    var systems: Array<GameSystem> = Array(),
    var onSpawnRejected: ((IGameEntity) -> Unit)? = null
) : Updatable, Resettable, Disposable {

    companion object {
        const val TAG = "GameEngine"
    }

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

        internal fun forEachEntity(action: (IGameEntity) -> Unit) = set.forEach(action)

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
        if (queued) onSpawnQueued(entity)
        queued
    } else {
        onSpawnRejected?.invoke(entity)
        false
    }

    private fun onSpawnQueued(entity: IGameEntity) {
        val destroyQueued = entitiesToKill.contains(entity)
        when (entity.existenceState) {
            EntityExistenceState.UNSPAWNED ->
                if (destroyQueued) logSpawnWhileDestroyQueued(entity)
                else setExistenceState(entity, EntityExistenceState.SPAWN_QUEUED)

            EntityExistenceState.LIVE ->
                GameLogger.debug(TAG, "spawn(): the entity is live, so onSpawn runs again: $entity")

            EntityExistenceState.DESTROY_QUEUED -> if (destroyQueued) logSpawnWhileDestroyQueued(entity)
            EntityExistenceState.SPAWN_QUEUED -> {}
        }
    }

    private fun logSpawnWhileDestroyQueued(entity: IGameEntity) =
        GameLogger.debug(TAG, "spawn(): a destroy is queued, so the entity ends destroyed: $entity")

    private fun spawnNow(entity: IGameEntity, spawnProps: Properties) {
        entities.add(entity)

        if (!entity.initialized) initialize(entity)
        entity.onSpawn(spawnProps)
        setExistenceState(
            entity,
            if (entitiesToKill.contains(entity)) EntityExistenceState.DESTROY_QUEUED else EntityExistenceState.LIVE
        )

        updateSystemMembershipsFor(entity)
    }

    private fun initialize(entity: IGameEntity) {
        entity.init()
        entity.initialized = true
    }

    fun destroy(entity: IGameEntity): Boolean {
        val shouldBeQueued = entitiesToKill.add(entity)
        if (shouldBeQueued) onDestroyQueued(entity)
        return shouldBeQueued
    }

    private fun onDestroyQueued(entity: IGameEntity) = when (entity.existenceState) {
        EntityExistenceState.LIVE -> setExistenceState(entity, EntityExistenceState.DESTROY_QUEUED)
        EntityExistenceState.SPAWN_QUEUED -> setExistenceState(entity, EntityExistenceState.UNSPAWNED)
        EntityExistenceState.UNSPAWNED ->
            GameLogger.debug(TAG, "destroy(): the entity is not spawned, so onDestroy runs again: $entity")

        EntityExistenceState.DESTROY_QUEUED -> {}
    }

    private fun destroyNow(entity: IGameEntity) {
        entities.remove(entity)

        systems.forEach { s -> s.remove(entity) }

        entity.components.forEach { it.value.reset() }
        entity.onDestroy()
        val respawnQueued = entitiesToSpawn.contains(entity) && !entitiesToKill.contains(entity)
        setExistenceState(
            entity,
            if (respawnQueued) EntityExistenceState.SPAWN_QUEUED else EntityExistenceState.UNSPAWNED
        )
    }

    private fun setExistenceState(entity: IGameEntity, state: EntityExistenceState) {
        val gameEntity = entity as? GameEntity
            ?: throw IllegalArgumentException("The engine can only hold a GameEntity: $entity")
        gameEntity.existenceState = state
    }

    private fun clearQueues() {
        entitiesToSpawn.forEachEntity { setExistenceState(it, EntityExistenceState.UNSPAWNED) }
        entitiesToSpawn.clear()
        entitiesToKill.clear()
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

                clearQueues()

                systems.forEach { it.reset() }

                false
            }
        }
    }

    override fun dispose() {
        entities.filter { it.spawned }.forEach { destroyNow(it) }
        entities.clear()

        clearQueues()

        systems.forEach { it.reset() }
        systems.forEach { it.dispose() }

        disposed = true
    }
}
