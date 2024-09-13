package com.megaman.maverick.game.world.body

import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys

object BodyComponentCreator {

    fun create(entity: IBodyEntity, body: Body): BodyComponent {
        body.fixtures.forEach { (_, fixture) -> fixture.setEntity(entity) }
        body.setEntity(entity)
        body.preProcess.put(ConstKeys.DELTA) { body.putProperty(ConstKeys.PRIOR, body.getPosition().cpy()) }
        body.onReset = { body.resetBodySenses() }
        return BodyComponent(body)
    }
}
