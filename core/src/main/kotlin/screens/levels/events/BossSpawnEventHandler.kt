package com.megaman.maverick.game.screens.levels.events

import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.events.EventType

class BossSpawnEventHandler(private val game: MegamanMaverickGame) : Updatable, Resettable {

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
            boss.spawn(bossSpawnProps)
            game.eventsMan.submitEvent(Event(EventType.BEGIN_BOSS_SPAWN, props(ConstKeys.BOSS pairTo boss)))
        }

        spawnTimer.runOnFinished = {
            game.eventsMan.submitEvent(Event(EventType.END_BOSS_SPAWN, props(ConstKeys.MINI pairTo isMini)))

            if (!isMini) {
                val music = MusicAsset.valueOf(
                    bossSpawnProps.getOrDefault(
                        ConstKeys.MUSIC,
                        MusicAsset.MMX6_BOSS_FIGHT_MUSIC.name,
                        String::class
                    ).uppercase()
                )
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

    override fun reset() {
        timerQueue.clear()
    }
}
