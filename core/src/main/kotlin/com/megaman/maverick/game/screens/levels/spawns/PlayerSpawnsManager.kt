package com.megaman.maverick.game.screens.levels.spawns

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.overlaps
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toProps

class PlayerSpawnsManager(
    private val camera: Camera,
    private val onChangeSpawn: ((RectangleMapObject?, RectangleMapObject?) -> Unit)? = null
) : Runnable, Resettable {

    val currentSpawnProps: Properties?
        get() = current?.let get@{
            val props = it.toProps()
            props.put(ConstKeys.BOUNDS, current?.rectangle?.toGameRectangle())
            return@get props
        }

    private val spawns = Array<RectangleMapObject>()
    private var current: RectangleMapObject? = null

    private val reusableArray = Array<RectangleMapObject>()

    fun set(spawns: Array<RectangleMapObject>) {
        this.spawns.addAll(spawns)
        current = popFirstSpawn()
    }

    private fun popFirstSpawn(): RectangleMapObject {
        reusableArray.clear()
        reusableArray.addAll(spawns)
        reusableArray.sort { o1, o2 -> o1.name.compareTo(o2.name) }

        val spawn = reusableArray[0]
        spawns.removeValue(spawn, false)
        return spawn
    }

    private fun popIfInCamera(): RectangleMapObject? {
        val iter = spawns.iterator()
        while (iter.hasNext()) {
            val spawn = iter.next()
            if (shouldPop(spawn)) {
                iter.remove()
                return spawn
            }
        }
        return null
    }

    private fun shouldPop(spawn: RectangleMapObject) = camera.overlaps(
        spawn.rectangle.toGameRectangle(), GameObjectPools.fetch(BoundingBox::class)
    )

    override fun run() {
        if (spawns.isEmpty) return

        val new = popIfInCamera()
        if (new != null && new != current) {
            val old = current
            onChangeSpawn?.invoke(new, old)
            current = new
        }
    }

    override fun reset() {
        spawns.clear()
        current = null
    }

    fun remove(name: String) {
        val iter = spawns.iterator()
        while (iter.hasNext()) {
            val spawn = iter.next()
            if (spawn.name == name) iter.remove()
        }
    }
}
