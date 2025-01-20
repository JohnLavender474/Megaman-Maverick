package com.megaman.maverick.game.damage

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.ISizable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.damage.IDamager
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.hazards.Saw
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import kotlin.reflect.KClass

class StandardDamageNegotiator(val overrides: ObjectMap<KClass<out IDamager>, DamageNegotiation?> = ObjectMap()) :
    IDamageNegotiator {

    companion object {
        private val LARGE_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(3),
            Fireball::class pairTo dmgNeg(5),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 5 else 3
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 3 else 1
            },
            Saw::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
        )

        private val MEDIUM_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(5),
            Fireball::class pairTo dmgNeg(15),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 15 else 10
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 10 else 5
            },
            Saw::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
        )

        private val SMALL_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(15),
            Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 15 else 10
            },
            Saw::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
        )

        // damage is determined by the damageable's size instead of the damager's size
        fun get(size: Size, damager: IDamager): Int {
            val negotiations = when (size) {
                Size.LARGE -> LARGE_DMG_NEGS
                Size.MEDIUM -> MEDIUM_DMG_NEGS
                Size.SMALL -> SMALL_DMG_NEGS
            }
            return negotiations.get(damager::class)?.get(damager) ?: 0
        }
    }

    override fun get(damager: IDamager): Int {
        val key = damager::class
        return when {
            overrides.containsKey(key) -> overrides[key]?.get(damager) ?: 0
            damager is ISizable -> {
                val negotiations = when (damager.size) {
                    Size.LARGE -> LARGE_DMG_NEGS
                    Size.MEDIUM -> MEDIUM_DMG_NEGS
                    Size.SMALL -> SMALL_DMG_NEGS
                }
                negotiations.get(damager::class)?.get(damager) ?: 0
            }
            else -> 0
        }
    }
}
