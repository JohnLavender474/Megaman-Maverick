package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.*

object PlayerItemRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    private fun begin(playerFixture: IFixture, itemFixture: IFixture) {
        val player = playerFixture.getEntity().asPlayer
        val itemEntity = itemFixture.getEntity()

        if (player != null && itemEntity is ItemEntity) itemEntity.contactWithPlayer(player)
    }
}
