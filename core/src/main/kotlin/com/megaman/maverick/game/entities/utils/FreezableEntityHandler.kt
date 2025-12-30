package com.megaman.maverick.game.entities.utils

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.blocks.FrozenEntityBlock
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class FreezableEntityHandler(
    private val entity: IFreezableEntity,
    private val onFrozen: () -> Unit = {},
    private val onUnfrozen: () -> Unit = {},
    private val onJustFinished: () -> Unit = {},
    private val doSpawnFrozenBlock: () -> Boolean = { true },
    private val frozenBlockBoundsSupplier: (() -> GameRectangle)? = {
        (entity as IBodyEntity).body.getBounds()
    },
    duration: Float = ConstVals.STANDARD_FROZEN_DUR
) : Updatable {

    companion object {
        const val TAG = "FreezableEntityHandler"
    }

    init {
        if (entity is AbstractHealthEntity) {
            entity.invinciblePredicates.add { isFrozen() }

            entity.onDamagedCallbacks.add { damager, _ ->
                if (!entity.frozen && damager is IFreezerEntity) entity.frozen = true
                if (entity.frozen && damager is IFireEntity) entity.frozen = false
            }
        }

        if (entity is AbstractEnemy) entity.canDamagePredicates.add { !entity.frozen }
    }

    private val frozenEntityBlock = FrozenEntityBlock((entity as MegaGameEntity).game)

    val timer = Timer(duration).setToEnd()

    fun setFrozen(value: Boolean) {
        GameLogger.debug(TAG, "setFrozen(): value=$value")
        if (value) {
            if (frozenEntityBlock.dead && doSpawnFrozenBlock.invoke())
                frozenEntityBlock.spawn(props(ConstKeys.ENTITY pairTo entity))
            timer.reset()
            onFrozen.invoke()
        } else {
            if (frozenEntityBlock.spawned) frozenEntityBlock.destroy()
            timer.setToEnd()
            onUnfrozen.invoke()
        }
    }

    fun isFrozen() = !timer.isFinished()

    fun isJustFinished() = timer.isJustFinished()

    fun isFinished() = timer.isFinished()

    override fun update(delta: Float) {
        timer.update(delta)
        if (timer.isJustFinished()) {
            onJustFinished.invoke()
            IceShard.spawn5((entity as IBodyEntity).body.getCenter())
        }

        if (isFrozen() && frozenEntityBlock.spawned) {
            val bounds = frozenBlockBoundsSupplier?.invoke()
            if (bounds != null) {
                frozenEntityBlock.body.set(bounds)
                frozenEntityBlock.body.forEachFixture { fixture ->
                    fixture.setShape(bounds)
                }
            }
        }

        if ((entity as MegaGameEntity).dead || !entity.frozen) setFrozen(false)
    }
}
