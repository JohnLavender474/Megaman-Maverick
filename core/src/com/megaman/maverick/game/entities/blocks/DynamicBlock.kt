package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.math.Vector2
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IDrawableShapesEntity
import com.engine.entities.contracts.IUpdatableEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

open class DynamicBlock(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IUpdatableEntity,
    IDrawableShapesEntity {

    companion object {
        const val TAG = "DynamicBlock"
    }

    private var staticInnerBlock: Block? = null

    override fun init() {
        super<MegaGameEntity>.init()
        addComponent(DrawableShapesComponent(debug = true))
        addComponent(defineUpdatablesComponennt())
        addComponent(defineBodyComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        staticInnerBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD) as Block
        val staticInnerBlockProps = props(ConstKeys.BOUNDS to bounds)
        if (spawnProps.containsKey(ConstKeys.BODY_LABELS)) staticInnerBlockProps.put(
            ConstKeys.BODY_LABELS,
            spawnProps.get(ConstKeys.BODY_LABELS)
        )
        if (spawnProps.containsKey(ConstKeys.FIXTURE_LABELS)) staticInnerBlockProps.put(
            ConstKeys.FIXTURE_LABELS,
            spawnProps.get(ConstKeys.FIXTURE_LABELS)
        )
        if (spawnProps.containsKey(ConstKeys.FRICTION_X)) staticInnerBlockProps.put(
            ConstKeys.FRICTION_X,
            spawnProps.get(ConstKeys.FRICTION_X)
        )
        if (spawnProps.containsKey(ConstKeys.FRICTION_Y)) staticInnerBlockProps.put(
            ConstKeys.FRICTION_Y,
            spawnProps.get(ConstKeys.FRICTION_Y)
        )
        game.engine.spawn(staticInnerBlock!!, staticInnerBlockProps)

        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2(), Vector2::class)
        body.physics.gravity.set(gravity)
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        staticInnerBlock?.kill()
        staticInnerBlock = null
    }

    protected open fun defineUpdatablesComponennt() = UpdatablesComponent({
        staticInnerBlock!!.body.setCenter(body.getCenter())
    })

    protected open fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        addDebugShapeSupplier { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            (bodyFixture.rawShape as GameRectangle).set(body)
        }

        return BodyComponentCreator.create(this, body)
    }
}