package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.getRandom
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ItemsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.setStandardOnTeleportEndProp
import com.megaman.maverick.game.entities.utils.setStandardOnTeleportStartProp
import com.megaman.maverick.game.events.EventType

abstract class AbstractEnemy(
    game: MegamanMaverickGame,
    dmgDuration: Float = DEFAULT_DMG_DURATION,
    dmgBlinkBlur: Float = DEFAULT_DMG_BLINK_DUR
) : AbstractHealthEntity(game, dmgDuration, dmgBlinkBlur), IDamager, IBodyEntity, IAudioEntity, ISpritesEntity,
    ICullableEntity {

    companion object {
        const val TAG = "AbstractEnemy"
        const val DEFAULT_CULL_TIME = 1f
    }

    protected var movementScalar = 1f
    protected var dropItemOnDeath = true
    protected var onDamageInflictedTo: ((IDamageable) -> Unit)? = null

    override fun getEntityType() = EntityType.ENEMY

    override fun init() {
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent())
        addComponent(CullablesComponent())
        runnablesOnDestroy.put(ConstKeys.ITEMS) {
            if (hasDepletedHealth()) {
                disintegrate()
                if (dropItemOnDeath) {
                    val randomInt = getRandom(0, 10)
                    val props = props(ConstKeys.POSITION to body.getCenter())
                    val entity: GameEntity? = when (randomInt) {
                        0, 1, 2 -> {
                            props.put(ConstKeys.LARGE, randomInt == 1)
                            EntityFactories.fetch(EntityType.ITEM, ItemsFactory.HEALTH_BULB)
                        }

                        3, 4, 5 -> {
                            // TODO: EntityFactories.fetch(EntityType.ITEM, ItemsFactory.WEAPON_ENERGY)
                            null
                        }

                        else -> null
                    }
                    entity?.spawn(props)
                }
            }
        }
        setStandardOnTeleportStartProp(this)
        setStandardOnTeleportEndProp(this)
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        onDamageInflictedTo = spawnProps.get(ConstKeys.ON_DAMAGE_INFLICTED_TO) as ((IDamageable) -> Unit)?
        dropItemOnDeath = spawnProps.getOrDefault(ConstKeys.DROP_ITEM_ON_DEATH, true, Boolean::class)
        movementScalar = spawnProps.getOrDefault("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", 1f, Float::class)

        val cullWhenOutOfCamBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullWhenOutOfCamBounds) {
            val cullTime = spawnProps.getOrDefault(ConstKeys.CULL_TIME, DEFAULT_CULL_TIME, Float::class)
            val cullOnOutOfBounds = getGameCameraCullingLogic(this, cullTime)
            putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, cullOnOutOfBounds)
        } else removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)

        val cullEvents = spawnProps.getOrDefault(ConstKeys.CULL_EVENTS, true, Boolean::class)
        if (cullEvents) {
            val eventsToCullOn = objectSetOf<Any>(
                EventType.GAME_OVER, EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS, EventType.GATE_INIT_OPENING
            )
            val cullOnEvents = CullableOnEvent({ eventsToCullOn.contains(it.key) }, eventsToCullOn)

            game.eventsMan.addListener(cullOnEvents)
            GameLogger.debug(TAG, "Added CullableOnEvent from EventsManager")

            runnablesOnDestroy.put(ConstKeys.CULL_EVENTS) {
                game.eventsMan.removeListener(cullOnEvents)
                GameLogger.debug(TAG, "Removed CullableOnEvent from EventsManager")
            }

            putCullable(ConstKeys.CULL_EVENTS, cullOnEvents)
        } else removeCullable(ConstKeys.CULL_EVENTS)
    }

    protected abstract fun defineBodyComponent(): BodyComponent

    protected abstract fun defineSpritesComponent(): SpritesComponent

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        return damaged
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        onDamageInflictedTo?.invoke(damageable)
    }

    protected open fun disintegrate(disintegrationProps: Properties? = null) {
        if (overlapsGameCamera()) playSoundNow(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        val disintegration = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.DISINTEGRATION)
        val props = disintegrationProps ?: props(ConstKeys.POSITION to body.getCenter())
        disintegration!!.spawn(props)
    }

    protected open fun explode(explosionProps: Properties? = null) {
        if (overlapsGameCamera()) playSoundNow(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        val props = explosionProps ?: props(ConstKeys.OWNER to this, ConstKeys.POSITION to body.getCenter())
        explosion.spawn(props)
    }

    open fun isMegamanShootingAtMe(): Boolean {
        if (!getMegaman().shooting) return false
        return body.x < getMegaman().body.x && getMegaman().facing == Facing.LEFT || body.x > getMegaman().body.x && getMegaman().facing == Facing.RIGHT
    }
}
