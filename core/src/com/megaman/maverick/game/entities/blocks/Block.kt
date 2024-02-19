package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.IGame2D
import com.engine.common.GameLogger
import com.engine.common.interfaces.Updatable
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
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.*

open class Block(game: IGame2D) : GameEntity(game), IBodyEntity {

    companion object {
        const val TAG = "Block"
        const val STANDARD_FRICTION_X = 0.035f
        const val STANDARD_FRICTION_Y = 0f
    }

    protected lateinit var blockFixture: Fixture
        private set
    protected val debugShapeSuppliers = Array<() -> IDrawableShape?>()

    override fun init() {
        GameLogger.debug(TAG, "init(): Initializing Block entity.")
        addComponent(defineBodyComponent())
        debugShapeSuppliers.add { body }
        addComponent(
            DrawableShapesComponent(this, debugShapeSuppliers = debugShapeSuppliers, debug = true)
        )
    }

    protected open fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)

        // block fixture
        blockFixture = Fixture(GameRectangle(), FixtureType.BLOCK)
        body.addFixture(blockFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable { (blockFixture.shape as GameRectangle).set(body) })

        return BodyComponentCreator.create(this, body)
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val persist =
            if (spawnProps.containsKey(ConstKeys.PERSIST)) spawnProps.get(ConstKeys.PERSIST) as Boolean
            else false
        if (persist) removeComponent(CullablesComponent::class)
        else addComponent(CullablesComponent(this, getGameCameraCullingLogic(this)))

        if (properties.containsKey(ConstKeys.FRICTION_X))
            body.physics.frictionToApply.x = properties.get(ConstKeys.FRICTION_X) as Float
        else body.physics.frictionToApply.x = STANDARD_FRICTION_X

        if (properties.containsKey(ConstKeys.FRICTION_Y))
            body.physics.frictionToApply.y = properties.get(ConstKeys.FRICTION_Y) as Float
        else body.physics.frictionToApply.y = STANDARD_FRICTION_Y

        if (properties.containsKey(ConstKeys.GRAVITY_ON))
            body.physics.gravityOn = properties.get(ConstKeys.GRAVITY_ON) as Boolean

        if (properties.containsKey(ConstKeys.RESIST_ON))
            body.physics.takeFrictionFromOthers = properties.get(ConstKeys.RESIST_ON) as Boolean

        body.clearBodyLabels()
        if (properties.containsKey(ConstKeys.BODY_LABELS)) {
            val labels = (properties.get(ConstKeys.BODY_LABELS) as String).replace("\\s+", "").split(",")
            for (label in labels) {
                val bodyLabel = BodyLabel.valueOf(label.uppercase())
                body.addBodyLabel(bodyLabel)
            }
        }

        val bounds = spawnProps.get(ConstKeys.BOUNDS, Rectangle::class)
        if (bounds != null) body.set(bounds)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)
        if (position != null) body.setPosition(position)

        val blockFixtureOn = spawnProps.getOrDefault(ConstKeys.BLOCK_ON, true) as Boolean
        blockFixture.active = blockFixtureOn
    }
}
