package com.mega.game.engine.entities.contracts

import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent

interface IBodyEntity : IGameEntity {

    val bodyComponent: BodyComponent
        get() {
            val key = BodyComponent::class
            return getComponent(key)!!
        }

    val body: Body
        get() = bodyComponent.body
}
