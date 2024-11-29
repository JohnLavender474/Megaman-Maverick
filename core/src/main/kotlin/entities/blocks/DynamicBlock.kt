package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.IUpdatableEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

open class DynamicBlock(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IUpdatableEntity,
    IDrawableShapesEntity {

    companion object {
        const val TAG = "DynamicBlock"
    }

    private var staticInnerBlock: Block? = null

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        addComponent(DrawableShapesComponent(debug = true))
        addComponent(defineUpdatablesComponennt())
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        staticInnerBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD) as Block
        val staticInnerBlockProps = props(ConstKeys.BOUNDS pairTo bounds)
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
        staticInnerBlock!!.spawn(staticInnerBlockProps)

        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2(), Vector2::class)
        body.physics.gravity.set(gravity)
    }

    override fun onDestroy() {
        super.onDestroy()
        staticInnerBlock?.destroy()
        staticInnerBlock = null
    }

    protected open fun defineUpdatablesComponennt() = UpdatablesComponent({
        staticInnerBlock!!.body.setCenter(body.getCenter())
    })

    protected open fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        addDebugShapeSupplier { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            (bodyFixture.getShape() as GameRectangle).set(body)
        }

        return BodyComponentCreator.create(this, body)
    }
}
