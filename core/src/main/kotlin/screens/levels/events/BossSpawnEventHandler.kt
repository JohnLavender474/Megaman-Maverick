package com.megaman.maverick.game.screens.levels.events

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.events.EventType

class BossSpawnEventHandler(private val game: MegamanMaverickGame) {

    companion object {
        const val TAG = "BossSpawnEventHandler"
    }

    fun init(bossName: String, bossSpawnProps: Properties) {
        val boss = EntityFactories.fetch(EntityType.BOSS, bossName)!! as AbstractBoss
        boss.ready = false
        boss.spawn(bossSpawnProps)
        game.eventsMan.submitEvent(Event(EventType.BEGIN_BOSS_SPAWN, props(ConstKeys.BOSS pairTo boss)))
    }
}
