package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.IGameEntity
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
        const val STANDARD_FRICTION_X = 1.85f
        const val STANDARD_FRICTION_Y = 0f
    }

    lateinit var blockFixture: Fixture
        private set

    var draw = true

    protected val debugShapeSuppliers = Array<() -> IDrawableShape?>()

    private val fixturesToRemove = ObjectSet<Fixture>()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        debugShapeSuppliers.add { if (draw) body.getBounds() else null }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapeSuppliers, debug = true))

        addComponent(CullablesComponent())
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        // TODO: if the bug where blocks do not always spawn persists, then the default value here can be set to false
        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        when {
            cullOutOfBounds -> putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(this))
            else -> removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)
        }

        body.physics.frictionToApply.x =
            spawnProps.getOrDefault(ConstKeys.FRICTION_X, STANDARD_FRICTION_X, Float::class)
        body.physics.frictionToApply.y =
            spawnProps.getOrDefault(ConstKeys.FRICTION_Y, STANDARD_FRICTION_Y, Float::class)

        val flipFriction = spawnProps.getOrDefault("${ConstKeys.FLIP}_${ConstKeys.FRICTION}", false, Boolean::class)
        if (flipFriction) {
            val temp = body.physics.frictionToApply.x
            body.physics.frictionToApply.x = body.physics.frictionToApply.y
            body.physics.frictionToApply.y = temp
        }

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
        when {
            bounds != null -> body.set(bounds)
            else -> {
                val width = spawnProps.get(ConstKeys.WIDTH, Float::class)
                if (width != null) body.setWidth(width)

                val height = spawnProps.get(ConstKeys.HEIGHT, Float::class)
                if (height != null) body.setHeight(height)

                val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)
                if (position != null) body.setPosition(position)
            }
        }

        val collisionOn = spawnProps.getOrDefault(ConstKeys.ON, true, Boolean::class)
        body.physics.collisionOn = collisionOn

        blockFixture.setActive(spawnProps.getOrDefault(ConstKeys.ACTIVE, true, Boolean::class))

        val fixtureEntriesToAdd = spawnProps.get(ConstKeys.FIXTURES) as Array<GamePair<FixtureType, Properties>>?
        fixtureEntriesToAdd?.forEach { fixtureEntry ->
            val (fixtureType, fixtureProps) = fixtureEntry
            val fixture = Fixture(body, fixtureType, GameRectangle(body))
            fixture.putAllProperties(fixtureProps)
            fixture.setEntity(this)
            body.addFixture(fixture)
            fixturesToRemove.add(fixture)
        }

        body.clearBlockFilters()
        if (spawnProps.containsKey(ConstKeys.BLOCK_FILTERS)) {
            var filters = spawnProps.get(ConstKeys.BLOCK_FILTERS)
            when (filters) {
                is String -> {
                    filters = filters.replace("\\s+", "").split(",")
                    filters.forEach { tag ->
                        body.addBlockFilter { entity, _ ->
                            entity.getTag().uppercase() == tag.uppercase()
                        }
                    }
                }

                is Iterable<*> -> {
                    try {
                        filters as Iterable<String>
                        filters.forEach { tag ->
                            body.addBlockFilter { entity, _ ->
                                entity.getTag().uppercase() == tag.uppercase()
                            }
                        }
                    } catch (e: Exception) {
                        GameLogger.debug(TAG, "onSpawn(): e=$e")
                        filters as Iterable<(MegaGameEntity, MegaGameEntity) -> Boolean>
                        filters.forEach { filter -> body.addBlockFilter(filter) }
                    }
                }

                else -> {
                    val filter = filters as (MegaGameEntity, MegaGameEntity) -> Boolean
                    body.addBlockFilter(filter)
                }
            }
        }

        draw = spawnProps.getOrDefault(ConstKeys.DRAW, true, Boolean::class)

        val owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        if (owner == null) removeProperty(ConstKeys.OWNER) else putProperty(ConstKeys.OWNER, owner)

        val feetSound = spawnProps.getOrDefault("${ConstKeys.FEET}_${ConstKeys.SOUND}", true, Boolean::class)
        putProperty("${ConstKeys.FEET}_${ConstKeys.SOUND}", feetSound)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        fixturesToRemove.forEach { body.removeFixture(it) }
        fixturesToRemove.clear()
    }

    open fun hitByBody(bodyFixture: IFixture) {}

    open fun hitBySide(sideFixture: IFixture) {}

    open fun hitByFeet(processState: ProcessState, feetFixture: IFixture) {}

    open fun hitByHead(processState: ProcessState, headFixture: IFixture) {}

    open fun hitByProjectile(projectileFixture: IFixture) {}

    protected open fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)
        debugShapeSuppliers.add { if (body.physics.collisionOn) body.getBounds() else null }

        val blockRect = GameRectangle()
        blockFixture = Fixture(body, FixtureType.BLOCK, blockRect)
        body.addFixture(blockFixture)
        debugShapeSuppliers.add { blockFixture }

        body.preProcess.put("${ConstKeys.BLOCK}_${ConstKeys.FIXTURE}") {
            blockRect.set(body)

            blockFixture.drawingColor = if (blockFixture.isActive()) Color.RED else Color.GRAY
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun getType() = EntityType.BLOCK

    override fun getTag() = TAG
}
