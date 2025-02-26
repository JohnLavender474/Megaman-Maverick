package com.megaman.maverick.game.entities.sensors

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

open class Death(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity {

    companion object {
        const val TAG = "Death"
    }

    override fun init() = addComponent(defineBodyComponent())

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        val instant = spawnProps.getOrDefault(ConstKeys.INSTANT, false, Boolean::class)
        body.forEachFixture { fixture ->
            fixture as Fixture

            (fixture.rawShape as GameRectangle).set(bounds)

            if (fixture.getType() == FixtureType.DEATH) fixture.putProperty(ConstKeys.INSTANT, instant)
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val deathFixture = Fixture(body, FixtureType.DEATH, GameRectangle())
        body.addFixture(deathFixture)
        return BodyComponentCreator.create(this, body)
    }

    override fun getType() = EntityType.SENSOR

    override fun getTag() = TAG
}
