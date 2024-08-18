package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.engine.common.enums.Direction
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class GravityChange(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity {

    private lateinit var gravityChangeFixture: Fixture

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        addComponent(defineBodyComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        (gravityChangeFixture.rawShape as GameRectangle).set(bounds)

        if (spawnProps.containsKey(ConstKeys.GRAVITY)) {
            val gravityScalar = spawnProps.getOrDefault(ConstKeys.GRAVITY, 1f, Float::class)
            setGravityScalar(gravityScalar)
        } else removeGravityScalar()

        if (spawnProps.containsKey(ConstKeys.DIRECTION)) {
            spawnProps.get(ConstKeys.DIRECTION).let { direction ->
                if (direction is String)
                    setGravityDirection(Direction.valueOf(direction.uppercase()))
                else if (direction is Direction)
                    setGravityDirection(direction)
            }
        } else removeGravityDirection()

        val cull = spawnProps.getOrDefault(ConstKeys.CULL, true, Boolean::class)
        if (cull) addComponent(createCullablesComponent()) else removeComponent(CullablesComponent::class)
    }

    private fun setGravityScalar(scalar: Float) {
        gravityChangeFixture.putProperty(ConstKeys.GRAVITY, scalar)
    }

    private fun removeGravityScalar() {
        gravityChangeFixture.removeProperty(ConstKeys.GRAVITY)
    }

    private fun setGravityDirection(direction: Direction) {
        gravityChangeFixture.putProperty(ConstKeys.DIRECTION, direction)
    }

    private fun removeGravityDirection() {
        gravityChangeFixture.removeProperty(ConstKeys.DIRECTION)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.color = Color.BROWN
        gravityChangeFixture = Fixture(body, FixtureType.GRAVITY_CHANGE, GameRectangle())
        body.addFixture(gravityChangeFixture)
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun createCullablesComponent(): CullablesComponent {
        val cullOnOutOfBounds = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS to cullOnOutOfBounds))
    }
}
