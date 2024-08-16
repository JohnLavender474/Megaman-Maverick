package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.time.Timer
import com.engine.entities.contracts.IAudioEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.events.EventType

class RoomShaker(game: MegamanMaverickGame) : MegaGameEntity(game), IEventListener, IAudioEntity {

    companion object {
        const val TAG = "RoomShaker"
    }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_SPAWN,
        EventType.BEGIN_ROOM_TRANS,
        EventType.END_ROOM_TRANS,
        EventType.SET_TO_ROOM_NO_TRANS
    )

    private lateinit var roomToShake: String
    private lateinit var delayTimer: Timer

    private var sound: SoundAsset? = null

    private var interval = 0f
    private var duration = 0f
    private var x = 0f
    private var y = 0f
    private var run = false

    override fun init() {
        super<MegaGameEntity>.init()
        addComponent(defineUpdatablesComponent())
        addComponent(AudioComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawning RoomShake entity")
        super.spawn(spawnProps)

        game.eventsMan.addListener(this)

        roomToShake = spawnProps.get(ConstKeys.ROOM, String::class)!!

        val delay = spawnProps.get(ConstKeys.DELAY, Float::class)!!
        delayTimer = Timer(delay)

        interval = spawnProps.get(ConstKeys.INTERVAL, Float::class)!!
        duration = spawnProps.get(ConstKeys.DURATION, Float::class)!!
        x = spawnProps.get(ConstKeys.X, Float::class)!!
        y = spawnProps.get(ConstKeys.Y, Float::class)!!

        run = false

        sound = if (spawnProps.containsKey(ConstKeys.SOUND))
            SoundAsset.valueOf(spawnProps.get(ConstKeys.SOUND, String::class)!!.uppercase())
        else null
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "Destroying RoomShake entity")
        super<MegaGameEntity>.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "Received event: $event")

        when (event.key) {
            EventType.PLAYER_SPAWN, EventType.BEGIN_ROOM_TRANS -> {
                GameLogger.debug(TAG, "Room transition started")
                run = false
            }
            EventType.END_ROOM_TRANS -> {
                GameLogger.debug(TAG, "Room transition ended")
                val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (roomToShake == room) {
                    GameLogger.debug(TAG, "Shaking room: $roomToShake")
                    run = true
                    delayTimer.reset()
                }
            }
            EventType.SET_TO_ROOM_NO_TRANS -> {
                GameLogger.debug(TAG, "Setting to room without transition")
                val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (roomToShake == room) {
                    GameLogger.debug(TAG, "Shaking room: $roomToShake")
                    run = true
                    delayTimer.reset()
                } else {
                    GameLogger.debug(TAG, "Not shaking room: $roomToShake")
                    run = false
                }
            }
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!run) return@UpdatablesComponent

        delayTimer.update(delta)
        if (delayTimer.isFinished()) {
            GameLogger.debug(TAG, "Shaking room")

            game.eventsMan.submitEvent(
                Event(
                    EventType.SHAKE_CAM, props(
                        ConstKeys.INTERVAL to interval,
                        ConstKeys.DURATION to duration,
                        ConstKeys.X to x,
                        ConstKeys.Y to y
                    )
                )
            )

            sound?.let { requestToPlaySound(sound, false) }

            delayTimer.reset()
        }
    })
}