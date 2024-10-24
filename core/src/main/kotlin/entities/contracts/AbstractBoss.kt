package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.points.PointsComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues.EXPLOSION_ORB_SPEED
import com.megaman.maverick.game.events.EventType

abstract class AbstractBoss(
    game: MegamanMaverickGame,
    dmgDuration: Float = DEFAULT_BOSS_DMG_DURATION,
    dmgBlinkDur: Float = DEFAULT_DMG_BLINK_DUR,
    defeatDur: Float = DEFAULT_DEFEAT_DURATION
) : AbstractEnemy(game, dmgDuration, dmgBlinkDur), IEventListener {

    companion object {
        const val TAG = "AbstractBoss"
        const val DEFAULT_BOSS_DMG_DURATION = 1.25f
        const val DEFAULT_DEFEAT_DURATION = 3f
        const val EXPLOSION_TIME = 0.25f
    }

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

    override fun getEntityType() = EntityType.BOSS

    override fun onSpawn(spawnProps: Properties) {
        game.eventsMan.addListener(this)
        mini = spawnProps.getOrDefault(ConstKeys.MINI, false, Boolean::class)
        spawnProps.put(ConstKeys.DROP_ITEM_ON_DEATH, false)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        ready = false
        defeated = false
        defeatTimer.setToEnd()
        bossKey = spawnProps.getOrDefault(
            "${ConstKeys.BOSS}_${ConstKeys.KEY}",
            "NO_BOSS_KEY_FOR_ABSTRACT_BOSS",
            String::class
        )
        super.onSpawn(spawnProps)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        removeProperty("${ConstKeys.BOSS}_${ConstKeys.KEY}")
        game.eventsMan.removeListener(this)
        ready = false

        super.onDestroy()

        if (getCurrentHealth() > 0) return

        playSoundNow(SoundAsset.DEFEAT_SOUND, false)
        val explosionOrbTrajectories = gdxArrayOf(
            Vector2(-EXPLOSION_ORB_SPEED, 0f),
            Vector2(-EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED),
            Vector2(0f, EXPLOSION_ORB_SPEED),
            Vector2(EXPLOSION_ORB_SPEED, EXPLOSION_ORB_SPEED),
            Vector2(EXPLOSION_ORB_SPEED, 0f),
            Vector2(EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED),
            Vector2(0f, -EXPLOSION_ORB_SPEED),
            Vector2(-EXPLOSION_ORB_SPEED, -EXPLOSION_ORB_SPEED)
        )
        explosionOrbTrajectories.forEach { trajectory ->
            val explosionOrb = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION_ORB)
            explosionOrb?.spawn(
                props(
                    ConstKeys.TRAJECTORY pairTo trajectory.scl(ConstVals.PPM.toFloat()),
                    ConstKeys.POSITION pairTo body.getCenter()
                )
            )
        }
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "Boss received event: $event")
        when (event.key) {
            EventType.END_BOSS_SPAWN -> onReady()
            EventType.PLAYER_SPAWN -> destroy()
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (defeated) {
                defeatTimer.update(delta)
                onDefeated(delta)

                damageBlinkTimer.update(delta)
                if (damageBlinkTimer.isFinished()) {
                    damageBlinkTimer.reset()
                    damageBlink = !damageBlink
                }

                if (defeatTimer.isFinished()) {
                    GameLogger.debug(TAG, "Defeat timer is finished, dying and sending BOSS_DEAD event")
                    game.eventsMan.submitEvent(
                        Event(EventType.BOSS_DEAD, props(ConstKeys.BOSS pairTo this))
                    )
                    destroy()
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

    protected open fun onReady() {
        ready = true
    }

    protected open fun triggerDefeat() {
        GameLogger.debug(TAG, "Trigger defeat")
        game.eventsMan.submitEvent(Event(EventType.BOSS_DEFEATED, props(ConstKeys.BOSS pairTo this)))
        defeatTimer.reset()
        defeated = true
    }

    protected open fun onDefeated(delta: Float) {}

    protected open fun explodeOnDefeat(delta: Float) {
        explosionTimer.update(delta)
        if (explosionTimer.isFinished()) {
            val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
            val position = Position.entries.toTypedArray().toGdxArray().random()
            explosion.spawn(
                props(
                    ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND,
                    ConstKeys.POSITION pairTo body.getCenter().add(
                        (position.x - 1) * 0.75f * ConstVals.PPM, (position.y - 1) * 0.75f * ConstVals.PPM
                    )
                )
            )
            explosionTimer.reset()
        }
    }
}
