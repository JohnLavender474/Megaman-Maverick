package com.megaman.maverick.game.world

import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.engine.world.BodyComponent

object BodyComponentCreator {

  fun create(entity: IBodyEntity, body: Body): BodyComponent {
    body.fixtures.forEach { (_, fixture) -> fixture.setEntity(entity) }
    return BodyComponent(entity, body)
  }
}
