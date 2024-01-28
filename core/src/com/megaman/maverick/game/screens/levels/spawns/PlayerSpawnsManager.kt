package com.megaman.maverick.game.screens.levels.spawns

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.engine.common.extensions.overlaps
import com.engine.common.interfaces.Resettable
import com.engine.common.objects.Properties
import com.engine.common.shapes.toGameRectangle
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.utils.toProps
import java.util.*

class PlayerSpawnsManager(private val camera: Camera) : Runnable, Resettable {

  val currentSpawnProps: Properties?
    get() =
        current?.let {
          val props = it.toProps()
          props.put(ConstKeys.BOUNDS, current?.rectangle?.toGameRectangle())
          props
        }

  private var current: RectangleMapObject? = null
  private var spawns = PriorityQueue(Comparator.comparing { p: RectangleMapObject -> p.name })

  fun set(_spawns: Iterable<RectangleMapObject>) {
    spawns.addAll(_spawns)
    current = spawns.poll()
  }

  override fun run() {
    if (spawns.isEmpty()) return

    spawns.peek()?.let { if (camera.overlaps(it.rectangle)) current = spawns.poll() }
  }

  override fun reset() {
    spawns.clear()
    current = null
  }
}
