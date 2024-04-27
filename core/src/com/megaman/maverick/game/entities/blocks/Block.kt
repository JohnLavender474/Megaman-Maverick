package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.engine.IGame2D
import com.engine.common.GameLogger
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.*

open class Block(game: IGame2D) : GameEntity(game), IBodyEntity {

    companion object {
        const val TAG = "Block"
        const val STANDARD_FRICTION_X = 0.035f
        const val STANDARD_FRICTION_Y = 0f
    }

    lateinit var blockFixture: Fixture
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

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
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
                    body.addBodyLabel(bodyLabel)
                }
            } else {
                labels as ObjectSet<BodyLabel>
                body.addBodyLabels(labels)
            }
        }

        blockFixture.clearFixtureLabels()
        if (properties.containsKey(ConstKeys.FIXTURE_LABELS)) {
            val labels = properties.get(ConstKeys.FIXTURE_LABELS)
            if (labels is String) {
                val labelStrings = labels.replace("\\s+", "").split(",")
                labelStrings.forEach {
                    val fixtureLabel = FixtureLabel.valueOf(it.uppercase())
                    blockFixture.addFixtureLabel(fixtureLabel)
                }
            } else {
                labels as ObjectSet<FixtureLabel>
                blockFixture.addFixtureLabels(labels)
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
            val fixture = Fixture(body, fixtureType, GameRectangle().set(body))
            fixture.putAllProperties(fixtureProps)
            fixture.setEntity(this)
            body.addFixture(fixture)
            fixturesToRemove.add(fixture)
        }

        body.clearBlockFilters()
        if (properties.containsKey(ConstKeys.BLOCK_FILTERS)) {
            val filters = properties.get(ConstKeys.BLOCK_FILTERS)
            if (filters is String) {
                val filterStrings = filters.replace("\\s+", "").split(",")
                filterStrings.forEach { body.addBlockFilter(it) }
            } else {
                filters as ObjectSet<String>
                filters.forEach { body.addBlockFilter(it) }
            }
        }
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        val fixtureIter = body.fixtures.iterator()
        while (fixtureIter.hasNext()) {
            val (_, fixture) = fixtureIter.next()
            if (fixturesToRemove.contains(fixture)) fixtureIter.remove()
        }
        fixturesToRemove.clear()
    }

    open fun hitBySide(sideFixture: IFixture) {}

    open fun hitByFeet(feetFixture: IFixture) {}

    open fun hitByHead(headFixture: IFixture) {}

    open fun hitByProjectile(projectileFixture: IFixture) {}

    protected open fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)
        blockFixture = Fixture(body, FixtureType.BLOCK, GameRectangle())
        body.addFixture(blockFixture)
        body.preProcess.put(ConstKeys.DEFAULT) { (blockFixture.rawShape as GameRectangle).set(body) }
        return BodyComponentCreator.create(this, body)
    }
}
