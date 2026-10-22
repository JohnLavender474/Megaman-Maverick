package com.maverick.game.engine.entities.contracts

import com.maverick.game.engine.common.interfaces.Updatable
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.updatables.UpdatablesComponent

interface IUpdatableEntity : IGameEntity {

    val updatablesComponent: UpdatablesComponent
        get() {
            val key = UpdatablesComponent::class
            return getComponent(key)!!
        }

    fun putUpdatable(key: Any, updatable: Updatable) = updatablesComponent.updatables.put(key, updatable)
}