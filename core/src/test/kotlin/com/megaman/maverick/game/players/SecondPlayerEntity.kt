package com.megaman.maverick.game.players

import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.testServices

class SecondPlayerEntity(services: MegaServices) : MegaGameEntity(services), IBodyEntity {

    init {
        addComponent(BodyComponent(Body(BodyType.DYNAMIC)))
    }

    override fun getType() = EntityType.SPECIAL
}

fun registeredPlayer(): SecondPlayerEntity {
    val players = MegaPlayers()
    val player = SecondPlayerEntity(testServices(players = players))
    players.register(PlayerSlot(0), player)
    return player
}

fun nonPlayer() = SecondPlayerEntity(testServices())
