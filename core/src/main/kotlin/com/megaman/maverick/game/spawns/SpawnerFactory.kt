package com.megaman.maverick.game.spawns

import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.events.Event

object SpawnerFactory {

    const val TAG = "SpawnerFactory"

    fun spawnerForWhenInCamera(
        cameraBounds: GameRectangle,
        spawnShape: IGameShape2D,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true,
        shouldTest: (Float) -> Boolean = { true },
        rearmMargin: Float = 0f
    ): SpawnerForBoundsEntered {
        GameLogger.debug(TAG, "spawnerForWhenEnteringCamera(): creating spawner for cameraBounds=$cameraBounds")

        val rearmBounds = GameRectangle()
        return SpawnerForBoundsEntered(
            spawnSupplier = spawnSupplier,
            thisBounds = { spawnShape },
            otherBounds = { cameraBounds },
            respawnable = respawnable,
            shouldTestPred = shouldTest,
            rearmBounds = {
                rearmBounds.set(
                    cameraBounds.getX() - rearmMargin,
                    cameraBounds.getY() - rearmMargin,
                    cameraBounds.getWidth() + 2f * rearmMargin,
                    cameraBounds.getHeight() + 2f * rearmMargin
                )
            },
            rearmsOnLevelReset = rearmMargin > 0f
        )
    }

    fun spawnerForWhenEventCalled(
        events: ObjectSet<Any>,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true,
        shouldTest: (Float) -> Boolean = { true }
    ): SpawnerForEvent {
        GameLogger.debug(TAG, "spawnerForWhenEventCalled(): creating spawner for events=$events")

        return SpawnerForEvent(
            predicate = { events.contains(it.key) },
            spawnSupplier = spawnSupplier,
            eventKeyMask = events,
            respawnable = respawnable,
            shouldTestPred = shouldTest
        )
    }

    fun spawnerForOnEvent(
        predicate: (Event) -> Boolean,
        eventKeyMask: ObjectSet<Any>,
        spawnSupplier: () -> Spawn,
        respawnable: Boolean = true,
        shouldTest: (Float) -> Boolean = { true }
    ): SpawnerForEvent {
        GameLogger.debug(TAG, "spawnerForOnEvent(): creating spawner for eventKeyMask=$eventKeyMask")

        return SpawnerForEvent(
            predicate = { predicate.invoke(it) },
            spawnSupplier = spawnSupplier,
            eventKeyMask = eventKeyMask,
            respawnable = respawnable,
            shouldTestPred = shouldTest
        )
    }
}
