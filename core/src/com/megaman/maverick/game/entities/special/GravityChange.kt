package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class GravityChange(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity {

    private lateinit var gravityChangeFixture: Fixture

    override fun init() {
        addComponent(defineBodyComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        (gravityChangeFixture.shape as GameRectangle).set(bounds)

        spawnProps.get(ConstKeys.DIRECTION).let { direction ->
            if (direction is String)
                setGravityDirection(Direction.valueOf(direction.uppercase()))
            else if (direction is Direction)
                setGravityDirection(direction)
        }

        removeComponent(CullablesComponent::class)
        val cull = spawnProps.getOrDefault(ConstKeys.CULL, true, Boolean::class)
        if (cull) addComponent(createCullablesComponent())
    }

    fun setGravityDirection(direction: Direction) {
        gravityChangeFixture.putProperty(ConstKeys.DIRECTION, direction)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()

        // gravity-change fixture
        gravityChangeFixture = Fixture(GameRectangle(), FixtureType.GRAVITY_CHANGE)
        body.addFixture(gravityChangeFixture)
        gravityChangeFixture.shape.color = Color.ORANGE
        debugShapes.add { gravityChangeFixture.shape }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun createCullablesComponent(): CullablesComponent {
        val cullOnOutOfBounds = getGameCameraCullingLogic(this)
        return CullablesComponent(this, gdxArrayOf(cullOnOutOfBounds))
    }
}
