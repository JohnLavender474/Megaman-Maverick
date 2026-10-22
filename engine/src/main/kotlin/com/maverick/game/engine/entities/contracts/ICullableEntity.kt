package com.maverick.game.engine.entities.contracts

import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.cullables.ICullable
import com.maverick.game.engine.entities.IGameEntity

interface ICullableEntity : IGameEntity {

    val cullablesComponent: CullablesComponent
        get() {
            val key = CullablesComponent::class
            return getComponent(key)!!
        }

    fun putCullable(key: String, cullable: ICullable) {
        this.cullablesComponent.put(key, cullable)
    }

    fun removeCullable(key: String) {
        this.cullablesComponent.remove(key)
    }

    fun getCullable(key: String) = cullablesComponent.cullables.get(key)
}

