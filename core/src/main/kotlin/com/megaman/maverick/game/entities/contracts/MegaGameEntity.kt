package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Pool
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.entities.GameEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaGameEntities

abstract class MegaGameEntity(override val game: MegamanMaverickGame) : GameEntity(game.engine), IMegaGameEntity {

    companion object {
        const val TAG = "MegaGameEntity"

        // If an entity is not assigned an ID via spawn props, then it will be assigned
        // a negative id.
        private var CURRENT_NEW_ID = -1
        private val RANDOM_ID_POOL = Pool<Int>(
            supplier = { CURRENT_NEW_ID },
            onSupplyNew = { --CURRENT_NEW_ID }
        )
    }

    val runnablesOnSpawn = OrderedMap<String, () -> Unit>()
    val runnablesOnDestroy = OrderedMap<String, () -> Unit>()

    var dead = false
    var id = 0
        private set
    var timeSpawned = 0L
        private set

    override fun canSpawn(spawnProps: Properties): Boolean {
        if (!super.canSpawn(spawnProps)) return false

        val difficultyMode = game.state.getDifficultyMode()

        val hardModeOnly = spawnProps.getOrDefault(ConstKeys.HARD_MODE_ONLY, false, Boolean::class)
        if (hardModeOnly && difficultyMode != DifficultyMode.HARD) return false

        val normalModeOnly = spawnProps.getOrDefault(ConstKeys.NORMAL_MODE_ONLY, false, Boolean::class)
        return !(normalModeOnly && difficultyMode != DifficultyMode.NORMAL)
    }

    override fun onSpawn(spawnProps: Properties) {
        id = spawnProps.getOrDefault(ConstKeys.ID, RANDOM_ID_POOL.fetch(), Int::class)
        timeSpawned = System.currentTimeMillis()
        MegaGameEntities.add(this)
        runnablesOnSpawn.values().forEach { it.invoke() }
        GameLogger.debug(TAG, "${getTag()}: onSpawn(): this=$this")
    }

    override fun onDestroy() {
        MegaGameEntities.remove(this)
        runnablesOnDestroy.values().forEach { it.invoke() }
        if (id < 0) RANDOM_ID_POOL.free(id)
        GameLogger.debug(TAG, "${getTag()}: onDestroy(): this=$this")
    }

    override fun getTag() = this::class.simpleName ?: "?"

    override fun toString() = "{ ${this::class.simpleName}:[" +
        "id=$id, dead=$dead, hashCode=${hashCode()}] }"
}
