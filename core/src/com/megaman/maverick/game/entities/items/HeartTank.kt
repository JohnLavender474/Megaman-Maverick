package com.megaman.maverick.game.entities.items

import com.engine.entities.GameEntity
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.megaman.Megaman

// TODO
class HeartTank(game: MegamanMaverickGame) : GameEntity(game), ItemEntity {

    override fun contactWithPlayer(megaman: Megaman) {

    }
}
