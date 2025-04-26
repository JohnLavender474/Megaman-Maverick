package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.entities.GameEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
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

    override fun onSpawn(spawnProps: Properties) {
        mapObjectId = spawnProps.getOrDefault(ConstKeys.ID, 0, Int::class)
        runnablesOnSpawn.values().forEach { it.invoke() }
        MegaGameEntities.add(this)
        GameLogger.debug(TAG, "${getTag()}: onSpawn(): this=$this, spawnProps=$spawnProps")
    }

    override fun onDestroy() {
        runnablesOnDestroy.values().forEach { it.invoke() }
        MegaGameEntities.remove(this)
        GameLogger.debug(TAG, "${getTag()}: onDestroy(): this=$this")
    }

    override fun getTag() = this::class.simpleName ?: "?"

    override fun toString() = "{ ${this::class.simpleName}:[" +
        "mapObjId=$mapObjectId, dead=$dead, hashCode=${hashCode()}] }"
}
