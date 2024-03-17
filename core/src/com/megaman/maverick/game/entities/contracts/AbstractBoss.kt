package com.megaman.maverick.game.entities.contracts

import com.engine.common.GameLogger
import com.engine.common.enums.Position
import com.engine.common.extensions.objectSetOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.time.Timer
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.points.PointsComponent
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.events.EventType

abstract class AbstractBoss(
    game: MegamanMaverickGame,
    dmgDuration: Float = DEFAULT_BOSS_DMG_DURATION,
    dmgBlinkDur: Float = DEFAULT_DMG_BLINK_DUR,
    defeatDur: Float = DEFAULT_DEFEAT_DURATION
) : AbstractEnemy(game, dmgDuration, dmgBlinkDur), IEventListener {

    companion object {
        const val TAG = "AbstractBoss"
        const val DEFAULT_BOSS_DMG_DURATION = 0.75f
        const val DEFAULT_DEFEAT_DURATION = 3f
        const val EXPLOSION_TIME = 0.25f
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.END_BOSS_SPAWN, EventType.PLAYER_SPAWN)

    protected val defeatTimer = Timer(defeatDur)
    protected val explosionTimer = Timer(EXPLOSION_TIME)

    var ready = false
    var defeated = false
        private set

    protected open fun triggerDefeat() {
        GameLogger.debug(TAG, "triggerDefeat() = sending event and resetting defeat timer")
        game.eventsMan.submitEvent(Event(EventType.BOSS_DEFEATED, props(ConstKeys.BOSS to this)))
        defeatTimer.reset()
        defeated = true
    }

    protected open fun explodeOnDefeat(delta: Float) {
        explosionTimer.update(delta)
        if (explosionTimer.isFinished()) {
            val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
            val position = Position.values().toGdxArray().random()
            game.gameEngine.spawn(
                explosion,
                props(
                    ConstKeys.SOUND to SoundAsset.EXPLOSION_2_SOUND,
                    ConstKeys.POSITION to body.getCenter().add(
                        position.x * ConstVals.PPM.toFloat(), position.y + ConstVals.PPM.toFloat()
                    )
                )
            )
            explosionTimer.reset()
        }
    }

    override fun spawn(spawnProps: Properties) {
        game.eventsMan.addListener(this)
        spawnProps.put(ConstKeys.DROP_ITEM_ON_DEATH, false)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        ready = false
        defeated = false
        defeatTimer.setToEnd()
        super.spawn(spawnProps)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        game.eventsMan.removeListener(this)
        ready = false
        super.onDestroy()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.END_BOSS_SPAWN -> ready = true
            EventType.PLAYER_SPAWN -> kill()
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (defeated) {
                defeatTimer.update(delta)
                if (defeatTimer.isFinished()) {
                    GameLogger.debug(TAG, "Defeat timer is finished, dying and sending BOSS_DEAD event")
                    game.eventsMan.submitEvent(
                        Event(EventType.BOSS_DEAD, props(ConstKeys.BOSS to this))
                    )
                    kill()
                }
            }
        }
    }

    override fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent(this)
        pointsComponent.putPoints(
            ConstKeys.HEALTH, max = ConstVals.MAX_HEALTH, current = ConstVals.MAX_HEALTH, min = ConstVals.MIN_HEALTH
        )
        pointsComponent.putListener(ConstKeys.HEALTH) {
            if (it.current <= ConstVals.MIN_HEALTH && !defeated) triggerDefeat()
        }
        return pointsComponent
    }
}