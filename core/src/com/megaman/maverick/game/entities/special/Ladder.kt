package com.megaman.maverick.game.entities.special

import com.engine.common.extensions.gdxArrayOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
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

class Ladder(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity {

    private lateinit var ladderRectangle: GameRectangle

    override fun init() {
        addComponent(defineBodyComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        ladderRectangle.set(bounds)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        ladderRectangle = GameRectangle()
        val ladderFixture = Fixture(body, FixtureType.LADDER, ladderRectangle)
        body.addFixture(ladderFixture)

        addComponent(
            DrawableShapesComponent(
                this, debugShapeSuppliers = gdxArrayOf({ ladderRectangle }), debug = true
            )
        )

        return BodyComponentCreator.create(this, body)
    }
}
