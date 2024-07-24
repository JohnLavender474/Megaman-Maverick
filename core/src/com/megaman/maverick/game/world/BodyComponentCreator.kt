package com.megaman.maverick.game.world

import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.megaman.maverick.game.ConstKeys

object BodyComponentCreator {

    fun create(entity: IBodyEntity, body: Body): BodyComponent {
        body.fixtures.forEach { (_, fixture) -> fixture.setEntity(entity) }
        body.setEntity(entity)
        body.preProcess.put(ConstKeys.DELTA) {
            body.putProperty(ConstKeys.PRIOR, body.getPosition().cpy())
        }
        return BodyComponent(entity, body)
    }
}
