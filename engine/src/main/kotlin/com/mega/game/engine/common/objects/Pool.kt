package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IdentityMap
import com.mega.game.engine.common.interfaces.Initializable

open class Pool<T : Any>(
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

    // Tracks which elements are currently sitting in the queue, so that free() can reject a double-free. Membership
    // must be by *identity*: value-based equality would reject two distinct-but-equal elements, and would also break
    // for mutable elements whose hash changes while they are pooled.
    protected open val freeSet = IdentityMap<T, Boolean>()

    override fun init(vararg params: Any) {
        (0 until startAmount).forEach { _ -> free(supplyNew()) }
        initialized = true
    }

    open fun fetch(): T {
        if (!initialized) init()

        val element = if (queue.isEmpty) supplyNew() else queue.pop()

        // must be removed before onFetch is invoked: onFetch typically resets the element, and removal by identity
        // stays correct either way, but keeping this first preserves the invariant "not in queue => not in freeSet"
        freeSet.remove(element)

        onFetch?.invoke(element)

        return element
    }

    open fun free(element: T): Boolean {
        if (freeSet.containsKey(element)) return false

        queue.add(element)
        freeSet.put(element, true)

        onFree?.invoke(element)

        return true
    }

    open fun clear() {
        queue.clear()
        freeSet.clear()
    }

    protected open fun supplyNew(): T {
        val element = supplier()
        onSupplyNew?.invoke(element)
        return element
    }
}
