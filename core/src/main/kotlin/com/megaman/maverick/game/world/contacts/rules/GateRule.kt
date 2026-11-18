package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.entities.sensors.Gate
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.*

object GateRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = markTouched(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = markTouched(a, b)

    private fun markTouched(otherFixture: IFixture, gateFixture: IFixture) {
        val player = otherFixture.getEntity().asPlayer
        if (player == null) return
        (gateFixture.getEntity() as Gate).markTouched(player)
    }
}
