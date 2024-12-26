package com.mega.game.engine.entities.contracts

import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.motion.IMotion
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition

interface IMotionEntity : IGameEntity {

    val motionComponent: MotionComponent
        get() {
            val key = MotionComponent::class
            return getComponent(key)!!
        }

    val motionDefinitions: OrderedMap<Any, MotionDefinition>
        get() = this.motionComponent.definitions

    fun getMotionDefinition(key: Any): MotionDefinition? {
        return this.motionDefinitions.get(key)
    }

    fun getMotion(key: Any): IMotion? {
        val definition = getMotionDefinition(key)
        return definition?.motion
    }

    fun putMotionDefinition(key: Any, definition: MotionDefinition): MotionDefinition? {
        return this.motionComponent.put(key, definition)
    }

    fun removeMotionDefinition(key: Any): MotionDefinition? {
        return this.motionDefinitions.remove(key)
    }

    fun clearMotionDefinitions() {
        this.motionDefinitions.clear()
    }

    fun resetMotionComponent() {
        this.motionComponent.reset()
    }
}

