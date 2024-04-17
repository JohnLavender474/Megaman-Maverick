package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.ObjectMap
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.extensions.objectSetOf
import com.engine.common.getRandom
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.time.Timer
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.events.Event
import com.engine.points.PointsComponent
import com.engine.updatables.UpdatablesComponent
import com.engine.world.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ItemsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.setStandardOnPortalHopperEndProp
import com.megaman.maverick.game.entities.utils.setStandardOnPortalHopperStartProp
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.utils.toGameRectangle
import kotlin.reflect.KClass

abstract class AbstractEnemy(
    game: MegamanMaverickGame, dmgDuration: Float = DEFAULT_DMG_DURATION, dmgBlinkDur: Float = DEFAULT_DMG_BLINK_DUR
) : GameEntity(game), IDamager, IDamageable, IBodyEntity, IAudioEntity, IHealthEntity, ISpriteEntity, ICullableEntity {

    companion object {
        const val TAG = "AbstractEnemy"
        const val DEFAULT_CULL_TIME = 1f
        const val DEFAULT_DMG_DURATION = .15f
        const val DEFAULT_DMG_BLINK_DUR = .025f
    }

    override val invincible: Boolean
        get() = !damageTimer.isFinished()

    protected val megaman: Megaman
        get() = getMegamanMaverickGame().megaman

    protected abstract val damageNegotiations: ObjectMap<KClass<out IDamager>, DamageNegotiation>

    protected val damageTimer = Timer(dmgDuration)
    protected val damageBlinkTimer = Timer(dmgBlinkDur)

    protected var damageBlink = false
    protected var dropItemOnDeath = true
    protected var onDamageInflictedTo: ((IDamageable) -> Unit)? = null

    override fun init() {
        addComponent(definePointsComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent(this))
        addComponent(CullablesComponent(this))
        val updatablesComponent = UpdatablesComponent(this)
        defineUpdatablesComponent(updatablesComponent)
        addComponent(updatablesComponent)
        runnablesOnSpawn.add { setHealth(getMaxHealth()) }
        runnablesOnDestroy.add {
            if (hasDepletedHealth()) {
                disintegrate()
                if (dropItemOnDeath) {
                    val randomInt = getRandom(0, 10)
                    val props = props(ConstKeys.POSITION to body.getCenter())
                    val entity: IGameEntity? = when (randomInt) {
                        0, 1, 2 -> {
                            props.put(ConstKeys.LARGE, randomInt == 1)
                            EntityFactories.fetch(EntityType.ITEM, ItemsFactory.HEALTH_BULB)
                        }

                        3, 4, 5 -> {
                            // TODO: EntityFactories.fetch(EntityType.ITEM, ItemsFactory.WEAPON_ENERGY)
                            null
                        }

                        6, 7, 8, 9, 10 -> {
                            GameLogger.debug(TAG, "No item dropped")
                            null
                        }

                        else -> null
                    }
                    entity?.let { game.engine.spawn(it, props) }
                }
            }
        }
        setStandardOnPortalHopperStartProp(this)
        setStandardOnPortalHopperEndProp(this)
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        onDamageInflictedTo = spawnProps.get(ConstKeys.ON_DAMAGE_INFLICTED_TO) as ((IDamageable) -> Unit)?
        dropItemOnDeath = spawnProps.getOrDefault(ConstKeys.DROP_ITEM_ON_DEATH, true, Boolean::class)
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
            runnablesOnSpawn.add {
                game.eventsMan.addListener(cullOnEvents)
                GameLogger.debug(TAG, "Added CullableOnEvent from EventsManager")
            }
            runnablesOnDestroy.add {
                game.eventsMan.removeListener(cullOnEvents)
                GameLogger.debug(TAG, "Removed CullableOnEvent from EventsManager")
            }
            putCullable(ConstKeys.CULL_EVENTS, cullOnEvents)
        } else removeCullable(ConstKeys.CULL_EVENTS)
        damageTimer.setToEnd()
        damageBlinkTimer.setToEnd()
    }

    protected open fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent(this)
        pointsComponent.putPoints(
            ConstKeys.HEALTH, max = ConstVals.MAX_HEALTH, current = ConstVals.MAX_HEALTH, min = ConstVals.MIN_HEALTH
        )
        pointsComponent.putListener(ConstKeys.HEALTH) {
            if (it.current <= ConstVals.MIN_HEALTH) kill()
        }
        return pointsComponent
    }

    protected abstract fun defineBodyComponent(): BodyComponent

    protected abstract fun defineSpritesComponent(): SpritesComponent

    override fun canBeDamagedBy(damager: IDamager) = !invincible &&
            (damageNegotiations.get(damager::class)?.get(damager) ?: 0) > 0

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damagerKey = damager::class
        if (!damageNegotiations.containsKey(damagerKey)) return false
        damageTimer.reset()
        val damage = damageNegotiations[damagerKey].get(damager)
        if (damage <= 0) return false
        addHealth(-damage)
        requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        return true
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        onDamageInflictedTo?.invoke(damageable)
    }

    protected open fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        updatablesComponent.add {
            damageTimer.update(it)
            if (!damageTimer.isFinished()) {
                damageBlinkTimer.update(it)
                if (damageBlinkTimer.isFinished()) {
                    damageBlinkTimer.reset()
                    damageBlink = !damageBlink
                }
            }
            if (damageTimer.isJustFinished()) damageBlink = false
        }
    }

    protected open fun disintegrate(disintegrationProps: Properties? = null) {
        getMegamanMaverickGame().audioMan.playSound(SoundAsset.ENEMY_DAMAGE_SOUND)
        val disintegration = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.DISINTEGRATION)
        val props = disintegrationProps ?: props(ConstKeys.POSITION to body.getCenter())
        game.engine.spawn(disintegration!!, props)
    }

    protected open fun explode(explosionProps: Properties? = null) {
        getMegamanMaverickGame().audioMan.playSound(SoundAsset.ENEMY_DAMAGE_SOUND)
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)
        val props = explosionProps ?: props(ConstKeys.POSITION to body.getCenter())
        game.engine.spawn(explosion!!, props)
    }

    open fun isMegamanShootingAtMe(): Boolean {
        val megaman = getMegamanMaverickGame().megaman
        if (!megaman.shooting) return false
        return body.x < megaman.body.x && megaman.facing == Facing.LEFT || body.x > megaman.body.x && megaman.facing == Facing.RIGHT
    }

    open fun isInGameCamBounds() = getMegamanMaverickGame().getGameCamera().toGameRectangle().overlaps(body as Rectangle)
}
