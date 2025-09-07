package com.megaman.maverick.game.damage

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.ISizable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.damage.IDamager
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.hazards.DrippingToxicGoop
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.Saw
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.*
import kotlin.math.ceil
import kotlin.reflect.KClass

class StandardDamageNegotiator(val overrides: ObjectMap<KClass<out IDamager>, DamageNegotiation?> = ObjectMap()) :
    IDamageNegotiator {

    companion object {
        private val LARGE_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(3),
            Fireball::class pairTo dmgNeg(5),
            MagmaFlame::class pairTo dmgNeg(5),
            MagmaWave::class pairTo dmgNeg(5),
            MagmaExplosion::class pairTo dmgNeg(5),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 5 else 3
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 3 else 1
            },
            Saw::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            MoonScythe::class pairTo dmgNeg(5),
            SmallIceCube::class pairTo dmgNeg(3),
            DrippingToxicGoop::class pairTo dmgNeg(1),
            Asteroid::class pairTo dmgNeg(10),
            PreciousGem::class pairTo dmgNeg(10),
            LampeonBullet::class pairTo dmgNeg(5),
            Explosion::class pairTo dmgNeg(5),
            Axe::class pairTo dmgNeg(10),
            Needle::class pairTo dmgNeg(10),
            Megaman::class pairTo dmgNeg(15),
            SlashWave::class pairTo dmgNeg damage@{
                it as SlashWave
                return@damage ceil(0.5f * it.getDissipation()).toInt()
            }
        )

        private val MEDIUM_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(5),
            Fireball::class pairTo dmgNeg(15),
            MagmaFlame::class pairTo dmgNeg(15),
            MagmaWave::class pairTo dmgNeg(15),
            MagmaExplosion::class pairTo dmgNeg(15),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 15 else 10
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 10 else 5
            },
            Saw::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            MoonScythe::class pairTo dmgNeg(15),
            SmallIceCube::class pairTo dmgNeg(5),
            DrippingToxicGoop::class pairTo dmgNeg(1),
            Asteroid::class pairTo dmgNeg(20),
            PreciousGem::class pairTo dmgNeg(20),
            LampeonBullet::class pairTo dmgNeg(15),
            Explosion::class pairTo dmgNeg(15),
            Axe::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Needle::class pairTo dmgNeg(20),
            Megaman::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            SlashWave::class pairTo dmgNeg damage@{
                it as SlashWave
                return@damage it.getDissipation()
            }
        )

        private val SMALL_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(15),
            Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            MagmaFlame::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            MagmaWave::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            MagmaExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 15 else 10
            },
            Saw::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            MoonScythe::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            SmallIceCube::class pairTo dmgNeg(15),
            DrippingToxicGoop::class pairTo dmgNeg(1),
            Asteroid::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            PreciousGem::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            LampeonBullet::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Explosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Axe::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Needle::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Megaman::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            SlashWave::class pairTo dmgNeg damage@{
                it as SlashWave
                return@damage 3 * it.getDissipation()
            }
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
