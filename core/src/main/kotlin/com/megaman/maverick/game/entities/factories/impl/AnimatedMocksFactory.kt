package com.megaman.maverick.game.entities.factories.impl

import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactory

class AnimatedMocksFactory(private val game: MegaServices) : EntityFactory() {

    override fun init(vararg params: Any) {
        // TODO
    }

    override fun fetch(key: Any?): MegaGameEntity? {
        // TODO
        return null
    }
}
