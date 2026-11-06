package com.megaman.maverick.game.entities.utils

import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.interfaces.ISizable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.SelfSizeDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import kotlin.math.ceil
import kotlin.reflect.KClass

object X6Damage {

    const val BUSTER = 3
    const val HALF_CHARGE = 6
    const val FULL_CHARGE = 12

    fun scaled(x6Damage: Int, x6Health: Int) =
        ceil(x6Damage * ConstVals.MAX_HEALTH.toFloat() / x6Health).toInt().coerceAtMost(ConstVals.MAX_HEALTH)

    fun negotiator(sizable: ISizable, x6Health: Int) = SelfSizeDamageNegotiator(
        sizable,
        objectMapOf(
            Bullet::class pairTo dmgNeg(scaled(BUSTER, x6Health)),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                scaled(if (it.fullyCharged) FULL_CHARGE else HALF_CHARGE, x6Health)
            }
        )
    )
}
