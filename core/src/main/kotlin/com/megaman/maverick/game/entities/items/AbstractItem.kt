package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.set
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.contracts.ItemEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

abstract class AbstractItem(game: MegamanMaverickGame) : MegaGameEntity(game), ItemEntity, IBodyEntity,
    IScalableGravityEntity, ICullableEntity, IDirectional {

    companion object {
        const val TAG = "AbstractItem"

        private const val GRAVITY = 0.25f
        private const val MOON_GRAVITY = 0.1f
        private const val WATER_GRAVITY = 0.1f

        private const val VEL_CLAMP = 12f
        private const val MOON_VEL_CLAMP = 2.5f
        private const val WATER_VEL_CLAMP = 2.5f
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var gravityScalar = 1f

    protected var gravity = GRAVITY
    protected var velClamp = VEL_CLAMP

    override fun init() {
        super.init()

        addComponent(CullablesComponent())
        addComponent(defineBodyComponent())

        val updatablesComponent = UpdatablesComponent()
        defineUpdatablesComponent(updatablesComponent)
        addComponent(updatablesComponent)
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)

        direction = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, megaman.direction.name, String::class).uppercase()
        )

        val position = DirectionPositionMapper.getInvertedPosition(direction)
        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        body.positionOnPoint(spawn, position)

        val cullOutOfBounds = spawnProps.getOrDefault(ConstKeys.CULL_OUT_OF_BOUNDS, true, Boolean::class)
        when {
            cullOutOfBounds -> putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, getGameCameraCullingLogic(this))
            else -> removeCullable(ConstKeys.CULL_OUT_OF_BOUNDS)
        }

        gravity = spawnProps.getOrDefault(
            ConstKeys.GRAVITY,
            when (LevelDefinition.MOON_MAN) {
                game.getCurrentLevel() -> MOON_GRAVITY
                else -> GRAVITY
            },
            Float::class
        )
        velClamp = spawnProps.getOrDefault(ConstKeys.CLAMP, VEL_CLAMP, Float::class)
        gravityScalar = spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", 1f, Float::class)
    }

    open fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)

        val itemFixture = Fixture(body, FixtureType.ITEM, GameRectangle())
        body.addFixture(itemFixture)
        itemFixture.drawingColor = Color.PURPLE
        debugShapes.add { itemFixture }

        val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle())
        waterListenerFixture.setHitByWaterReceiver { water ->
            body.physics.velocity.setZero()
            gravity = WATER_GRAVITY
            velClamp = WATER_VEL_CLAMP
        }
        body.addFixture(waterListenerFixture)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (game.getCurrentLevel() == LevelDefinition.MOON_MAN) velClamp = MOON_VEL_CLAMP
            body.physics.velocityClamp.set(velClamp * ConstVals.PPM)

            if (body.isSensing(BodySense.FEET_ON_GROUND)) whenFeetOnGround()
            if (body.isSensing(BodySense.FEET_ON_SAND)) whenFeetOnSand()
            if (!body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_SAND)) whenInAir()

            (bodyFixture.rawShape as GameRectangle).set(body)
            (itemFixture.rawShape as GameRectangle).set(body)
            (waterListenerFixture.rawShape as GameRectangle).set(body)

            feetFixture.offsetFromBodyAttachment.y = (-body.getHeight() / 2f) + 0.1f * ConstVals.PPM
            feetFixture.putProperty(ConstKeys.STICK_TO_BLOCK, !body.isSensing(BodySense.FEET_ON_SAND))
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    open fun defineUpdatablesComponent(component: UpdatablesComponent) {
        component.add { direction = megaman.direction }
    }

    protected open fun whenFeetOnGround() {
        body.physics.gravityOn = false
        body.physics.velocity.setZero()
    }

    protected open fun whenFeetOnSand() {
        body.physics.gravityOn = false
        body.physics.velocity.setZero()
    }

    protected open fun whenInAir() {
        body.physics.gravityOn = true

        val gravityVec = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.LEFT -> gravityVec.set(gravity, 0f)
            Direction.RIGHT -> gravityVec.set(-gravity, 0f)
            Direction.UP -> gravityVec.set(0f, -gravity)
            Direction.DOWN -> gravityVec.set(0f, gravity)
        }.scl(gravityScalar * ConstVals.PPM.toFloat())

        body.physics.gravity.set(gravityVec)
    }

    override fun getType() = EntityType.ITEM
}
