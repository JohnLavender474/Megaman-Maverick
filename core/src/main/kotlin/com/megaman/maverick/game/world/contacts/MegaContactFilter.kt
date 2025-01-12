package com.megaman.maverick.game.world.contacts

import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.world.body.IFixture
import com.mega.game.engine.world.contacts.IContactFilter
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity

class MegaContactFilter : IContactFilter {

    private val filters = objectMapOf(
        FixtureType.CONSUMER pairTo objectSetOf(*FixtureType.entries.toTypedArray()),
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
        FixtureType.LASER pairTo objectSetOf(FixtureType.BLOCK),
        FixtureType.TELEPORTER pairTo objectSetOf(FixtureType.TELEPORTER_LISTENER)
    )

    override fun shouldProceedFiltering(fixture: IFixture) =
        filters.containsKey(fixture.getType() as FixtureType) || fixture.getType() == FixtureType.CUSTOM

    override fun filter(fixture1: IFixture, fixture2: IFixture): Boolean {
        if (fixture1 == fixture2 || fixture1.getEntity() == fixture2.getEntity()) return false

        if (fixture1.getType() == FixtureType.CUSTOM || fixture2.getType() == FixtureType.CUSTOM) {
            val custom: IFixture
            val other: IFixture
            when (FixtureType.CUSTOM) {
                fixture1.getType() -> {
                    custom = fixture1
                    other = fixture2
                }

                else -> {
                    custom = fixture2
                    other = fixture1
                }
            }

            val filter = custom.getProperty(ConstKeys.FILTER) as (IFixture) -> Boolean
            return filter.invoke(other)
        }

        return (filters.get(fixture1.getType() as FixtureType)?.contains(fixture2.getType()) == true ||
            filters.get(fixture2.getType() as FixtureType)?.contains(fixture1.getType()) == true)
    }
}
