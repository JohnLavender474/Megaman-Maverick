package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.audio.SoundRequest
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageable
import com.maverick.game.engine.damage.IDamager
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.players.forEach
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.explosions.*
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues.EXPLOSION_ORB_SPEED
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.players.IPlayerDamageListener
import com.megaman.maverick.game.players.MegaPlayer
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.utils.misc.HealthFillType
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

abstract class AbstractBoss(
    game: MegamanMaverickGame,
    dmgDuration: Float = DEFAULT_BOSS_DMG_DURATION,
    dmgBlinkDur: Float = DEFAULT_DMG_BLINK_DUR,
    defeatDur: Float = DEFAULT_DEFEAT_DURATION,
    size: Size = Size.MEDIUM
) : AbstractEnemy(game, dmgDuration, dmgBlinkDur, size), IPlayerDamageListener, IEventListener {

    companion object {
        const val TAG = "AbstractBoss"

        const val DEFAULT_BOSS_DMG_DURATION = 1.25f
        const val DEFAULT_MINI_BOSS_DMG_DURATION = 0.5f
        const val DEFAULT_DEFEAT_DURATION = 3f
        const val EXPLOSION_TIME = 0.25f

        val EXPLOSION_ORB_TRAJS = gdxArrayOf(
            Vector2(-EXPLOSION_ORB_SPEED, 0f),
            Vector2(-EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED),
            Vector2(0f, EXPLOSION_ORB_SPEED),
            Vector2(EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED),
            Vector2(EXPLOSION_ORB_SPEED, 0f),
            Vector2(EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED),
            Vector2(0f, -EXPLOSION_ORB_SPEED),
            Vector2(-EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED)
        )

        protected val BOSS_DMG_NEG = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(1),
            Fireball::class pairTo dmgNeg(1),
            MagmaFlame::class pairTo dmgNeg(1),
            MagmaWave::class pairTo dmgNeg(1),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 2 else 1
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 2 else 1
            },
            MoonScythe::class pairTo dmgNeg(1),
            SmallIceCube::class pairTo dmgNeg(1),
            Axe::class pairTo dmgNeg(1),
            Needle::class pairTo dmgNeg(1),
            SlashWave::class pairTo dmgNeg(1),
            PreciousGem::class pairTo dmgNeg(1),
            ReactorManProjectile::class pairTo dmgNeg(1),
            IceBombExplosion::class pairTo dmgNeg(1),
            ReactorExplosion::class pairTo dmgNeg(1)
        )

        private val DEFAULT_BOSS_BATTLE_MUSIC = MusicAsset.MMX6_BOSS_FIGHT_MUSIC.name
    }

    protected open class BossDamageNegotiator : IDamageNegotiator {

        override fun get(damager: IDamager) = BOSS_DMG_NEG[damager::class]?.get(damager) ?: 0
    }

    override val damageNegotiator: IDamageNegotiator = BossDamageNegotiator()
    override val eventKeyMask = objectSetOf<Any>(EventType.END_BOSS_SPAWN, EventType.PLAYER_SPAWN)

    protected open val defeatTimer = Timer(defeatDur)
    protected open val spawnDefeatExplosionTimer = Timer(EXPLOSION_TIME)

    var ready = false
    var mini = false
        private set
    var defeated = false
        private set
    var bossKey = ""
        private set
    var spawnDefeatOrbs = true

    var betweenReadyAndEndBossSpawnEvent = false
        private set

    private var end = true

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        // Assume that none of the bosses should die by spikes or other "instant death" contacts
        body.forEachFixture { fixture -> fixture.putProperty(ConstKeys.DEATH_LISTENER, false) }

        game.eventsMan.addListener(this)
        // TODO: a player who registers during a boss fight is not listened to; joining players are a co-op rule
        //  for item 047
        players.forEach { it.addDamageListener(this) }

        bossKey = spawnProps.getOrDefault("${ConstKeys.BOSS}_${ConstKeys.KEY}", "NO_BOSS_KEY", String::class)
        mini = spawnProps.getOrDefault(ConstKeys.MINI, false, Boolean::class)
        end = spawnProps.getOrDefault(ConstKeys.END, true, Boolean::class)

        spawnProps.put(ConstKeys.DROP_ITEM_ON_DEATH, false)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)

        ready = false
        defeated = false
        betweenReadyAndEndBossSpawnEvent = false

        defeatTimer.setToEnd()
        spawnDefeatOrbs = spawnProps.getOrDefault(ConstKeys.ORB, !mini, Boolean::class)

        if (spawnProps.containsKey(ConstKeys.MUSIC)) putProperty(ConstKeys.MUSIC, spawnProps.get(ConstKeys.MUSIC))

        super.onSpawn(spawnProps)

        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        ready = false

        players.forEach { it.removeDamageListener(this) }
        game.eventsMan.removeListener(this)
        removeProperty("${ConstKeys.BOSS}_${ConstKeys.KEY}")

        if (isHealthDepleted() && spawnDefeatOrbs) spawnExplosionOrbs(body.getCenter())
    }

    protected open fun spawnExplosionOrbs(spawn: Vector2) {
        playSoundNow(SoundAsset.DEFEAT_SOUND, false)

        EXPLOSION_ORB_TRAJS.forEach { trajectory ->
            val orb = MegaEntityFactory.fetch(ExplosionOrb::class)!!
            orb.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.TRAJECTORY pairTo trajectory.cpy().scl(ConstVals.PPM.toFloat())
                )
            )
        }
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent: event=$event")
        when (event.key) {
            EventType.END_BOSS_SPAWN -> onEndBossSpawnEvent()
            EventType.PLAYER_SPAWN -> destroy()
        }
    }

    override fun canDamage(damageable: IDamageable) = !defeated && ready && super.canDamage(damageable)

    override fun canBeDamagedBy(damager: IDamager) = !defeated && super.canBeDamagedBy(damager)

    // TODO: the enhancement is read from the global megaman, not from the owner's profile, until item 029e
    override fun editDamageFrom(damager: IDamager, baseDamage: Int) = when {
        damager is IOwnable<*> && damager.owner.asPlayer != null &&
            megaman.hasEnhancement(MegaEnhancement.DAMAGE_INCREASE) ->
            MegaEnhancement.scaleDamage(baseDamage, MegaEnhancement.BOSS_DAMAGE_INCREASE_SCALAR)

        else -> baseDamage
    }

    override fun onPlayerDamaged(damager: IDamager, player: MegaPlayer) {}

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) {
                preReady(delta)
                if (isReady(delta)) {
                    ready = true
                    onReady()
                }
            }

            if (defeated) {
                defeatTimer.update(delta)
                onDefeated(delta)

                damageBlinkTimer.update(delta)
                if (damageBlinkTimer.isFinished()) {
                    damageBlinkTimer.reset()
                    damageBlink = !damageBlink
                }

                if (defeatTimer.isFinished()) {
                    GameLogger.debug(TAG, "update(): defeat timer finished")
                    destroy()
                    game.eventsMan.submitEvent(Event(EventType.BOSS_DEAD, props(ConstKeys.BOSS pairTo this)))
                }
            }
        }
    }

    protected open fun shouldTriggerDefeat(health: Int) = health <= ConstVals.MIN_HEALTH && !defeated

    override fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent()
        pointsComponent.putPoints(
            ConstKeys.HEALTH, max = ConstVals.MAX_HEALTH, current = ConstVals.MAX_HEALTH, min = ConstVals.MIN_HEALTH
        )
        pointsComponent.putListener(ConstKeys.HEALTH) {
            if (shouldTriggerDefeat(it.current)) triggerDefeat()
        }
        return pointsComponent
    }

    open fun getEventOnDefeated(): EventType? =
        if (!mini && end) EventType.VICTORY_EVENT else EventType.INTERMEDIATE_BOSS_DEAD

    protected open fun playBossMusic() = !mini

    protected open fun getHealthFillType() = HealthFillType.BIT_BY_BIT

    protected abstract fun isReady(delta: Float): Boolean

    protected open fun preReady(delta: Float) {}

    protected open fun onReady() {
        GameLogger.debug(TAG, "onReady()")

        val event = Event(
            EventType.BOSS_READY, props(
                ConstKeys.BOSS pairTo this,
                ConstKeys.HEALTH_FILL_TYPE pairTo getHealthFillType()
            )
        )
        game.eventsMan.submitEvent(event)

        betweenReadyAndEndBossSpawnEvent = true
    }

    protected open fun onEndBossSpawnEvent() {
        GameLogger.debug(TAG, "onEndBossSpawnEvent()")

        betweenReadyAndEndBossSpawnEvent = false

        if (playBossMusic()) {
            try {
                val musicStr = getOrDefaultProperty(ConstKeys.MUSIC, DEFAULT_BOSS_BATTLE_MUSIC, String::class)
                val ass = MusicAsset.valueOf(musicStr.uppercase())
                game.audioMan.playMusic(ass, ass.loop)
            } catch (e: Exception) {
                throw Exception("Failed to play music. Music prop: ${getProperty(ConstKeys.MUSIC)}.", e)
            }
        }
    }

    protected open fun triggerDefeat() {
        GameLogger.debug(TAG, "triggerDefeat()")
        game.eventsMan.submitEvent(Event(EventType.BOSS_DEFEATED, props(ConstKeys.BOSS pairTo this)))
        defeatTimer.reset()
        defeated = true
    }

    protected open fun onDefeated(delta: Float) {}

    protected open fun explodeOnDefeat(delta: Float) {
        spawnDefeatExplosionTimer.update(delta)
        if (spawnDefeatExplosionTimer.isFinished()) {
            spawnDefeatExplosion()
            spawnDefeatExplosionTimer.reset()
        }
    }

    protected open fun spawnDefeatExplosion() {
        val position = Position.entries.random()

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter().add(
                    (position.x - 1) * 0.75f * ConstVals.PPM, (position.y - 1) * 0.75f * ConstVals.PPM
                ),
                ConstKeys.DAMAGER pairTo false,
            )
        )

        game.audioMan.playSound(
            SoundRequest(SoundAsset.EXPLOSION_2_SOUND, loop = false, allowOverlap = true)
        )
    }

    override fun getType() = EntityType.BOSS
}
