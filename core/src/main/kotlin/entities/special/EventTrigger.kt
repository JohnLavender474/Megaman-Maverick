package com.megaman.maverick.game.entities.special


import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType

class EventTrigger(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity {

    companion object {
        const val TAG = "EventTrigger"
        const val EVENT_TYPE = "${ConstKeys.EVENT}_${ConstKeys.TYPE}"
    }

    override fun getEntityType() = EntityType.SPECIAL

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        lateinit var eventType: EventType
        val eventProps = Properties()
        spawnProps.forEach { key, value ->
            if (key == EVENT_TYPE) eventType = EventType.valueOf(value.toString().uppercase())
            else eventProps.put(key, value)
        }
        val event = Event(eventType, eventProps)
        GameLogger.debug(TAG, "onSpawn(): event=$event, spawnProps=$spawnProps")
        game.eventsMan.submitEvent(event)
        destroy()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }
}
