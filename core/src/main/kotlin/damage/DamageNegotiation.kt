package com.megaman.maverick.game.damage

import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.damage.IDamager
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import kotlin.reflect.KClass

class DamageNegotiation(var negotation: (IDamager) -> Int) {

    constructor(damage: Int) : this({ damage })

    fun get(damager: IDamager) = negotation(damager)
}

fun dmgNeg(negotation: (IDamager) -> Int) = DamageNegotiation(negotation)

fun dmgNeg(damage: Int) = DamageNegotiation(damage)

object EnemyDamageNegotiations {

    private val LARGE_ENEMY_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(3),
        Fireball::class pairTo dmgNeg(5),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 5 else 3
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 3 else 1
        }
    )

    private val MEDIUM_ENEMY_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(5),
        Fireball::class pairTo dmgNeg(15),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 5 else 3
        }
    )

    private val SMALL_ENEMY_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 25 else 15
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )

    fun getEnemyDmgNegs(
        enemySize: Size,
        vararg overrides: GamePair<KClass<out IDamager>, DamageNegotiation>
    ): ObjectMap<KClass<out IDamager>, DamageNegotiation> {
        val dmgNegs = ObjectMap(
            when (enemySize) {
                Size.LARGE -> LARGE_ENEMY_DMG_NEGS
                Size.MEDIUM -> MEDIUM_ENEMY_DMG_NEGS
                Size.SMALL -> SMALL_ENEMY_DMG_NEGS
            }
        )
        overrides.forEach { dmgNegs.put(it.first, it.second) }
        return dmgNegs
    }
}
