package com.megaman.maverick.game.entities.special

import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
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

class Ladder(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity {

    private lateinit var ladderRectangle: GameRectangle

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        ladderRectangle.set(bounds)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        ladderRectangle = GameRectangle()
        val ladderFixture = Fixture(body, FixtureType.LADDER, ladderRectangle)
        body.addFixture(ladderFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ ladderRectangle }), debug = true))

        return BodyComponentCreator.create(this, body)
    }
}
