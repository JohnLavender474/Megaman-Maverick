package com.megaman.maverick.game.entities.contracts

import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.events.Event
import com.engine.events.IEventListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.events.EventType

abstract class AbstractBoss(
    game: MegamanMaverickGame,
    dmgDuration: Float = DEFAULT_BOSS_DMG_DURATION,
    dmgBlinkDur: Float = DEFAULT_DMG_BLINK_DUR
) : AbstractEnemy(game, dmgDuration, dmgBlinkDur), IEventListener {

    companion object {
        const val TAG = "AbstractBoss"
        const val DEFAULT_BOSS_DMG_DURATION = 0.75f
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.END_BOSS_SPAWN)

    var ready = false

    override fun init() {
        super.init()
        dropItemOnDeath = false
    }

    override fun spawn(spawnProps: Properties) {
        game.eventsMan.addListener(this)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        ready = false
        super.spawn(spawnProps)
    }

    override fun onDestroy() {
        game.eventsMan.removeListener(this)
        ready = false
        super.onDestroy()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.END_BOSS_SPAWN -> ready = true
        }
    }
}