package com.megaman.maverick.game.entities.utils

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.blocks.ShieldEntity
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class FreezableEntityHandler<T>(
    private val entity: T,
    private val onFrozen: () -> Unit = {},
    private val canBeFrozen: () -> Boolean = { true },
    private val onJustFinished: () -> Unit = {},
    private val doSpawnFrozenShield: () -> Boolean = { true },
    private val frozenShieldBoundsSupplier: (() -> GameRectangle)? = {
        (entity as IBodyEntity).body.getBounds()
    },
    duration: Float = ConstVals.STANDARD_FROZEN_DUR,
    private val forceSound: Boolean = false,
    noDamageOnFrozen: Boolean = true
) : Updatable where T : MegaGameEntity, T : IFreezableEntity {

    companion object {
        const val TAG = "FreezableEntityHandler"
    }

    init {
        if (entity is AbstractHealthEntity) {
            entity.invinciblePredicates.add { isFrozen() }

            entity.onHealthDepletedCallbacks.add {
                if (isFrozen()) IceShard.spawn5((entity as IBodyEntity).body.getCenter())
            }

            entity.onDamagedCallbacks.add { damager, _ ->
                if (!entity.frozen && canBeFrozen.invoke() && damager is IFreezerEntity) entity.frozen = true
                if (entity.frozen && damager is IFireEntity) entity.frozen = false
            }
        }

        if (noDamageOnFrozen && entity is AbstractEnemy) entity.canDamagePredicates.add { !entity.frozen }
    }

    private val shieldEntity = ShieldEntity(entity.services)

    val timer = Timer(duration).setToEnd()

    fun setFrozen(value: Boolean) {
        if (value) {
            if (!shieldEntity.existenceState.liveAfterUpdate && doSpawnFrozenShield.invoke())
                shieldEntity.spawn(props(ConstKeys.OWNER pairTo entity))
            timer.reset()
            onFrozen.invoke()
        } else {
            if (shieldEntity.existenceState.liveNow) shieldEntity.destroy()
            timer.setToEnd(false)
        }
    }

    fun isFrozen() = !timer.isFinished()

    fun isJustFinished() = timer.isJustFinished()

    fun isFinished() = timer.isFinished()

    override fun update(delta: Float) {
        timer.update(delta)
        if (timer.isJustFinished()) {
            GameLogger.debug(TAG, "update(): timer just finished")
            onJustFinished.invoke()
            IceShard.spawn5((entity as IBodyEntity).body.getCenter(), forceSound = forceSound)
        }

        if (isFrozen() && shieldEntity.existenceState.liveNow) {
            val bounds = frozenShieldBoundsSupplier?.invoke()
            if (bounds != null) {
                shieldEntity.body.set(bounds)
                shieldEntity.body.forEachFixture { fixture ->
                    fixture.setShape(bounds)
                }
            }
        }

        if (!entity.existenceState.liveAfterUpdate || !entity.frozen) setFrozen(false)
    }
}
