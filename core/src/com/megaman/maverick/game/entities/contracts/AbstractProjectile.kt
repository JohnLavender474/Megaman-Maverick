package com.megaman.maverick.game.entities.contracts

import com.engine.common.objects.Properties
import com.engine.damage.IDamageable
import com.engine.entities.GameEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

abstract class AbstractProjectile(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity {

    protected var onDamageInflictedTo: ((IDamageable) -> Unit)? = null

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        onDamageInflictedTo = spawnProps.get(ConstKeys.ON_DAMAGE_INFLICTED_TO) as ((IDamageable) -> Unit)?
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        super.onDamageInflictedTo(damageable)
        onDamageInflictedTo?.invoke(damageable)
    }

}