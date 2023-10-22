package com.megaman.maverick.game.spawns

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.utils.ObjectSet
import com.engine.common.shapes.GameRectangle
import com.engine.spawns.Spawn
import com.engine.spawns.SpawnerForBoundsEntered
import com.engine.spawns.SpawnerForEvent
import com.megaman.maverick.game.utils.toGameRectangle

/** A factory for creating spawners. */
object SpawnerFactory {

    /**
   * Creates a spawner that spawns an entity when the camera enters the spawn bounds.
   *
   * @return a spawner that spawns an entity when the camera enters the spawn bounds
   */
  fun spawnerForWhenEnteringCamera(
      camera: Camera,
      spawnBounds: GameRectangle,
      spawnSupplier: () -> Spawn
  ) = SpawnerForBoundsEntered(spawnSupplier, { spawnBounds }, { camera.toGameRectangle() })

  /**
   * Creates a spawner that spawns an entity when an event is called.
   *
   * @return a spawner that spawns an entity when an event is called.
   */
  fun spawnerForWhenEventCalled(events: ObjectSet<Any>, spawnSupplier: () -> Spawn) =
      SpawnerForEvent({ events.contains(it.key) }, spawnSupplier, eventKeyMask = events)
}
