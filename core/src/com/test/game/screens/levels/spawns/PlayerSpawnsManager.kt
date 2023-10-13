package com.test.game.screens.levels.spawns

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.common.extensions.overlaps
import com.engine.common.interfaces.Resettable
import java.util.*

class PlayerSpawnsManager(private val camera: Camera) : Runnable, Resettable {

  private var current: RectangleMapObject? = null
  private var spawns =
      PriorityQueue(Comparator.comparing { p: RectangleMapObject -> p.name.toInt() })

  fun getCurrentSpawn() = current

  fun set(spawns: Iterable<RectangleMapObject>) {
    this.spawns.addAll(spawns)
    current = this.spawns.poll()
  }

  override fun run() {
    if (spawns.isEmpty()) return
    current?.let {
      if (camera.overlaps(it.rectangle)) {
        spawns.add(it)
      }
    }
  }

  override fun reset() {
    spawns.clear()
    current = null
  }
}
