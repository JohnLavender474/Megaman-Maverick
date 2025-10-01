package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.entities.GameEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.MegaGameEntities

abstract class MegaGameEntity(override val game: MegamanMaverickGame) : GameEntity(game.engine), IMegaGameEntity {

    companion object {
        const val TAG = "MegaGameEntity"
    }

    val runnablesOnSpawn = OrderedMap<String, () -> Unit>()
    val runnablesOnDestroy = OrderedMap<String, () -> Unit>()

    var dead = false
    var mapObjectId = 0
        private set

    override fun canSpawn(spawnProps: Properties): Boolean {
        try {
            if (!super.canSpawn(spawnProps)) return false

            val difficultyMode = game.state.getDifficultyMode()

            val hardModeOnly = spawnProps.getOrDefault(ConstKeys.HARD_MODE_ONLY, false, Boolean::class)
            if (hardModeOnly && difficultyMode != DifficultyMode.HARD) return false

            val normalModeOnly = spawnProps.getOrDefault(ConstKeys.NORMAL_MODE_ONLY, false, Boolean::class)
            return !(normalModeOnly && difficultyMode != DifficultyMode.NORMAL)
        } catch (e: Exception) {
            throw Exception("Exception while evaluating 'can spawn' for entity $this", e)
        }
    }

    override fun onSpawn(spawnProps: Properties) {
        try {
            mapObjectId = spawnProps.getOrDefault(ConstKeys.ID, -1, Int::class)
            runnablesOnSpawn.values().forEach { it.invoke() }
            MegaGameEntities.add(this)
            GameLogger.debug(TAG, "${getTag()}: onSpawn(): this=$this, spawnProps=$spawnProps")
        } catch (e: Exception) {
            throw Exception("Exception while spawning entity $this", e)
        }
    }

    override fun onDestroy() {
        try {
            MegaGameEntities.remove(this)
            runnablesOnDestroy.values().forEach { it.invoke() }
            GameLogger.debug(TAG, "${getTag()}: onDestroy(): this=$this")
        } catch (e: Exception) {
            throw Exception("Exception while destroying entity $this", e)
        }
    }

    override fun getTag() = this::class.simpleName ?: "?"

    override fun toString() = "{ ${this::class.simpleName}:[" +
        "mapObjId=$mapObjectId, dead=$dead, hashCode=${hashCode()}] }"
}
