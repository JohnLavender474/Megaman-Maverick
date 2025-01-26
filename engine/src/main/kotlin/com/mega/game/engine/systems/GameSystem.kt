package com.mega.game.engine.systems

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.ImmutableCollection
import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.components.IGameComponent
import com.mega.game.engine.entities.IGameEntity
import kotlin.reflect.KClass

abstract class GameSystem(
    private val componentMask: ObjectSet<KClass<out IGameComponent>>,
    private val entities: MutableOrderedSet<IGameEntity> = MutableOrderedSet()
) : Updatable, Resettable, Disposable {

    companion object {
        const val TAG = "GameSystem"

        private fun buildComponentMaskFromVarArgs(
            componentMask: kotlin.Array<out KClass<out IGameComponent>>
        ): ObjectSet<KClass<out IGameComponent>> {
            val set = ObjectSet<KClass<out IGameComponent>>()
            componentMask.forEach { set.add(it) }
            return set
        }
    }

    private val entitiesToAdd = Array<IGameEntity>()
    private val entitiesToRemove = Array<IGameEntity>()

    protected val disposables = Array<Disposable>()

    var on = true
    var updating = false
        private set

    constructor(vararg componentMask: KClass<out IGameComponent>) : this(buildComponentMaskFromVarArgs(componentMask))

    internal abstract fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float)

    fun contains(e: IGameEntity) = entities.contains(e)

    fun remove(e: IGameEntity) = if (contains(e)) {
        if (updating) {
            entitiesToRemove.add(e)
            2
        } else {
            entities.remove(e)
            1
        }
    } else 0

    fun add(e: IGameEntity): Int = if (qualifies(e)) {
        if (updating) {
            entitiesToAdd.add(e)
            2
        } else {
            entities.add(e)
            1
        }
    } else 0

    fun qualifies(e: IGameEntity) = componentMask.all { e.hasComponent(it) }

    final override fun update(delta: Float) {
        updating = true

        entities.addAll(entitiesToAdd)
        entitiesToAdd.clear()

        entities.removeAll(entitiesToRemove)
        entitiesToRemove.clear()

        entities.removeAll { !qualifies(it) }
        process(on, ImmutableCollection(entities), delta)

        updating = false
    }

    override fun reset() {
        entities.clear()
        entitiesToAdd.clear()
    }

    override fun dispose() = disposables.forEach { it.dispose() }
}
