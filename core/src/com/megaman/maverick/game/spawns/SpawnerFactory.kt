package com.megaman.maverick.game.spawns

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.GameLogger
import com.engine.common.shapes.GameRectangle
import com.engine.spawns.Spawn
import com.engine.spawns.SpawnerForBoundsEntered
import com.engine.spawns.SpawnerForEvent
import com.megaman.maverick.game.utils.toGameRectangle

/** A factory for creating spawners. */
object SpawnerFactory {

  const val TAG = "SpawnerFactory"

  /**
   * Creates a spawner that spawns an entity when the camera enters the spawn bounds.
   *
   * @return a spawner that spawns an entity when the camera enters the spawn bounds
   */
  fun spawnerForWhenEnteringCamera(
      camera: Camera,
      spawnBounds: GameRectangle,
      spawnSupplier: () -> Spawn
  ): SpawnerForBoundsEntered {
    GameLogger.debug(TAG, "spawnerForWhenEnteringCamera(): Creating spawner for camera: $camera")
    return SpawnerForBoundsEntered(spawnSupplier, { spawnBounds }, { camera.toGameRectangle() })
  }

  /**
   * Creates a spawner that spawns an entity when an event is called.
   *
   * @return a spawner that spawns an entity when an event is called.
   */
  fun spawnerForWhenEventCalled(
      events: ObjectSet<Any>,
      spawnSupplier: () -> Spawn
  ): SpawnerForEvent {
    GameLogger.debug(TAG, "spawnerForWhenEventCalled(): Creating spawner for events: $events")
    return SpawnerForEvent({ events.contains(it.key) }, spawnSupplier, eventKeyMask = events)
  }
}
