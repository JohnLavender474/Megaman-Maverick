package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.UtilMethods.getRandomBool
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.ISizable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.cullables.CullableOnEvent
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.SelfSizeDamageNegotiator
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ItemsFactory
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.entities.utils.setStandardOnTeleportEndProp
import com.megaman.maverick.game.entities.utils.setStandardOnTeleportStartProp
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.getCenter

abstract class AbstractEnemy(
    game: MegamanMaverickGame,
    dmgDuration: Float = DEFAULT_DMG_DURATION,
    dmgBlinkBlur: Float = DEFAULT_DMG_BLINK_DUR,
    override var size: Size = Size.MEDIUM
) : AbstractHealthEntity(game, dmgDuration, dmgBlinkBlur), IDamager, IBodyEntity, IAudioEntity, ISpritesEntity,
    ICullableEntity, ISizable {

    companion object {
        const val TAG = "AbstractEnemy"
        const val DEFAULT_CULL_TIME = 1f
        const val BASE_DROP_ITEM_CHANCE = 0.2f
        const val MEGAMAN_HEALTH_INFLUENCE_FACTOR = 0.3f
    }

    override val damageNegotiator: IDamageNegotiator = SelfSizeDamageNegotiator(this)

    protected var movementScalar = 1f
    protected var dropItemOnDeath = true
    protected var onDamageInflictedTo: ((IDamageable) -> Unit)? = null

    override fun init() {
        super.init()

        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(AudioComponent())
        addComponent(CullablesComponent())

        runnablesOnDestroy.put(ConstKeys.ITEMS) {
            if (isHealthDepleted()) {
                disintegrate()

                if (dropItemOnDeath) {
                    val playerHealthModifier = 1f - megaman.getHealthRatio()
                    val dropChance = BASE_DROP_ITEM_CHANCE + (playerHealthModifier * MEGAMAN_HEALTH_INFLUENCE_FACTOR)
                    val rand = getRandom(0f, 1f)
                    GameLogger.debug(
                        TAG,
                        "Player health modifier = $playerHealthModifier. Drop chance = $dropChance. Random: $rand"
                    )
                    if (rand < dropChance) {
                        val props = props(ConstKeys.POSITION pairTo body.getCenter())
                        val spawnHealth = getRandomBool()
                        val entity = if (spawnHealth) {
                            props.put(ConstKeys.LARGE, getRandomBool())
                            EntityFactories.fetch(EntityType.ITEM, ItemsFactory.HEALTH_BULB)
                        } else EntityFactories.fetch(EntityType.ITEM, ItemsFactory.WEAPON_ENERGY)
                        entity?.spawn(props)
                    }
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

    protected open fun isDamagerOwnedByMegaman(damager: IDamager) = damager is IOwnable &&
        damager.owner == megaman && megaman.has(MegaEnhancement.DAMAGE_INCREASE)

    override fun editDamageFrom(damager: IDamager, baseDamage: Int) = when {
        isDamagerOwnedByMegaman(damager) -> MegaEnhancement.scaleDamage(
            baseDamage, MegaEnhancement.ENEMY_DAMAGE_INCREASE_SCALAR
        )

        else -> baseDamage
    }

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        val canBeDamaged = super.canBeDamagedBy(damager)
        if (!canBeDamaged) return false

        if (damager is IOwnable) return damager.owner == megaman || damager.owner == null

        return true
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        return damaged
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        onDamageInflictedTo?.invoke(damageable)
    }

    protected open fun disintegrate(disintegrationProps: Properties? = null) {
        if (overlapsGameCamera()) playSoundNow(SoundAsset.ENEMY_DAMAGE_SOUND, false)

        val disintegration = MegaEntityFactory.fetch(Disintegration::class)!!
        val props = disintegrationProps ?: props(ConstKeys.POSITION pairTo body.getCenter())
        disintegration.spawn(props)
    }

    protected open fun explode(explosionProps: Properties? = null) {
        if (overlapsGameCamera()) playSoundNow(SoundAsset.EXPLOSION_2_SOUND, false)

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        val props = explosionProps ?: props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo body.getCenter())
        explosion.spawn(props)
    }

    open fun isMegamanShootingAtMe(): Boolean {
        if (!megaman.shooting) return false

        return body.getX() < megaman.body.getX() && megaman.facing == Facing.LEFT ||
            body.getX() > megaman.body.getX() && megaman.facing == Facing.RIGHT
    }

    override fun getType() = EntityType.ENEMY
}
