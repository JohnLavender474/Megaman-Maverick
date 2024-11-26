package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.events.Event
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera

object SpawnerFactory {

    const val TAG = "SpawnerFactory"

    fun spawnerForWhenInCamera(
        camera: RotatableCamera,
        spawnShape: IGameShape2D,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true
    ): SpawnerForBoundsEntered {
        GameLogger.debug(TAG, "spawnerForWhenEnteringCamera(): creating spawner for camera=$camera")
        return SpawnerForBoundsEntered(
            spawnSupplier,
            { spawnShape },
            { camera.getRotatedBounds() },
            respawnable = respawnable
        )
    }

    fun spawnerForWhenEventCalled(
        events: ObjectSet<Any>,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true
    ): SpawnerForEvent {
        GameLogger.debug(TAG, "spawnerForWhenEventCalled(): creating spawner for events=$events")
        return SpawnerForEvent(
            { events.contains(it.key) }, spawnSupplier, eventKeyMask = events, respawnable = respawnable
        )
    }

    fun spawnerForOnEvent(
        predicate: (Event) -> Boolean,
        eventKeyMask: ObjectSet<Any>,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true
    ): SpawnerForEvent {
        GameLogger.debug(TAG, "spawnerForOnEvent(): creating spawner for eventKeyMask=$eventKeyMask")
        return SpawnerForEvent(
            { predicate.invoke(it) }, spawnSupplier, eventKeyMask = eventKeyMask, respawnable = respawnable
        )
    }
}
