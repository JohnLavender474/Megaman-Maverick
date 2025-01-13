package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.putAll
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.state.StateMachineBuilder
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.EnemyDamageNegotiations
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class AstroAssAssaulter(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity,
    IDirectional, IFaceable {

    companion object {
        const val TAG = "AstroAssAssaulter"

        private const val STAND_DUR = 1f
        private const val SHOOT_DUR = 0.5f

        private const val THROW_DUR = 0.25f
        private const val THROW_TIME = 0.1f

        private const val FLAG_SENSOR_WIDTH = 20f
        private const val FLAG_SENSOR_HEIGHT = 3f

        private const val FLAG_THROW_IMPULSE_X = 8f
        private const val FLAG_THROW_IMPULSE_Y = 5f

        private const val LAZER_SPEED = 10f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class AstroAssState { STAND, SHOOT, THROW }

    override val damageNegotiations = EnemyDamageNegotiations.getEnemyDmgNegs(Size.MEDIUM)
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<AstroAssState>
    private val currentState: AstroAssState
        get() = stateMachine.getCurrent()

    private val stateTimers = OrderedMap<AstroAssState, Timer>()

    private val flagSensor = GameRectangle()
        .setSize(FLAG_SENSOR_WIDTH * ConstVals.PPM, FLAG_SENSOR_HEIGHT * ConstVals.PPM)

    private var shootUp = false

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            gdxArrayOf("stand", "throw", "shoot", "shoot_up").forEach { key ->
                regions.put(key, atlas.findRegion("${TAG}/$key"))
            }
        }

        if (stateTimers.isEmpty) {
            stateTimers.put(AstroAssState.STAND, Timer(STAND_DUR))
            stateTimers.put(AstroAssState.SHOOT, Timer(SHOOT_DUR))
            stateTimers.put(AstroAssState.THROW, Timer(THROW_DUR).also { timer ->
                val runnable = TimeMarkedRunnable(THROW_TIME) { throwFlag() }
                timer.setRunnables(runnable)
            })
        }

        stateMachine = buildStateMachine()

        super.init()

        addComponent(defineAnimationsComponent())

        flagSensor.drawingColor = Color.GRAY
        addDebugShapeSupplier { flagSensor }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        val position = DirectionPositionMapper.getInvertedPosition(direction)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position)

        updateFacing()

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        shootUp = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun shouldThrowFlag() =
        MegaGameEntities.getEntitiesOfTag(StagedMoonLandingFlag.TAG).isEmpty &&
            flagSensor.overlaps(megaman.body.getBounds())

    private fun throwFlag() {
        GameLogger.debug(TAG, "throwFlag()")

        val spawn = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> spawn.set(0.35f * facing.value, 0.2f)
            Direction.DOWN -> spawn.set(0.35f * facing.value, -0.2f)
            Direction.LEFT -> spawn.set(-0.2f, 0.35f * facing.value)
            Direction.RIGHT -> spawn.set(0.2f, 0.35f * -facing.value)
        }
        spawn.scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        // TODO: impulse dependent on direction
        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(FLAG_THROW_IMPULSE_X * facing.value, FLAG_THROW_IMPULSE_Y)
            .scl(ConstVals.PPM.toFloat())

        val flag = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.STAGED_MOON_LANDING_FLAG)!!
        flag.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse
            )
        )
    }

    private fun shootLazer() {
        val spawn = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP -> spawn.set(0.35f * facing.value, 0.1f)
            Direction.DOWN -> spawn.set(0.35f * facing.value, -0.1f)
            Direction.LEFT -> spawn.set(-0.1f, 0.35f * facing.value)
            Direction.RIGHT -> spawn.set(0.1f, 0.35f * -facing.value)
        }
        spawn.scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = GameObjectPools.fetch(Vector2::class)
        when (direction) {
            Direction.UP, Direction.DOWN ->
                trajectory.set(LAZER_SPEED * ConstVals.PPM * facing.value, 0f)

            Direction.LEFT ->
                trajectory.set(0f, LAZER_SPEED * ConstVals.PPM * facing.value)

            Direction.RIGHT ->
                trajectory.set(0f, -LAZER_SPEED * ConstVals.PPM * facing.value)
        }

        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory
        )

        val lazer =
            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SUPER_COOL_ACTION_STAR_WARS_SPACE_LAZER)!!
        lazer.spawn(props)

        GameLogger.debug(TAG, "shootLazer(): spawn=$spawn, trajectory=$trajectory")
    }

    private fun updateFacing() {
        facing = when (direction) {
            Direction.UP, Direction.DOWN -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (megaman.body.getY() < body.getY()) Facing.RIGHT else Facing.LEFT
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            flagSensor.setCenter(body.getCenter())

            if (currentState == AstroAssState.STAND) updateFacing()

            val timer = stateTimers[currentState]
            timer.update(delta)

            if (timer.isFinished()) {
                stateMachine.next()

                timer.reset()
            }
        }
    }

    private fun buildStateMachine(): StateMachine<AstroAssState> {
        val builder = StateMachineBuilder<AstroAssState>()
        builder.setOnChangeState(this::onChangeState)
        AstroAssState.entries.forEach { builder.state(it.name, it) }
        builder.initialState(AstroAssState.STAND.name)
            .transition(AstroAssState.STAND.name, AstroAssState.THROW.name) { shouldThrowFlag() }
            .transition(AstroAssState.STAND.name, AstroAssState.SHOOT.name) { true }
            .transition(AstroAssState.THROW.name, AstroAssState.STAND.name) { true }
            .transition(AstroAssState.SHOOT.name, AstroAssState.STAND.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: AstroAssState, previous: AstroAssState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        if (current == AstroAssState.SHOOT) {
            shootUp = false // TODO: determine if shoot up is true or false

            shootLazer()
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2.5f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val flipX = facing == Facing.RIGHT
            val flipY = direction == Direction.DOWN
            sprite.setFlip(flipX, flipY)

            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            val position = DirectionPositionMapper.getInvertedPosition(direction)
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when (currentState) {
                        AstroAssState.STAND, AstroAssState.THROW -> currentState.name.lowercase()
                        else -> "shoot${if (shootUp) "_up" else ""}"
                    }
                }
                .applyToAnimations { animations ->
                    animations.putAll(
                        "stand" pairTo Animation(regions["stand"]),
                        "throw" pairTo Animation(regions["throw"], 2, 1, 0.1f, false),
                        "shoot" pairTo Animation(regions["shoot"], 2, 1, 0.1f, false),
                        "shoot_up" pairTo Animation(regions["shoot_up"], 2, 1, 0.1f, false)
                    )
                }
                .build()
        )
        .build()
}
