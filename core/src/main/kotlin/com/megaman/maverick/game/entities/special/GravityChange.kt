package com.megaman.maverick.game.entities.special

import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class GravityChange(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity {

    private lateinit var gravityChangeFixture: Fixture

    override fun getType() = EntityType.SPECIAL

    override fun init() {
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

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
        gravityChangeFixture = Fixture(body, FixtureType.GRAVITY_CHANGE, GameRectangle())
        body.addFixture(gravityChangeFixture)
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun createCullablesComponent(): CullablesComponent {
        val cullOnOutOfBounds = getGameCameraCullingLogic(this)
        return CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo cullOnOutOfBounds))
    }
}
