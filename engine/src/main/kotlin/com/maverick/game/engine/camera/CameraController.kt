package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import kotlin.reflect.KClass

class CameraController(val camera: Camera) : Updatable, Resettable {

    val settledPosition = Vector3()

    private val movers = Array<ICameraPart>()
    private val effects = Array<ICameraPart>()
    private val orientation = CameraOrientation(camera)

    private var rotation: CameraRotation? = null

    fun addMover(part: ICameraPart) {
        movers.add(part)
    }

    fun addEffect(part: ICameraPart) {
        effects.add(part)
        if (part is CameraRotation) rotation = part
    }

    fun applyOrientation() {
        rotation?.let { orientation.write(it.angle) }
    }

    fun <T : Any> getPart(type: KClass<T>): T? = find(movers, type) ?: find(effects, type)

    override fun update(delta: Float) {
        for (i in 0 until movers.size) if (movers[i].update(camera, delta)) break
        settledPosition.set(camera.position)
        for (i in 0 until effects.size) effects[i].update(camera, delta)
        applyOrientation()
    }

    override fun reset() {
        reset(movers)
        reset(effects)
    }

    private fun reset(parts: Array<ICameraPart>) {
        for (i in 0 until parts.size) (parts[i] as? Resettable)?.reset()
    }

    private fun <T : Any> find(parts: Array<ICameraPart>, type: KClass<T>): T? {
        for (i in 0 until parts.size) {
            val part = parts[i]
            if (type.isInstance(part)) @Suppress("UNCHECKED_CAST") return part as T
        }
        return null
    }
}
