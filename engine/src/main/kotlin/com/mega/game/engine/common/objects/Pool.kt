package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.interfaces.Initializable

open class Pool<T>(
    var supplier: () -> T,
    private var startAmount: Int = 10,
    var onSupplyNew: ((T) -> Unit)? = null,
    var onFetch: ((T) -> Unit)? = null,
    var onFree: ((T) -> Unit)? = null
) : Initializable {

    companion object {
        const val TAG = "Pool"
    }

    protected open var initialized = false
    protected open val queue = Array<T>()

    override fun init() {
        (0 until startAmount).forEach { free(supplyNew()) }
        initialized = true
    }

    open fun fetch(): T {
        if (!initialized) init()
        val element = if (queue.isEmpty) supplyNew() else queue.pop()
        onFetch?.invoke(element)
        return element
    }

    open fun free(element: T) {
        queue.add(element)
        onFree?.invoke(element)
    }

    open fun clear() = queue.clear()

    protected open fun supplyNew(): T {
        val element = supplier()
        onSupplyNew?.invoke(element)
        return element
    }
}
