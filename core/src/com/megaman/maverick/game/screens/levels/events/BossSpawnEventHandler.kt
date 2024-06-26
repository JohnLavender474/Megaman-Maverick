package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.utils.Queue
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.time.Timer
import com.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.events.EventType

class BossSpawnEventHandler(private val game: MegamanMaverickGame) : Updatable {

    companion object {
        const val TAG = "BossSpawnEventHandler"
        private const val SPAWN_TIME = 1f
    }

    val finished: Boolean
        get() = timerQueue.isEmpty

    private val timerQueue = Queue<Timer>()

    fun init(bossName: String, bossSpawnProps: Properties, isMini: Boolean = false) {
        val spawnTimer = Timer(SPAWN_TIME)

        spawnTimer.runOnFirstUpdate = {
            val boss = EntityFactories.fetch(EntityType.BOSS, bossName)!! as AbstractBoss
            boss.ready = false
            game.engine.spawn(boss, bossSpawnProps)
            game.eventsMan.submitEvent(
                Event(
                    EventType.BEGIN_BOSS_SPAWN,
                    props(ConstKeys.BOSS to boss)
                )
            )
        }

        spawnTimer.runOnFinished = {
            game.engine.systems.forEach { it.on = true }
            game.eventsMan.submitEvent(Event(EventType.END_BOSS_SPAWN, props(ConstKeys.MINI to isMini)))

            if (!isMini) {
                val music = MusicAsset.valueOf(bossSpawnProps.get(ConstKeys.MUSIC, String::class)!!)
                game.audioMan.playMusic(music, true)
            }
        }

        timerQueue.addLast(spawnTimer)
    }

    override fun update(delta: Float) {
        if (timerQueue.isEmpty) return
        val timer = timerQueue.first()
        timer.update(delta)
        if (timer.isFinished()) timerQueue.removeFirst()
    }
}
