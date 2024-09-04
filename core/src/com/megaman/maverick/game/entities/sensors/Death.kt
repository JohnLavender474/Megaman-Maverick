package com.megaman.maverick.game.entities.sensors

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.Body
import com.mega.game.engine.world.BodyComponent
import com.mega.game.engine.world.BodyType
import com.mega.game.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Death(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity {

    override fun getEntityType() = EntityType.SENSOR

    override fun init() = addComponent(defineBodyComponent())

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
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
