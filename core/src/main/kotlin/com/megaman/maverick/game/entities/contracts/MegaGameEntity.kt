package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.groups.GroupsComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.state.MapObjectComponent
import com.megaman.maverick.game.entities.state.mapObjectId

abstract class MegaGameEntity(val services: MegaServices) : GameEntity(services.engine), IMegaGameEntity {

    companion object {
        const val TAG = "MegaGameEntity"
    }

    val runnablesOnSpawn = OrderedMap<String, () -> Unit>()
    val runnablesOnDestroy = OrderedMap<String, () -> Unit>()
    val runnablesOnDestroyQueued = OrderedMap<String, () -> Unit>()
    private val runnablesToRun = Array<() -> Unit>()

    var timeSpawned = 0L
        private set

    override fun canSpawn(spawnProps: Properties): Boolean {
        if (!super.canSpawn(spawnProps)) return false

        val canSpawnInLevel = services.levelState.currentLevel!!.levelOverride?.canSpawn
        if (canSpawnInLevel != null && !canSpawnInLevel.invoke(this)) return false

        val difficultyMode = services.gameState.getDifficultyMode()

        val hardModeOnly = spawnProps.getOrDefault(ConstKeys.HARD_MODE_ONLY, false, Boolean::class)
        if (hardModeOnly && difficultyMode != DifficultyMode.HARD) return false

        val normalModeOnly = spawnProps.getOrDefault(ConstKeys.NORMAL_MODE_ONLY, false, Boolean::class)
        return !(normalModeOnly && difficultyMode != DifficultyMode.NORMAL)
    }

    override fun onSpawn(spawnProps: Properties) {
        when (val id = spawnProps.get(ConstKeys.ID, Int::class)) {
            null -> removeComponent(MapObjectComponent::class)
            else -> {
                val component = getComponent(MapObjectComponent::class)
                if (component != null) component.id = id else addComponent(MapObjectComponent(id))
            }
        }
        spawnProps.get(ConstKeys.GROUP, String::class)?.let { group ->
            val groups = getComponent(GroupsComponent::class) ?: GroupsComponent().also { addComponent(it) }
            groups.groups.add(group)
        }
        timeSpawned = System.currentTimeMillis()
        MegaGameEntities.add(this)
        runnablesOnSpawn.values().forEach { it.invoke() }
        GameLogger.debug(TAG, "${getTag()}: onSpawn(): this=$this")
    }

    override fun onDestroyQueued() {
        runnablesOnDestroyQueued.values().forEach { runnablesToRun.add(it) }
        runnablesToRun.forEach { it.invoke() }
        runnablesToRun.clear()
    }

    override fun onDestroy() {
        MegaGameEntities.remove(this)
        runnablesOnDestroy.values().forEach { it.invoke() }
        GameLogger.debug(TAG, "${getTag()}: onDestroy(): this=$this")
    }

    override fun getTag() = this::class.simpleName ?: "?"

    override fun toString() = "{ ${this::class.simpleName}:[" +
        "mapObjectId=$mapObjectId, existenceState=$existenceState, hashCode=${hashCode()}] }"
}
