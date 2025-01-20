package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.interfaces.ISizable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.IEventListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame

abstract class AbstractProjectile(game: MegamanMaverickGame, override var size: Size = Size.MEDIUM) :
    MegaGameEntity(game), IProjectileEntity, ISpritesEntity, ISizable {

    override var owner: IGameEntity? = null

    open val canMove: Boolean
        get() = !game.isCameraRotating()

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
        when {
            cullOutOfBounds -> {
                val cullTime = spawnProps.getOrDefault(ConstKeys.CULL_TIME, PROJECTILE_DEFAULT_CULL_TIME, Float::class)
                putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getCullOnOutOfGameCam(cullTime))
            }

            else -> removeCullOnOutOfGameCam()
        }

        val doCullOnEvents = spawnProps.getOrDefault(ConstKeys.CULL_EVENTS, true, Boolean::class)
        when {
            doCullOnEvents -> {
                val cullOnEvents = getCullOnEventCullable()
                putCullable(ConstKeys.CULL_EVENTS, cullOnEvents)
                putProperty(ConstKeys.CULL_EVENTS, cullOnEvents)
                game.eventsMan.addListener(cullOnEvents)
                runnablesOnDestroy.put(ConstKeys.CULL_EVENTS) { game.eventsMan.removeListener(cullOnEvents) }
            }

            else -> {
                removeCullOnEventCullable()
                val cullOnEvents = getProperty(ConstKeys.CULL_EVENTS)
                if (cullOnEvents != null) game.eventsMan.removeListener(cullOnEvents as IEventListener)
                runnablesOnDestroy.remove(ConstKeys.CULL_EVENTS)
            }
        }
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        onDamageInflictedTo?.invoke(damageable)
    }
}
