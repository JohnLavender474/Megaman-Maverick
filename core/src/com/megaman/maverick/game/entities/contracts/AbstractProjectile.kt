package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.world.body.*;
import com.mega.game.engine.world.collisions.*;
import com.mega.game.engine.world.contacts.*;
import com.mega.game.engine.world.pathfinding.*;

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

abstract class AbstractProjectile(game: MegamanMaverickGame) : MegaGameEntity(game), IProjectileEntity, ISpritesEntity {

    override var owner: GameEntity? = null

    protected var onDamageInflictedTo: ((IDamageable) -> Unit)? = null
    protected var movementScalar = 1f

    override fun init() {
        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
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