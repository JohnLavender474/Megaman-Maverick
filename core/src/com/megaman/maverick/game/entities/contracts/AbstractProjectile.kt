package com.megaman.maverick.game.entities.contracts

import com.engine.common.objects.Properties
import com.engine.damage.IDamageable
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

abstract class AbstractProjectile(game: MegamanMaverickGame) : MegaGameEntity(game), IProjectileEntity, ISpritesEntity,
    IAudioEntity {

    companion object {
        const val DEFAULT_PROJECTILE_CULL_TIME = 0.5f
    }

    override var owner: IGameEntity? = null

    protected var onDamageInflictedTo: ((IDamageable) -> Unit)? = null
    protected var movementScalar = 1f

    override fun init() {
        super.init()
        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        onDamageInflictedTo = spawnProps.get(ConstKeys.ON_DAMAGE_INFLICTED_TO) as ((IDamageable) -> Unit)?
        movementScalar = spawnProps.getOrDefault("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", 1f, Float::class)

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullOutOfBounds) putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getCullOnOutOfGameCam())
        else removeCullOnOutOfGameCam()

        val cullOnEvents = spawnProps.getOrDefault(ConstKeys.CULL_EVENTS, true, Boolean::class)
        if (cullOnEvents) putCullable(ConstKeys.CULL_EVENTS, getCullOnEventCullable())
        else removeCullOnEventCullable()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        onDamageInflictedTo?.invoke(damageable)
    }
}