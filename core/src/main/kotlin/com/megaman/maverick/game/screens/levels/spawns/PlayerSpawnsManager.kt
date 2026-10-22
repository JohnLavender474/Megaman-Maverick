package com.megaman.maverick.game.screens.levels.spawns

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
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

    fun startAt(spawn: RectangleMapObject) {
        // TODO: every spawn whose name sorts before the start spawn counts as passed. This uses the
        //  string order of the names ("10" sorts before "2"), not the checkpoint ranks planned in item
        //  036, and it drops forked checkpoints that share a rank with the start spawn. Replace it with
        //  the rank rule when item 036 changes how checkpoints activate.
        val iter = spawns.iterator()
        while (iter.hasNext()) {
            val other = iter.next()
            if (other == spawn || other.name < spawn.name) iter.remove()
        }
        current = spawn
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

    private fun shouldPop(spawn: RectangleMapObject) = camera.toGameRectangle().overlaps(
        spawn.rectangle.toGameRectangle()
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
