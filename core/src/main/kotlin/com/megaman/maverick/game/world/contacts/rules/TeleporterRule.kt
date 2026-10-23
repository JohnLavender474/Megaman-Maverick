package com.megaman.maverick.game.world.contacts.rules

import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.entities.contracts.ITeleporterEntity
import com.megaman.maverick.game.world.body.*

object TeleporterRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = teleport(a, b)

    override fun onContinue(a: IFixture, b: IFixture, delta: Float) = teleport(a, b)

    private fun teleport(teleporterListenerFixture: IFixture, teleporterFixture: IFixture) {
        val teleporterListener = teleporterListenerFixture.getEntity() as IBodyEntity

        val teleporterEntity = teleporterFixture.getEntity() as ITeleporterEntity

        if (teleporterEntity.shouldTeleport(teleporterListener)) teleporterEntity.teleport(teleporterListener)
    }
}
