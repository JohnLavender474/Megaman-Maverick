package com.megaman.maverick.game.entities.special

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.players.registeredSlots
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.players
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.utils.misc.StunType

class TestStunPulser(game: MegaServices) : MegaGameEntity(game) {

    companion object {
        const val TAG = "TestStunPulser"
        private const val DEFAULT_INTERVAL = 3f
        private const val ROOM_SHAKE_DUR = 0.5f
        private const val ROOM_SHAKE_INTERVAL = 0.1f
        private const val ROOM_SHAKE_X = 0.001f
        private const val ROOM_SHAKE_Y = 0.005f
    }

    private val bounds = GameRectangle()
    private val timer = Timer(DEFAULT_INTERVAL)

    override fun getType() = EntityType.SPECIAL

    override fun init(vararg params: Any) {
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        timer.resetDuration(spawnProps.getOrDefault(ConstKeys.INTERVAL, DEFAULT_INTERVAL, Float::class))
    }

    private fun pulse() {
        GameLogger.debug(TAG, "pulse()")

        game.eventsMan.submitEvent(
            Event(
                EventType.STUN_PLAYER, props(
                    ConstKeys.TYPE pairTo StunType.STUN_BOUNCE_IF_ON_SURFACE,
                    PlayerEventKeys.SLOTS pairTo players.registeredSlots()
                )
            )
        )

        game.eventsMan.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.INTERVAL pairTo ROOM_SHAKE_INTERVAL,
                    ConstKeys.DURATION pairTo ROOM_SHAKE_DUR,
                    ConstKeys.X pairTo ROOM_SHAKE_X * ConstVals.PPM,
                    ConstKeys.Y pairTo ROOM_SHAKE_Y * ConstVals.PPM
                )
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!game.getGameCamera().getRotatedBounds().overlaps(bounds)) {
            timer.reset()
            return@UpdatablesComponent
        }

        timer.update(delta)
        if (timer.isFinished()) {
            pulse()
            timer.reset()
        }
    })
}
