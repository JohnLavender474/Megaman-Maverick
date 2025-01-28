package com.megaman.maverick.game.entities.megaman

import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.ISizable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.damage.IDamager
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.PropellerPlatform
import com.megaman.maverick.game.entities.blocks.RocketPlatform
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.hazards.DeadlyLeaf
import com.megaman.maverick.game.entities.hazards.LaserBeamer

class MegamanDamageNegotiator(private val megaman: Megaman): IDamageNegotiator {

    companion object {
        private val custom = objectMapOf<String, DamageNegotiation>(
            PropellerPlatform.TAG pairTo dmgNeg(2),
            RocketPlatform.TAG pairTo dmgNeg(2),
            LaserBeamer.TAG pairTo dmgNeg(3),
            DeadlyLeaf.TAG pairTo dmgNeg(2)
        )

        private val entityTypes = objectSetOf(EntityType.ENEMY, EntityType.EXPLOSION, EntityType.HAZARD)
    }

    override fun get(damager: IDamager): Int {
        val entity = damager as MegaGameEntity

        if (entity is IOwnable && entity.owner == megaman) return 0

        val tag = entity.getTag()

        return when {
            custom.containsKey(tag) -> custom[tag].get(damager)

            entity is ISizable -> when (entity.size) {
                Size.LARGE -> 4
                Size.MEDIUM -> 3
                Size.SMALL -> 2
            }

            entityTypes.contains(entity.getType()) -> 3

            else -> 0
        }
    }
}
