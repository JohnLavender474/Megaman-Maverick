package com.megaman.maverick.game.entities.explosions

import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class TorikoPlundge(game: MegamanMaverickGame): MegaGameEntity(game) {

    override fun getType() = EntityType.HAZARD
}
