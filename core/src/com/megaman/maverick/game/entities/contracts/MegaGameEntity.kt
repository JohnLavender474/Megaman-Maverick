package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.entities.GameEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.MegaGameEntitiesMap

abstract class MegaGameEntity(override val game: MegamanMaverickGame) : GameEntity(game.engine), IMegaGameEntity {

    val spawned: Boolean
        get() = state.spawned
    val dead: Boolean
        get() = !spawned
    val runnablesOnSpawn = OrderedMap<String, () -> Unit>()
    val runnablesOnDestroy = OrderedMap<String, () -> Unit>()

    override fun onSpawn(spawnProps: Properties) {
        MegaGameEntitiesMap.add(this)
        runnablesOnSpawn.values().forEach { it.invoke() }
    }

    override fun onDestroy() {
        MegaGameEntitiesMap.remove(this)
        runnablesOnDestroy.values().forEach { it.invoke() }
    }

    override fun getTag(): String = TAG
}