package com.megaman.maverick.game.entities.items

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class HealthTank(game: MegamanMaverickGame) : MegaGameEntity(game) {

    override fun init() {
        TODO("Not yet implemented")
    }

    override fun getType() = EntityType.ITEM
}
