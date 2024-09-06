package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.entities.GameEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.MegaGameEntitiesMap

abstract class MegaGameEntity(override val game: MegamanMaverickGame) : GameEntity(game.engine), IMegaGameEntity {

    val spawned: Boolean
        get() = state.spawned
    val dead: Boolean
        get() = !spawned
    val runnablesOnSpawn = Array<() -> Unit>()
    val runnablesOnDestroy = Array<() -> Unit>()

    override fun onSpawn(spawnProps: Properties) {
        MegaGameEntitiesMap.add(this)
        runnablesOnSpawn.forEach { it.invoke() }
    }

    override fun onDestroy() {
        MegaGameEntitiesMap.remove(this)
        runnablesOnDestroy.forEach { it.invoke() }
    }

    override fun getTag(): String = TAG
}