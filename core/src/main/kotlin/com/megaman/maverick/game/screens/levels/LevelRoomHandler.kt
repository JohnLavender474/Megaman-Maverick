package com.megaman.maverick.game.screens.levels

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.camera.CameraRoomTransition
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.forEach
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.decorations.GravitySwitchAura
import com.megaman.maverick.game.entities.state.running
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.players.asPlayer
import com.maverick.game.engine.camera.CameraRoomBounds
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.getCenter

class LevelRoomHandler(
    private val services: MegaServices,
    private val roomBounds: CameraRoomBounds,
    private val transition: CameraRoomTransition,
    private val player: () -> IPlayer,
    private val setBehaviorsOn: (Boolean) -> Unit
) {

    companion object {
        const val TAG = "LevelRoomHandler"
        private const val FADE_OUT_MUSIC_DUR = 1f
    }

    init {
        roomBounds.onFirstUpdateAfterReset = {
            tryToPlayCurrentRoomMusic()
        }
        transition.onBegin = {
            GameLogger.debug(
                TAG,
                "begin transition logic for rooms manager: " +
                    "current=${roomBounds.currentGameRoom?.name}, " +
                    "prior=${roomBounds.priorGameRoom?.name}"
            )

            services.events.submitEvent(Event(EventType.TURN_CONTROLLER_OFF))

            val current = roomBounds.currentGameRoom!!
            if (current.properties.get(ConstKeys.FADE_OUT_MUSIC, Boolean::class.java) == true)
                services.audio.fadeOutMusic(FADE_OUT_MUSIC_DUR)
            val prior = roomBounds.priorGameRoom
            services.events.submitEvent(
                Event(
                    EventType.BEGIN_ROOM_TRANS, props(
                        ConstKeys.PRIOR pairTo prior,
                        ConstKeys.ROOM pairTo current,
                        ConstKeys.NAME pairTo current.name,
                        "${ConstKeys.PRIOR}_${ConstKeys.NAME}" pairTo prior?.name,
                        ConstKeys.POSITION pairTo transition.getTransitionInterpolation(GameObjectPools.fetch(Vector2::class)),
                    )
                )
            )

            MegaGameEntities.getOfType(EntityType.ENEMY).forEach { it.destroy() }
            MegaGameEntities.getOfType(EntityType.PROJECTILE).forEach {
                val shouldDestroy = it !is IOwnable<*> || it.owner.asPlayer == null
                if (shouldDestroy) it.destroy()
            }

            setBehaviorsOn(false)
            services.levelState.roomTransition = true

            tryToPlayCurrentRoomMusic()
        }
        transition.onContinue = {
            services.events.submitEvent(
                Event(
                    EventType.CONTINUE_ROOM_TRANS,
                    props(
                        ConstKeys.PRIOR pairTo roomBounds.priorGameRoom,
                        ConstKeys.ROOM pairTo roomBounds.currentGameRoom,
                        ConstKeys.POSITION pairTo transition.getTransitionInterpolation(GameObjectPools.fetch(Vector2::class))
                    )
                )
            )

            GameLogger.debug(
                TAG, "continue transition logic for rooms manager: " +
                    "transitionInterpolation=${transition.getTransitionInterpolation(GameObjectPools.fetch(Vector2::class))} " +
                    "player().body.getCenter=${player().body.getCenter()}"
            )

            services.levelState.roomTransition = true
        }
        transition.onEnd = {
            GameLogger.debug(
                TAG, "end transition logic for rooms manager: " +
                    "current=${roomBounds.currentGameRoom?.name}, " +
                    "prior=${roomBounds.priorGameRoom?.name}"
            )

            val currentRoom = roomBounds.currentGameRoom
            val hasEvent = currentRoom?.properties?.containsKey(ConstKeys.EVENT) == true
            val event = currentRoom?.properties?.get(ConstKeys.EVENT, String::class.java)

            services.events.submitEvent(
                Event(
                    EventType.END_ROOM_TRANS, props(
                        ConstKeys.ROOM pairTo roomBounds.currentGameRoom,
                        ConstKeys.PRIOR pairTo roomBounds.priorGameRoom,
                        ConstKeys.VELOCITY pairTo !(hasEvent && event == ConstKeys.BOSS)
                    )
                )
            )

            when {
                hasEvent -> {
                    val props = props(ConstKeys.ROOM pairTo currentRoom)
                    val roomEvent = when (event) {
                        ConstKeys.BOSS -> EventType.ENTER_BOSS_ROOM
                        ConstKeys.SUCCESS -> EventType.VICTORY_EVENT
                        else -> throw IllegalStateException("Unknown room event: $event")
                    }
                    services.events.submitEvent(Event(roomEvent, props))
                }

                else -> services.events.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
            }

            if (currentRoom?.properties?.containsKey("megaman_direction") == true) {
                val direction = Direction.valueOf(
                    currentRoom.properties.get("megaman_direction", String::class.java).uppercase()
                )
                if (direction != services.gravity.direction) {
                    services.gravity.set(direction)

                    val aura = MegaEntityFactory.fetch(GravitySwitchAura::class)!!
                    aura.spawn(props(ConstKeys.POSITION pairTo player().body.getCenter()))

                    services.audio.playSound(SoundAsset.LIFT_OFF_SOUND, false)
                }
            }

            setBehaviorsOn(true)
            services.levelState.roomTransition = false

            services.players.forEach { it.running = false }
        }
        roomBounds.onSetToRoomNoTrans = {
            GameLogger.debug(TAG, "on set to room no trans")
            services.events.submitEvent(
                Event(
                    EventType.SET_TO_ROOM_NO_TRANS,
                    props(ConstKeys.ROOM pairTo roomBounds.currentGameRoom)
                )
            )
            tryToPlayCurrentRoomMusic()
        }
    }

    private fun tryToPlayCurrentRoomMusic() {
        if (roomBounds.currentGameRoom?.properties?.containsKey(ConstKeys.MUSIC) == true) {
            val musicAsset = MusicAsset.valueOf(
                roomBounds
                    .currentGameRoom!!
                    .properties
                    .get(ConstKeys.MUSIC, String::class.java)
                    .uppercase()
            )
            if (musicAsset != services.audio.getCurrentMusicAsset()) musicAsset.let { services.audio.playMusic(it, it.loop) }
        }
    }
}
