package com.mega.game.engine.entities.contracts

import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.cullables.ICullable
import com.mega.game.engine.entities.IGameEntity

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
}

