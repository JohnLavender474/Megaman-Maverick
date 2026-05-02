package com.megaman.maverick.game.world.contacts

import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.world.body.IFixture
import com.mega.game.engine.world.contacts.IContactFilter
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.sensors.DeathForPlayerOnly
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.getFilter
import com.megaman.maverick.game.world.body.hasFilter

class MegaContactFilter : IContactFilter {

    private val filters = objectMapOf(
        FixtureType.PLAYER pairTo objectSetOf(
            FixtureType.BODY,
            FixtureType.ITEM
        ),
        FixtureType.DAMAGEABLE pairTo objectSetOf(
            FixtureType.DAMAGER
        ),
        FixtureType.BODY pairTo objectSetOf(
            FixtureType.BODY,
            FixtureType.FEET,
            FixtureType.SIDE,
            FixtureType.BLOCK,
            FixtureType.FORCE,
            FixtureType.BOUNCER,
            FixtureType.GRAVITY_CHANGE
        ),
        FixtureType.EXPLOSION pairTo objectSetOf(
            FixtureType.BLOCK,
            FixtureType.BODY
        ),
        FixtureType.DEATH pairTo objectSetOf(
            FixtureType.FEET,
            FixtureType.SIDE,
            FixtureType.HEAD,
            FixtureType.BODY,
            FixtureType.PLAYER
        ),
        FixtureType.WATER pairTo objectSetOf(
            FixtureType.WATER_LISTENER
        ),
        FixtureType.LADDER pairTo objectSetOf(
            FixtureType.HEAD,
            FixtureType.FEET
        ),
        FixtureType.SIDE pairTo objectSetOf(
            FixtureType.SIDE,
            FixtureType.ICE,
            FixtureType.GATE,
            FixtureType.BLOCK,
            FixtureType.BOUNCER
        ),
        FixtureType.FEET pairTo objectSetOf(
            FixtureType.ICE,
            FixtureType.BLOCK,
            FixtureType.BOUNCER,
            FixtureType.SAND,
            FixtureType.SNOW,
            FixtureType.CART,
            FixtureType.GATE
        ),
        FixtureType.HEAD pairTo objectSetOf(
            FixtureType.BLOCK,
            FixtureType.BOUNCER,
            FixtureType.FEET,
            FixtureType.GATE
        ),
        FixtureType.PROJECTILE pairTo objectSetOf(
            FixtureType.BODY,
            FixtureType.BLOCK,
            FixtureType.WATER,
            FixtureType.SHIELD,
            FixtureType.SAND,
            FixtureType.PROJECTILE,
            FixtureType.EXPLOSION
        ),
        FixtureType.LASER pairTo objectSetOf(
            FixtureType.BLOCK,
            FixtureType.BODY,
            FixtureType.SHIELD
        ),
        FixtureType.TELEPORTER pairTo objectSetOf(
            FixtureType.TELEPORTER_LISTENER
        )
    )

    override fun shouldProceedFiltering(fixture: IFixture) =
        filters.containsKey(fixture.getType() as FixtureType) || fixture.getType() == FixtureType.CONSUMER

    override fun filter(fixture1: IFixture, fixture2: IFixture): Boolean {
        if (fixture1 == fixture2) return false

        val entity1 = fixture1.getEntity()
        val entity2 = fixture2.getEntity()

        if (entity1 == entity2) return false
        if (entity1.getType() == EntityType.ENEMY && entity2.getType() == EntityType.ENEMY) return false

        val type1 = fixture1.getType()
        val type2 = fixture2.getType()

        if ((type1 == FixtureType.GATE || type2 == FixtureType.GATE) &&
            entity1.getType() != EntityType.MEGAMAN && entity2.getType() != EntityType.MEGAMAN
        ) return false

        if ((type1 == FixtureType.CART || type2 == FixtureType.CART) &&
            entity1.getType() != EntityType.MEGAMAN && entity2.getType() != EntityType.MEGAMAN
        ) return false

        if ((type1 == FixtureType.DEATH && entity2.getType() == EntityType.PROJECTILE) ||
            (type2 == FixtureType.DEATH && entity1.getType() == EntityType.PROJECTILE)
        ) return false

        var deathEntity: MegaGameEntity? = null
        when {
            type1 == FixtureType.DEATH -> deathEntity = entity1
            type2 == FixtureType.DEATH -> deathEntity = entity2
        }
        if (deathEntity != null &&
            entity1.getType() != EntityType.MEGAMAN &&
            entity2.getType() != EntityType.MEGAMAN &&
            deathEntity.getTag() == DeathForPlayerOnly.TAG
        ) return false

        if (type1 == FixtureType.CONSUMER || type2 == FixtureType.CONSUMER) {
            val consumer: IFixture
            val other: IFixture

            if (type1 == FixtureType.CONSUMER) {
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

        return (filters.get(type1 as FixtureType)?.contains(type2) == true ||
            filters.get(type2 as FixtureType)?.contains(type1) == true)
    }
}
