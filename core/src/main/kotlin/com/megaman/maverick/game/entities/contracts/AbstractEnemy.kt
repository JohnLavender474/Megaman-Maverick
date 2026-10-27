package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.UtilMethods.getRandom
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.ISizable
import com.maverick.game.engine.common.objects.GamePair
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.shapes.ShapeUtils
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.cullables.CullableOnEvent
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.damage.IDamager
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.entities.direction
import com.maverick.game.engine.entities.facing
import com.maverick.game.engine.players.any
import com.maverick.game.engine.players.playerSlot
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.SelfSizeDamageNegotiator
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.decorations.FloatingPoints
import com.megaman.maverick.game.entities.decorations.FloatingPoints.FloatingPointsType
import com.megaman.maverick.game.entities.decorations.Whack
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.healthRatio
import com.megaman.maverick.game.entities.items.HealthBulb
import com.megaman.maverick.game.entities.items.Life
import com.megaman.maverick.game.entities.items.WeaponEnergyBulb
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.entities.shooting
import com.megaman.maverick.game.entities.special.CartV2
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.isSensing
import kotlin.reflect.KClass

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
        const val MEGAMAN_HEALTH_INFLUENCE_FACTOR = 0.25f

        const val LIVES_THRESHOLD = 3
        const val LOW_LIFE_CHANCE = 4
        const val HIGH_LIFE_CHANCE = 8

        // TODO: const val SCREW_CHANCE = 50

        const val HEALTH_CHANCE = 25
        const val WEAPON_CHANCE = 50

        const val LARGE_ITEM_CHANCE = 25

        private const val SPAWN_BLINK_INTERVAL = 0.05f

        // TODO: the HealthBulb odds read the target player's health ratio; with more players, whose health counts
        //  is a co-op rule (item 047)
        private val DROP_ENTITIES = gdxArrayOf<GamePair<KClass<out MegaGameEntity>, (AbstractEnemy) -> Number>>(
            Life::class pairTo {
                when {
                    it.game.state.getLives(it.targetPlayer.playerSlot!!) < LIVES_THRESHOLD -> HIGH_LIFE_CHANCE
                    else -> LOW_LIFE_CHANCE
                }
            },
            // TODO: Screw::class pairTo { SCREW_CHANCE },
            HealthBulb::class pairTo chance@{ it ->
                val playerHealthModifier = 100 - (100 * it.targetPlayer.healthRatio)
                return@chance HEALTH_CHANCE + (playerHealthModifier * MEGAMAN_HEALTH_INFLUENCE_FACTOR)
            },
            WeaponEnergyBulb::class pairTo { WEAPON_CHANCE }
        )
    }

    val canDamagePredicates = Array<(IDamageable) -> Boolean>()

    override val damageNegotiator: IDamageNegotiator = SelfSizeDamageNegotiator(this)

    protected var movementScalar = 1f
    protected var dropItemOnDeath = true
    protected var onDamageInflictedTo: ((IDamageable) -> Unit)? = null

    private val spawnBlinkTimer = Timer(0f)
    private val spawnBlinkIntervalTimer = Timer(SPAWN_BLINK_INTERVAL)
    private var spawnBlinkHidden = false

    override fun init(vararg params: Any) {
        super.init()

        addComponent(AudioComponent())
        addComponent(CullablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())

        invinciblePredicates.add { isSpawnBlinking() }
        canDamagePredicates.add { !isSpawnBlinking() }

        runnablesOnDestroy.put(ConstKeys.ITEMS) {
            if (isHealthDepleted() && canSpawnItemOnHealthDepleted()) {
                disintegrate()

                if (dropItemOnDeath && !body.isSensing(BodySense.BODY_TOUCHING_BLOCK)) {
                    DROP_ENTITIES.shuffle()
                    DROP_ENTITIES.any { (type, chanceFunction) ->
                        val random = getRandom(0, 100)
                        val chance = chanceFunction.invoke(this)
                        if (random <= chance.toInt()) {
                            val position = body.getCenter()
                            val large = getRandom(0, 100) <= LARGE_ITEM_CHANCE
                            val entity = MegaEntityFactory.fetch(type)!!
                            return@any entity.spawn(
                                props(ConstKeys.LARGE pairTo large, ConstKeys.POSITION pairTo position)
                            )
                        }

                        return@any false
                    }
                }
            }
        }
    }

    fun isSpawnBlinking() = !spawnBlinkTimer.isFinished()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!isSpawnBlinking()) return@add

            spawnBlinkTimer.update(delta)
            if (spawnBlinkTimer.isFinished()) {
                spawnBlinkHidden = false
                damageBlink = false
                return@add
            }

            spawnBlinkIntervalTimer.update(delta)
            if (spawnBlinkIntervalTimer.isFinished()) {
                spawnBlinkHidden = !spawnBlinkHidden
                spawnBlinkIntervalTimer.reset()
            }

            damageBlink = spawnBlinkHidden
        }
    }

    protected open fun canSpawnItemOnHealthDepleted() = lastDamager != CartV2

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawnBlink = spawnProps.getOrDefault(ConstKeys.SPAWN_BLINK, 0f, Float::class)
        if (spawnBlink > 0f) {
            spawnBlinkTimer.resetDuration(spawnBlink)
            spawnBlinkIntervalTimer.reset()
        } else spawnBlinkTimer.setToEnd()
        spawnBlinkHidden = false

        onDamageInflictedTo = spawnProps.get(ConstKeys.ON_DAMAGE_INFLICTED_TO) as ((IDamageable) -> Unit)?
        dropItemOnDeath = spawnProps.getOrDefault(ConstKeys.DROP_ITEM_ON_DEATH, true, Boolean::class)
        movementScalar = spawnProps.getOrDefault("${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}", 1f, Float::class)

        val cullWhenOutOfCamBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullWhenOutOfCamBounds) {
            val cullTime = spawnProps.getOrDefault(ConstKeys.CULL_TIME, DEFAULT_CULL_TIME, Float::class)

            val cullBoundsSupplier = spawnProps.getOrDefault(
                "${ConstKeys.CULL}_${ConstKeys.BOUNDS}_${ConstKeys.SUPPLIER}",
                { body.getBounds() }
            ) as () -> GameRectangle

            val cullOnOutOfBounds = getGameCameraCullingLogic(
                game.getGameCamera(),
                cullBoundsSupplier,
                cullTime
            )

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

    override fun editDamageFrom(damager: IDamager, baseDamage: Int) = when {
        damager is IOwnable<*> && damager.owner.asPlayer != null &&
            game.state.containsEnhancement(MegaEnhancement.DAMAGE_INCREASE) ->
            MegaEnhancement.scaleDamage(baseDamage, MegaEnhancement.ENEMY_DAMAGE_INCREASE_SCALAR)
        else -> baseDamage
    }

    override fun canDamage(damageable: IDamageable) = existenceState.liveAfterUpdate && canDamagePredicates.all { it.invoke(damageable) }

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        val canBeDamaged = super.canBeDamagedBy(damager)
        if (!canBeDamaged) return false

        if (damager is ReactorManProjectile) return damager.owner != this
        if (damager is IOwnable<*>) return damager.owner.asPlayer != null || damager.owner == null

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

    open fun isMegamanShootingAtMe() = players.any { player ->
        player.shooting && (body.getX() < player.body.getX() && player.facing == Facing.LEFT ||
            body.getX() > player.body.getX() && player.facing == Facing.RIGHT)
    }

    open fun spawnWhackForOverlap(thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val overlap = GameObjectPools.fetch(GameRectangle::class)
        ShapeUtils.intersectRectangles(thisShape.getBoundingRectangle(), otherShape.getBoundingRectangle(), overlap)

        val position = overlap.getCenter()

        val whack = MegaEntityFactory.fetch(Whack::class)!!
        whack.spawn(props(ConstKeys.POSITION pairTo position))

        playSoundNow(SoundAsset.SMB3_KICK_SOUND, false)
    }

    open fun spawnFloatingPoints() {
        var floatingPointsType = FloatingPointsType.POINTS100

        val timeSinceLastFloatingPoints =
            game.getProperty(ConstKeys.TIME_SINCE_LAST_FLOATING_POINTS, Float::class)!!

        if (timeSinceLastFloatingPoints < ConstVals.AGGREGATE_FLOATING_POINTS_DUR) {
            val previousFloatingPointsType =
                game.getProperty(ConstKeys.LAST_FLOATING_POINTS_TYPE, FloatingPointsType::class)

            if (previousFloatingPointsType != null &&
                previousFloatingPointsType.ordinal < FloatingPointsType.entries.size - 1
            ) floatingPointsType = FloatingPointsType.entries[previousFloatingPointsType.ordinal + 1]
        }

        game.putProperty(ConstKeys.LAST_FLOATING_POINTS_TYPE, floatingPointsType)
        game.putProperty(ConstKeys.TIME_SINCE_LAST_FLOATING_POINTS, 0f)

        val pointsToAdd = floatingPointsType.points
        if (pointsToAdd != null) {
            val currentPoints = game.getOrDefaultProperty(ConstKeys.LEVEL_POINTS, 0, Int::class)
            game.putProperty(ConstKeys.LEVEL_POINTS, currentPoints + pointsToAdd)
        }

        // TODO: the one-up goes to the target player's slot; with more players, who gets it is a co-op rule (item 047)
        if (floatingPointsType == FloatingPointsType.ONE_UP && game.state.addLife(targetPlayer.playerSlot!!))
            requestToPlaySound(SoundAsset.ONE_UP_SOUND, false)

        val position = when (targetPlayer.direction) {
            Direction.UP -> GameObjectPools.fetch(Vector2::class)
                .set(
                    UtilMethods.getRandom(
                        body.getBounds().getX(),
                        body.getBounds().getMaxX()
                    ),
                    body.getBounds().getY()
                )
            Direction.DOWN -> GameObjectPools.fetch(Vector2::class)
                .set(
                    UtilMethods.getRandom(
                        body.getBounds().getX(),
                        body.getBounds().getMaxX()
                    ),
                    body.getBounds().getMaxY()
                )
            Direction.LEFT -> GameObjectPools.fetch(Vector2::class)
                .set(
                    body.getBounds().getMaxX(),
                    UtilMethods.getRandom(
                        body.getBounds().getY(),
                        body.getBounds().getMaxY()
                    ),
                )
            Direction.RIGHT -> GameObjectPools.fetch(Vector2::class)
                .set(
                    body.getBounds().getX(),
                    UtilMethods.getRandom(
                        body.getBounds().getY(),
                        body.getBounds().getMaxY()
                    ),
                )
        }

        FloatingPoints.spawnFloatingPoints(floatingPointsType, position, targetPlayer.direction)
    }

    override fun getType() = EntityType.ENEMY
}
