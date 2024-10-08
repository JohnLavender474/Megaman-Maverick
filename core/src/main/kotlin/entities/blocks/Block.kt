package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.body.*

open class Block(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity,
    IDrawableShapesEntity {

    companion object {
        const val TAG = "Block"
        const val STANDARD_FRICTION_X = 1.75f
        const val STANDARD_FRICTION_Y = 0f
    }

    lateinit var blockFixture: Fixture
        private set
    protected val debugShapeSuppliers = Array<() -> IDrawableShape?>()
    private val fixturesToRemove = ObjectSet<Fixture>()

    override fun getEntityType() = EntityType.BLOCK

    override fun getTag() = TAG

    override fun init() {
        GameLogger.debug(TAG, "init(): Initializing Block entity.")
        addComponent(defineBodyComponent())
        body.color = Color.GRAY
        debugShapeSuppliers.add { body.getBodyBounds() }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapeSuppliers, debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawned:` $spawnProps")
        super.onSpawn(spawnProps)

        // TODO: if the bug where blocks do not always spawn persists, then the default value here can be set to false
        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        if (cullOutOfBounds) addComponent(
            CullablesComponent(
                objectMapOf(
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this)
                )
            )
        ) else removeComponent(CullablesComponent::class)

        body.physics.frictionToApply.x =
            spawnProps.getOrDefault(ConstKeys.FRICTION_X, STANDARD_FRICTION_X, Float::class)
        body.physics.frictionToApply.y =
            spawnProps.getOrDefault(ConstKeys.FRICTION_Y, STANDARD_FRICTION_Y, Float::class)
        body.physics.gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)

        val resistOn = spawnProps.getOrDefault(ConstKeys.RESIST_ON, true, Boolean::class)
        body.physics.applyFrictionX = resistOn
        body.physics.applyFrictionY = resistOn

        body.clearBodyLabels()
        if (spawnProps.containsKey(ConstKeys.BODY_LABELS)) {
            val labels = spawnProps.get(ConstKeys.BODY_LABELS)
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
        if (spawnProps.containsKey(ConstKeys.FIXTURE_LABELS)) {
            val labels = spawnProps.get(ConstKeys.FIXTURE_LABELS)
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

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)
        if (bounds != null) body.set(bounds)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)
        if (position != null) body.setPosition(position)

        blockFixture.active = spawnProps.getOrDefault(ConstKeys.BLOCK_ON, true, Boolean::class)

        val fixtureEntriesToAdd = spawnProps.get(ConstKeys.FIXTURES) as Array<GamePair<FixtureType, Properties>>?
        fixtureEntriesToAdd?.forEach { fixtureEntry ->
            val (fixtureType, fixtureProps) = fixtureEntry
            val fixture = Fixture(body, fixtureType, GameRectangle().set(body))
            fixture.putAllProperties(fixtureProps)
            fixture.setEntity(this)
            body.addFixture(fixture)
            fixturesToRemove.add(fixture)
        }

        body.clearBlockFilters()
        if (spawnProps.containsKey(ConstKeys.BLOCK_FILTERS)) {
            val filters = spawnProps.get(ConstKeys.BLOCK_FILTERS)
            if (filters is String) {
                val filterStrings = filters.replace("\\s+", "").split(",")
                filterStrings.forEach { body.addBlockFilter(it.uppercase()) }
            } else {
                filters as ObjectSet<String>
                filters.forEach { body.addBlockFilter(it.uppercase()) }
            }
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "Destroyed: $properties")
        super.onDestroy()

        val fixtureIter = body.fixtures.iterator()
        while (fixtureIter.hasNext()) {
            val (_, fixture) = fixtureIter.next()
            if (fixturesToRemove.contains(fixture)) fixtureIter.remove()
        }

        fixturesToRemove.clear()
    }

    open fun hitByBody(bodyFixture: IFixture) {}

    open fun hitBySide(sideFixture: IFixture) {}

    open fun hitByFeet(feetFixture: IFixture) {}

    open fun hitByHead(headFixture: IFixture) {}

    open fun hitByProjectile(projectileFixture: IFixture) {}

    protected open fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)
        body.color = Color.GRAY
        debugShapeSuppliers.add { body.getBodyBounds() }

        blockFixture = Fixture(body, FixtureType.BLOCK)
        body.addFixture(blockFixture)
        blockFixture.rawShape.color = Color.BLUE
        debugShapeSuppliers.add { blockFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) { (blockFixture.rawShape as GameRectangle).set(body) }

        return BodyComponentCreator.create(this, body)
    }
}
