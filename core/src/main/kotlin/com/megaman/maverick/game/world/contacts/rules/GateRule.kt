package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.entities.contracts.IGateTrigger
import com.megaman.maverick.game.entities.sensors.Gate
import com.megaman.maverick.game.world.body.*

object GateRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    private fun begin(otherFixture: IFixture, gateFixture: IFixture) {
        val entity = otherFixture.getEntity()
        if (entity is IGateTrigger) {
            val gate = gateFixture.getEntity() as Gate
            if (gate.isTriggerable()) gate.trigger()
        }
    }
}
