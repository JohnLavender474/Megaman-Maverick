package com.megaman.maverick.game.screens.levels.spawns

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.mega.game.engine.common.extensions.overlaps
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.utils.LoopedSuppliers
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps
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

    fun set(spawns: Iterable<RectangleMapObject>) {
        this.spawns.addAll(spawns)
        current = this.spawns.poll()
    }

    override fun run() {
        if (spawns.isEmpty()) return
        spawns.peek()?.let {
            if (camera.overlaps(it.rectangle.toGameRectangle(), LoopedSuppliers.getBoundingBox()))
                current = spawns.poll()
        }
    }

    override fun reset() {
        spawns.clear()
        current = null
    }
}
