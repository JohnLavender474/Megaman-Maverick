package com.maverick.game.engine.entities.contracts

import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent

interface IBodyEntity : IGameEntity {

    val bodyComponent: BodyComponent
        get() {
            val key = BodyComponent::class
            return getComponent(key)!!
        }

    val body: Body
        get() = bodyComponent.body
}
