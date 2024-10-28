package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.shapes.IGameShape2D
import com.megaman.maverick.game.screens.levels.camera.RotatableCamera

object SpawnerFactory {

    const val TAG = "SpawnerFactory"

    fun spawnerForWhenInCamera(
        camera: RotatableCamera,
        spawnShape: IGameShape2D,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true,
        continueCheckingAfterOverlap: Boolean = false
    ): SpawnerForBoundsEntered {
        GameLogger.debug(TAG, "spawnerForWhenEnteringCamera(): Creating spawner for camera: $camera")
        return SpawnerForBoundsEntered(
            spawnSupplier,
            { spawnShape },
            { camera.getRotatedBounds() },
            respawnable = respawnable,
            continueCheckingAfterOverlap = continueCheckingAfterOverlap
        )
    }

    fun spawnerForWhenEventCalled(
        events: ObjectSet<Any>,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true
    ): SpawnerForEvent {
        GameLogger.debug(TAG, "spawnerForWhenEventCalled(): Creating spawner for events: $events")
        return SpawnerForEvent(
            { events.contains(it.key) }, spawnSupplier, eventKeyMask = events, respawnable = respawnable
        )
    }
}
