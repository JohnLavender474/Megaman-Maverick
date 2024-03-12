package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.engine.IGame2D
import com.engine.common.GameLogger
import com.engine.common.extensions.objectMapOf
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

    private val fixturesToRemove = ObjectSet<Fixture>()

    override fun init() {
        GameLogger.debug(TAG, "init(): Initializing Block entity.")
        addComponent(defineBodyComponent())
        debugShapeSuppliers.add { body }
        addComponent(
            DrawableShapesComponent(this, debugShapeSuppliers = debugShapeSuppliers, debug = true)
        )
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val cullOutOfBounds =
            if (spawnProps.containsKey(ConstKeys.CULL_OUT_OF_BOUNDS)) spawnProps.get(ConstKeys.CULL_OUT_OF_BOUNDS) as Boolean
            else true
        if (cullOutOfBounds) addComponent(
            CullablesComponent(
                this, objectMapOf(
                    ConstKeys.CULL_OUT_OF_BOUNDS to getGameCameraCullingLogic(this)
                )
            )
        )
        else removeComponent(CullablesComponent::class)

        if (properties.containsKey(ConstKeys.FRICTION_X)) body.physics.frictionToApply.x =
            properties.get(ConstKeys.FRICTION_X) as Float
        else body.physics.frictionToApply.x = STANDARD_FRICTION_X

        if (properties.containsKey(ConstKeys.FRICTION_Y)) body.physics.frictionToApply.y =
            properties.get(ConstKeys.FRICTION_Y) as Float
        else body.physics.frictionToApply.y = STANDARD_FRICTION_Y

        if (properties.containsKey(ConstKeys.GRAVITY_ON)) body.physics.gravityOn =
            properties.get(ConstKeys.GRAVITY_ON) as Boolean

        if (properties.containsKey(ConstKeys.RESIST_ON)) body.physics.takeFrictionFromOthers =
            properties.get(ConstKeys.RESIST_ON) as Boolean

        body.clearBodyLabels()
        if (properties.containsKey(ConstKeys.BODY_LABELS)) {
            val labels = properties.get(ConstKeys.BODY_LABELS)
            if (labels is String) {
                val labelStrings = labels.replace("\\s+", "").split(",")
                labelStrings.forEach {
                    val bodyLabel = BodyLabel.valueOf(it.uppercase())
                    body.addBodyLabels(bodyLabel)
                }
            } else {
                labels as Array<BodyLabel>
                body.addBodyLabels(labels)
            }
        }

        val bounds = spawnProps.get(ConstKeys.BOUNDS, Rectangle::class)
        if (bounds != null) body.set(bounds)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)
        if (position != null) body.setPosition(position)

        val blockFixtureOn = spawnProps.getOrDefault(ConstKeys.BLOCK_ON, true) as Boolean
        blockFixture.active = blockFixtureOn

        val fixtureEntriesToAdd = spawnProps.get(ConstKeys.FIXTURES) as Array<Pair<FixtureType, Properties>>?
        fixtureEntriesToAdd?.forEach { fixtureEntry ->
            val (fixtureType, fixtureProps) = fixtureEntry
            val fixture = Fixture(GameRectangle().set(body), fixtureType)
            fixture.putAllProperties(fixtureProps)
            fixture.setEntity(this)
            body.addFixture(fixture)
            fixturesToRemove.add(fixture)
        }
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        val fixtureIter = body.fixtures.iterator()
        while (fixtureIter.hasNext()) {
            val (_, fixture) = fixtureIter.next()
            if (fixturesToRemove.contains(fixture)) fixtureIter.remove()
        }
    }

    protected open fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)

        // block fixture
        blockFixture = Fixture(GameRectangle(), FixtureType.BLOCK)
        body.addFixture(blockFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable { (blockFixture.shape as GameRectangle).set(body) })

        return BodyComponentCreator.create(this, body)
    }
}
