package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.points.PointsComponent
import com.mega.game.engine.updatables.UpdatablesComponent
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
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.ExplosionOrb
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues.EXPLOSION_ORB_SPEED
import com.megaman.maverick.game.entities.megaman.contracts.IMegamanDamageListener
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.misc.HealthFillType
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

abstract class AbstractBoss(
    game: MegamanMaverickGame,
    dmgDuration: Float = DEFAULT_BOSS_DMG_DURATION,
    dmgBlinkDur: Float = DEFAULT_DMG_BLINK_DUR,
    defeatDur: Float = DEFAULT_DEFEAT_DURATION,
    size: Size = Size.MEDIUM
) : AbstractEnemy(game, dmgDuration, dmgBlinkDur, size), IMegamanDamageListener, IEventListener {

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
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 2 else 1
            },
            ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 2 else 1
            },
            MoonScythe::class pairTo dmgNeg(1),
            SmallIceCube::class pairTo dmgNeg(1)
        )
    }

    protected open class BossDamageNegotiator : IDamageNegotiator {

        override fun get(damager: IDamager) = BOSS_DMG_NEG[damager::class]?.get(damager) ?: 0
    }

    override val damageNegotiator: IDamageNegotiator = BossDamageNegotiator()
    override val eventKeyMask = objectSetOf<Any>(EventType.END_BOSS_SPAWN, EventType.PLAYER_SPAWN)

    protected open val defeatTimer = Timer(defeatDur)
    protected open val explosionTimer = Timer(EXPLOSION_TIME)

    var ready = false
    var mini = false
        private set
    var defeated = false
        private set
    var bossKey = ""
        private set
    var orbs = true

    var betweenReadyAndEndBossSpawnEvent = false
        private set

    private var end = true

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        game.eventsMan.addListener(this)

        bossKey = spawnProps.getOrDefault("${ConstKeys.BOSS}_${ConstKeys.KEY}", "NO_BOSS_KEY", String::class)
        mini = spawnProps.getOrDefault(ConstKeys.MINI, false, Boolean::class)
        end = spawnProps.getOrDefault(ConstKeys.END, true, Boolean::class)

        spawnProps.put(ConstKeys.DROP_ITEM_ON_DEATH, false)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)

        ready = false
        defeated = false
        betweenReadyAndEndBossSpawnEvent = false

        defeatTimer.setToEnd()

        orbs = spawnProps.getOrDefault(ConstKeys.ORB, !mini, Boolean::class)

        if (playMusicOnSpawn()) {
            val musicStr =
                spawnProps.getOrDefault(ConstKeys.MUSIC, MusicAsset.MMX6_BOSS_FIGHT_MUSIC.name, String::class)
            val music = MusicAsset.valueOf(musicStr.uppercase())
            game.audioMan.playMusic(music, true)
        }

        super.onSpawn(spawnProps)

        megaman.addDamageListener(this)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        GameLogger.log(getTag(), "onDestroy()")

        super.onDestroy()

        ready = false

        megaman.removeDamageListener(this)
        game.eventsMan.removeListener(this)
        removeProperty("${ConstKeys.BOSS}_${ConstKeys.KEY}")

        if (getCurrentHealth() <= 0) {
            if (orbs) EXPLOSION_ORB_TRAJS.forEach { trajectory ->
                val explosionOrb = MegaEntityFactory.fetch(ExplosionOrb::class)!!
                explosionOrb.spawn(
                    props(
                        ConstKeys.POSITION pairTo body.getCenter(),
                        ConstKeys.TRAJECTORY pairTo trajectory.cpy().scl(ConstVals.PPM.toFloat())
                    )
                )
            }

            playSoundNow(SoundAsset.DEFEAT_SOUND, false)
        }
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent: event=$event")
        GameLogger.log(getTag(), "onEvent: event=$event")
        when (event.key) {
            EventType.END_BOSS_SPAWN -> onEndBossSpawnEvent()
            EventType.PLAYER_SPAWN -> destroy()
        }
    }

    override fun editDamageFrom(damager: IDamager, baseDamage: Int) = when {
        damager is IOwnable && damager.owner == megaman && megaman.hasEnhancement(MegaEnhancement.DAMAGE_INCREASE)->
            MegaEnhancement.scaleDamage(baseDamage, MegaEnhancement.BOSS_DAMAGE_INCREASE_SCALAR)

        else -> baseDamage
    }

    override fun onMegamanDamaged(damager: IDamager, megaman: Megaman) {}

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready && isReady(delta)) {
                ready = true
                onReady()
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

    override fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent()
        pointsComponent.putPoints(
            ConstKeys.HEALTH, max = ConstVals.MAX_HEALTH, current = ConstVals.MAX_HEALTH, min = ConstVals.MIN_HEALTH
        )
        pointsComponent.putListener(ConstKeys.HEALTH) {
            if (it.current <= ConstVals.MIN_HEALTH && !defeated) triggerDefeat()
        }
        return pointsComponent
    }

    fun isEndLevelBoss() = !mini && end

    protected open fun playMusicOnSpawn() = !mini

    protected open fun getHealthFillType() = HealthFillType.BIT_BY_BIT

    protected abstract fun isReady(delta: Float): Boolean

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
    }

    protected open fun triggerDefeat() {
        GameLogger.debug(TAG, "triggerDefeat()")
        game.eventsMan.submitEvent(Event(EventType.BOSS_DEFEATED, props(ConstKeys.BOSS pairTo this)))
        defeatTimer.reset()
        defeated = true
    }

    protected open fun onDefeated(delta: Float) {}

    protected open fun explodeOnDefeat(delta: Float) {
        explosionTimer.update(delta)
        if (explosionTimer.isFinished()) {
            spawnDefeatExplosion()
            explosionTimer.reset()
        }
    }

    protected open fun spawnDefeatExplosion() {
        val position = Position.entries.random()

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND,
                ConstKeys.POSITION pairTo body.getCenter().add(
                    (position.x - 1) * 0.75f * ConstVals.PPM, (position.y - 1) * 0.75f * ConstVals.PPM
                )
            )
        )
    }

    override fun getType() = EntityType.BOSS
}
