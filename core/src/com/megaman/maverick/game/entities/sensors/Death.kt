package com.megaman.maverick.game.entities.sensors

import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Death(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity {

    override fun init() = addComponent(defineBodyComponent())

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
        body.set(bounds)
        val deathFixture = body.fixtures.first().second
        ((deathFixture as Fixture).rawShape as GameRectangle).set(bounds)
        val instant = spawnProps.getOrDefault(ConstKeys.INSTANT, false, Boolean::class)
        deathFixture.putProperty(ConstKeys.INSTANT, instant)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val deathFixture = Fixture(body, FixtureType.DEATH, GameRectangle())
        body.addFixture(deathFixture)
        return BodyComponentCreator.create(this, body)
    }
}
