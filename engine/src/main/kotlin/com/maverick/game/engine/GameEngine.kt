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

        internal fun copyTo(out: Array<GamePair<IGameEntity, Properties>>) = queue.forEach { out.add(it) }

        internal fun remove(entity: IGameEntity) {
            if (!set.remove(entity)) return
            val index = queue.indexOfFirst { it.first == entity }
            queue.removeIndex(index)
        }

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
    private val entitiesToRespawn = EntitiesToSpawn()
    private val destroysWaitingForSpawn = Array<IGameEntity>()
    private val respawnsNow = Array<GamePair<IGameEntity, Properties>>()

    private var reset = false
    private var disposed = false

    fun contains(entity: IGameEntity, containedIfQueuedToSpawn: Boolean = false) =
        entities.contains(entity) || (containedIfQueuedToSpawn && entitiesToSpawn.contains(entity))

    fun spawn(entity: IGameEntity, spawnProps: Properties = Properties()): Boolean {
        if (!entity.canSpawn(spawnProps)) {
            onSpawnRejected?.invoke(entity)
            return false
        }

        if (entitiesToSpawn.contains(entity) || entitiesToRespawn.contains(entity)) return false

        if (entitiesToKill.contains(entity)) {
            entitiesToRespawn.add(entity, spawnProps)
            setExistenceState(entity, EntityExistenceState.RESPAWN_QUEUED)
            GameLogger.debug(TAG, "spawn(): a destroy is queued, so the entity respawns after it: $entity")
            entity.onSpawnQueued()
            return true
        }

        return when (entity.existenceState) {
            EntityExistenceState.LIVE, EntityExistenceState.SPAWN_QUEUED, EntityExistenceState.RESPAWN_QUEUED -> {
                GameLogger.debug(TAG, "spawn(): refused, the entity is live or spawning: $entity")
                false
            }

            EntityExistenceState.UNSPAWNED -> {
                entitiesToSpawn.add(entity, spawnProps)
                setExistenceState(entity, EntityExistenceState.SPAWN_QUEUED)
                entity.onSpawnQueued()
                true
            }

            EntityExistenceState.DESTROY_QUEUED -> {
                val queued = entitiesToSpawn.add(entity, spawnProps)
                if (queued) entity.onSpawnQueued()
                queued
            }
        }
    }

    private fun spawnNow(entity: IGameEntity, spawnProps: Properties) {
        entities.add(entity)

        if (!entity.initialized) initialize(entity)
        entity.onSpawn(spawnProps)
        setExistenceState(
            entity,
            when {
                entitiesToRespawn.contains(entity) -> EntityExistenceState.RESPAWN_QUEUED
                entitiesToKill.contains(entity) -> EntityExistenceState.DESTROY_QUEUED
                else -> EntityExistenceState.LIVE
            }
        )

        updateSystemMembershipsFor(entity)
    }

    private fun initialize(entity: IGameEntity) {
        entity.init()
        entity.initialized = true
    }

    fun destroy(entity: IGameEntity): Boolean {
        if (entitiesToRespawn.contains(entity)) {
            entitiesToRespawn.remove(entity)
            setExistenceState(
                entity,
                if (entitiesToKill.contains(entity)) EntityExistenceState.DESTROY_QUEUED
                else EntityExistenceState.UNSPAWNED
            )
            GameLogger.debug(TAG, "destroy(): cancels the queued respawn: $entity")
            entity.onDestroyQueued()
            return true
        }

        if (entitiesToKill.contains(entity)) return false

        return when (entity.existenceState) {
            EntityExistenceState.LIVE -> {
                entitiesToKill.add(entity)
                setExistenceState(entity, EntityExistenceState.DESTROY_QUEUED)
                entity.onDestroyQueued()
                true
            }

            EntityExistenceState.SPAWN_QUEUED -> {
                entitiesToKill.add(entity)
                setExistenceState(entity, EntityExistenceState.UNSPAWNED)
                entity.onDestroyQueued()
                true
            }

            EntityExistenceState.RESPAWN_QUEUED -> {
                entitiesToKill.add(entity)
                entity.onDestroyQueued()
                true
            }

            EntityExistenceState.UNSPAWNED ->
                if (entitiesToSpawn.contains(entity)) entitiesToKill.add(entity)
                else {
                    GameLogger.debug(TAG, "destroy(): refused, the entity is not spawned: $entity")
                    false
                }

            EntityExistenceState.DESTROY_QUEUED -> {
                GameLogger.debug(TAG, "destroy(): refused, the entity is being destroyed: $entity")
                false
            }
        }
    }

    private fun destroyNow(entity: IGameEntity) {
        entities.remove(entity)

        systems.forEach { s -> s.remove(entity) }

        entity.components.forEach { it.value.reset() }
        entity.onDestroy()
        setExistenceState(
            entity,
            when {
                entitiesToRespawn.contains(entity) -> EntityExistenceState.RESPAWN_QUEUED
                entitiesToSpawn.contains(entity) && !entitiesToKill.contains(entity) ->
                    EntityExistenceState.SPAWN_QUEUED

                else -> EntityExistenceState.UNSPAWNED
            }
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
        entitiesToRespawn.forEachEntity { setExistenceState(it, EntityExistenceState.UNSPAWNED) }
        entitiesToRespawn.clear()
        entitiesToKill.forEach { setExistenceState(it, EntityExistenceState.UNSPAWNED) }
        entitiesToKill.clear()
        destroysWaitingForSpawn.clear()
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
            if (entitiesToSpawn.contains(entity)) destroysWaitingForSpawn.add(entity) else destroyNow(entity)
        }
        destroysWaitingForSpawn.forEach { entitiesToKill.add(it) }
        destroysWaitingForSpawn.clear()

        entitiesToRespawn.copyTo(respawnsNow)
        respawnsNow.forEach { (entity, spawnProps) ->
            if (entitiesToRespawn.contains(entity)) {
                entitiesToRespawn.remove(entity)
                spawnNow(entity, spawnProps)
            }
        }
        respawnsNow.clear()

        systems.forEach { it.update(delta) }

        updating = false

        if (reset) reset()
    }

    override fun reset() {
        reset = when {
            updating -> true
            else -> {
                entities.filter { it.existenceState.liveNow }.forEach { destroyNow(it) }
                entities.clear()

                clearQueues()

                systems.forEach { it.reset() }

                false
            }
        }
    }

    override fun dispose() {
        entities.filter { it.existenceState.liveNow }.forEach { destroyNow(it) }
        entities.clear()

        clearQueues()

        systems.forEach { it.reset() }
        systems.forEach { it.dispose() }

        disposed = true
    }
}
