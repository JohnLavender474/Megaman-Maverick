package com.megaman.maverick.game.entities.contracts

import com.engine.common.objects.Properties
import com.engine.entities.GameEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.MegaGameEntitiesMap

abstract class MegaGameEntity(override val game: MegamanMaverickGame) : GameEntity(), IMegaGameEntity {

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        MegaGameEntitiesMap.add(getEntityType(), this)
    }

    override fun onDestroy() {
        super.onDestroy()
        MegaGameEntitiesMap.remove(getEntityType(), this)
    }
}