package com.megaman.maverick.game.screens.levels.spawns

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.common.extensions.overlaps
import com.engine.common.interfaces.Resettable
import java.util.*

/**
 * This class is responsible for managing the player spawns. The [RectangleMapObject]s should have a
 * name that is [Comparable] such as [Int] or [String]. The [PlayerSpawnsManager] will use ascending
 * order of the objects' names to determine which spawn to use next. The [PlayerSpawnsManager] will
 * use the [Camera] to determine when the next spawn should be used. When the [Camera] overlaps the
 * next spawn, the [current] spawn will be setBodySense to the next spawn. If there are no more
 * spawns, the [current] spawn will not be changed. The [set] method should be called with a new
 * collection of [RectangleMapObject]s each time a new tiled map is built.
 *
 * @param camera the [Camera] to use.
 */
class PlayerSpawnsManager(private val camera: Camera) : Runnable, Resettable {

  var current: RectangleMapObject? = null
    private set

  private var spawns = PriorityQueue(Comparator.comparing { p: RectangleMapObject -> p.name })

  /**
   * Sets the [spawns] to use.
   *
   * @param _spawns the [Iterable] of [RectangleMapObject]s to use.
   */
  fun set(_spawns: Iterable<RectangleMapObject>) {
    spawns.addAll(_spawns)
    current = spawns.poll()
  }

  /**
   * Checks if the camera is overlapping the next spawn, and if so sets the current spawn to the
   * next. If there are no more spawns, this method does nothing.
   */
  override fun run() {
    if (spawns.isEmpty()) return

    spawns.peek()?.let { if (camera.overlaps(it.rectangle)) current = spawns.poll() }
  }

  /** Clears the [spawns] and sets the [current] to null. */
  override fun reset() {
    spawns.clear()
    current = null
  }
}
