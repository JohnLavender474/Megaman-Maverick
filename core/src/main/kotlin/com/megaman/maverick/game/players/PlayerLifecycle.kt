package com.megaman.maverick.game.players

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.components.require
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.isBehaviorActive
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.locks.clearLocks
import com.maverick.game.engine.players.playerSlot
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.behaviors.BehaviorType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.entities.state.beginRoomTransitionCarry
import com.megaman.maverick.game.entities.state.endRoomTransitionCarry
import com.megaman.maverick.game.entities.state.stopMomentum
import com.megaman.maverick.game.entities.utils.isOutsideLevel
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.events.PlayerEventKeys
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.stopSmoothFollow

private const val TAG = "PlayerLifecycle"

private const val OUTSIDE_LEVEL_X_OFFSET = 1.5f
private const val OUTSIDE_LEVEL_Y_OFFSET = 1.5f

fun <T> T.playerSpawnStart(spawnProps: Properties)
    where T : AbstractHealthEntity, T : IBodyEntity, T : IFaceable, T : IEventListener {
    services.events.addListener(this)

    val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
    body.positionOnPoint(bounds.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)

    body.physics.velocity.setZero()
    body.physics.collisionOn = true
    body.physics.gravityOn = true
    body.forEachFixture { fixture -> fixture.setActive(true) }

    facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, ConstKeys.RIGHT, String::class).uppercase())
    services.gravity.set(
        Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())
    )

    setHealth(getMaxHealth())

    clearLocks(MegaLockGroup.MOVEMENT)
    require(TeleportableComponent::class).teleporter = null
    clearLocks(MegaLockGroup.DAMAGE)
}

fun MegaGameEntity.playerSpawnEnd() {
    services.cameraController.stopSmoothFollow()

    // TODO: only slot 0 submits this event, because its listeners use it as a level reset, which must run once; a
    //  later slot has no spawn event until item 038 splits it into a spawn event per player and a level reset
    if (playerSlot!!.index == 0)
        services.events.submitEvent(Event(EventType.PLAYER_SPAWN, props(PlayerEventKeys.SLOT pairTo playerSlot!!)))
}

// TODO: this event fires on each destroy of a player, also when it is not a death (item 119)
fun <T> T.playerOnDestroy() where T : MegaGameEntity, T : IEventListener {
    val events = services.events
    events.removeListener(this)
    events.submitEvent(Event(EventType.PLAYER_JUST_DIED, props(PlayerEventKeys.SLOT pairTo playerSlot!!)))
}

fun IBodyEntity.playerOnRoomTransition(event: Event) {
    when (event.key) {
        EventType.BEGIN_ROOM_TRANS, EventType.CONTINUE_ROOM_TRANS -> {
            val position = event.getProperty(ConstKeys.POSITION, Vector2::class)!!
            if (event.key == EventType.BEGIN_ROOM_TRANS)
                GameLogger.debug(TAG, "playerOnRoomTransition(): begin: position=$position")
            body.setCenter(position)

            if (event.key == EventType.BEGIN_ROOM_TRANS) beginRoomTransitionCarry()

            body.physics.gravityOn = false

            stopMomentum(true)
        }

        EventType.END_ROOM_TRANS -> {
            GameLogger.debug(TAG, "playerOnRoomTransition(): end")

            endRoomTransitionCarry()

            body.physics.gravityOn = !isBehaviorActive(BehaviorType.CLIMBING)

            stopMomentum(true)
        }

        else -> throw IllegalArgumentException("${event.key} is not a room transition event")
    }
}

fun <T> T.killPlayerIfOutsideLevel() where T : MegaGameEntity, T : IBodyEntity {
    if (isOutsideLevel(services.levelState, OUTSIDE_LEVEL_X_OFFSET, OUTSIDE_LEVEL_Y_OFFSET)) {
        GameLogger.error(TAG, "The player is outside the level bounds, kill it: $this")
        destroy()
    }
}
