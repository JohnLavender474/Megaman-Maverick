package com.megaman.maverick.game.world.contacts

import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.world.body.IFixture
import com.mega.game.engine.world.contacts.IContactFilter
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.getFilter
import com.megaman.maverick.game.world.body.hasFilter

class MegaContactFilter : IContactFilter {

    private val filters = objectMapOf(
        FixtureType.PLAYER pairTo objectSetOf(FixtureType.BODY, FixtureType.ITEM),
        FixtureType.DAMAGEABLE pairTo objectSetOf(FixtureType.DAMAGER),
        FixtureType.BODY pairTo objectSetOf(
            FixtureType.BODY,
            FixtureType.FEET,
            FixtureType.BLOCK,
            FixtureType.FORCE,
            FixtureType.EXPLOSION,
            FixtureType.GRAVITY_CHANGE
        ),
        FixtureType.DEATH pairTo objectSetOf(
            FixtureType.FEET, FixtureType.SIDE, FixtureType.HEAD, FixtureType.BODY
        ),
        FixtureType.WATER pairTo objectSetOf(FixtureType.WATER_LISTENER),
        FixtureType.LADDER pairTo objectSetOf(FixtureType.HEAD, FixtureType.FEET),
        FixtureType.SIDE pairTo objectSetOf(
            FixtureType.ICE, FixtureType.GATE, FixtureType.BLOCK, FixtureType.BOUNCER
        ),
        FixtureType.FEET pairTo objectSetOf(
            FixtureType.ICE,
            FixtureType.BLOCK,
            FixtureType.BOUNCER,
            FixtureType.SAND,
            FixtureType.SNOW,
            FixtureType.CART
        ),
        FixtureType.HEAD pairTo objectSetOf(FixtureType.BLOCK, FixtureType.BOUNCER),
        FixtureType.PROJECTILE pairTo objectSetOf(
            FixtureType.BODY,
            FixtureType.BLOCK,
            FixtureType.WATER,
            FixtureType.SHIELD,
            FixtureType.SAND,
            FixtureType.PROJECTILE
        ),
        FixtureType.LASER pairTo objectSetOf(FixtureType.BLOCK, FixtureType.BODY),
        FixtureType.TELEPORTER pairTo objectSetOf(FixtureType.TELEPORTER_LISTENER)
    )

    override fun shouldProceedFiltering(fixture: IFixture) =
        filters.containsKey(fixture.getType() as FixtureType) || fixture.getType() == FixtureType.CONSUMER

    override fun filter(fixture1: IFixture, fixture2: IFixture): Boolean {
        if (fixture1 == fixture2 || fixture1.getEntity() == fixture2.getEntity()) return false

        if (fixture1.getType() == FixtureType.CONSUMER || fixture2.getType() == FixtureType.CONSUMER) {
            val consumer: IFixture
            val other: IFixture

            if (fixture1.getType() == FixtureType.CONSUMER) {
                consumer = fixture1
                other = fixture2
            } else {
                consumer = fixture2
                other = fixture1
            }

            try {
                return consumer.getFilter().invoke(other)
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Failed to perform custom filter on fixtures: fixture1=$fixture1, fixture2=$fixture2", e
                )
            }
        }

        if ((fixture1.hasFilter() && !fixture1.getFilter().invoke(fixture2)) ||
            (fixture2.hasFilter() && !fixture2.getFilter().invoke(fixture1))
        ) return false

        return (filters.get(fixture1.getType() as FixtureType)?.contains(fixture2.getType()) == true ||
            filters.get(fixture2.getType() as FixtureType)?.contains(fixture1.getType()) == true)
    }
}
