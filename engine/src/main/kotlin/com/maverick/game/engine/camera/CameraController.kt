package com.maverick.game.engine.camera

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.interfaces.Resettable
import com.maverick.game.engine.common.interfaces.Updatable
import kotlin.reflect.KClass

class CameraController<C : Camera>(val camera: C) : Updatable, Resettable {

    val settledPosition = Vector3()

    private val movers = Array<ICameraPart<C>>()
    private val effects = Array<ICameraPart<C>>()

    fun addMover(part: ICameraPart<C>) {
        movers.add(part)
    }

    fun addEffect(part: ICameraPart<C>) {
        effects.add(part)
    }

    fun <T : Any> getPart(type: KClass<T>): T? = find(movers, type) ?: find(effects, type)

    override fun update(delta: Float) {
        for (i in 0 until movers.size) if (movers[i].update(camera, delta)) break
        settledPosition.set(camera.position)
        for (i in 0 until effects.size) effects[i].update(camera, delta)
    }

    override fun reset() {
        reset(movers)
        reset(effects)
    }

    private fun reset(parts: Array<ICameraPart<C>>) {
        for (i in 0 until parts.size) (parts[i] as? Resettable)?.reset()
    }

    private fun <T : Any> find(parts: Array<ICameraPart<C>>, type: KClass<T>): T? {
        for (i in 0 until parts.size) {
            val part = parts[i]
            if (type.isInstance(part)) @Suppress("UNCHECKED_CAST") return part as T
        }
        return null
    }
}
