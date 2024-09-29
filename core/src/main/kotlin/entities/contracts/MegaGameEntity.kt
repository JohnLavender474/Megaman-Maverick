package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.entities.GameEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.MegaGameEntitiesMap

abstract class MegaGameEntity(override val game: MegamanMaverickGame) : GameEntity(game.engine), IMegaGameEntity {

    companion object {
        const val TAG = "MegaGameEntity"
    }

    val runnablesOnSpawn = OrderedMap<String, () -> Unit>()
    val runnablesOnDestroy = OrderedMap<String, () -> Unit>()
    var dead = false

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): ${this::class.simpleName}, spawnProps=$spawnProps")
        MegaGameEntitiesMap.add(this)
        runnablesOnSpawn.values().forEach { it.invoke() }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "${this::class.simpleName}, onDestroy()")
        MegaGameEntitiesMap.remove(this)
        runnablesOnDestroy.values().forEach { it.invoke() }
    }

    override fun getTag(): String = TAG

    override fun toString() = "${this::class.simpleName}: dead=$dead"
}