package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.putAll
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*

class AstroAssAssaulter(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity,
    IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "AstroAssAssaulter"

        private const val STAND_DUR = 1f
        private const val SHOOT_DUR = 1f
        private const val SHOOT_EACH_DELAY = 0.25f

        private const val THROW_DUR = 0.25f
        private const val THROW_TIME = 0.1f

        private const val SENSOR_WIDTH = 30f
        private const val SENSOR_HEIGHT = 3f

        private const val FLAG_THROW_IMPULSE_X = 8f
        private const val FLAG_THROW_IMPULSE_Y = 5f
        private const val DEFAULT_FLAG_GRAVITY_SCALAR = 0.75f
        private const val DEFAULT_FLAG_MOVEMENT_SCALAR = 1f

        private const val LAZER_SPEED = 8f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class AstroAssState { STAND, SHOOT, THROW }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<AstroAssState>
    private val currentState: AstroAssState
        get() = stateMachine.getCurrent()
    private val stateTimers = OrderedMap<AstroAssState, Timer>()

    private var flag: StagedMoonLandingFlag? = null
    private val sensor = GameRectangle()
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
            stateTimers.put(AstroAssState.SHOOT, Timer(SHOOT_DUR).also { timer ->
                val runnables = Array<TimeMarkedRunnable>()

                val max = (SHOOT_DUR / SHOOT_EACH_DELAY).toInt()
                for (i in 0 until max) {
                    val time = SHOOT_EACH_DELAY * i
                    val runnable = TimeMarkedRunnable(time) { shootLazer() }
                    runnables.add(runnable)
                }

                timer.setRunnables(runnables)
            })
            stateTimers.put(AstroAssState.THROW, Timer(THROW_DUR).also { timer ->
                val runnable = TimeMarkedRunnable(THROW_TIME) { throwFlag() }
                timer.setRunnables(runnable)
            })
        }

        stateMachine = buildStateMachine()

        super.init()

        addComponent(defineAnimationsComponent())

        sensor.drawingColor = Color.GRAY
        addDebugShapeSupplier { sensor }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)

        updateFacing()

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        shootUp = false

        sensor.set(
            when {
                spawnProps.containsKey(ConstKeys.SENSORS) ->
                    spawnProps.get(ConstKeys.SENSOR, RectangleMapObject::class)!!.rectangle.toGameRectangle()

                else -> GameObjectPools.fetch(GameRectangle::class)
                    .setSize(SENSOR_WIDTH * ConstVals.PPM, SENSOR_HEIGHT * ConstVals.PPM)
                    .setCenter(body.getCenter())
            }
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()

        flag?.setToBeDestroyed()
        flag = null
    }

    private fun shouldThrowFlag() = flag == null && sensor.overlaps(megaman.body.getBounds())

    private fun throwFlag() {
        GameLogger.debug(TAG, "throwFlag()")

        val spawn = GameObjectPools.fetch(Vector2::class)
            .set(0.35f * facing.value, 0.2f)
            .scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        // TODO: impulse dependent on direction
        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(FLAG_THROW_IMPULSE_X * facing.value, FLAG_THROW_IMPULSE_Y)
            .scl(ConstVals.PPM.toFloat())

        val flag =
            EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.STAGED_MOON_LANDING_FLAG)!! as StagedMoonLandingFlag
        flag.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse,
                "${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}" pairTo DEFAULT_FLAG_MOVEMENT_SCALAR,
                "${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}" pairTo DEFAULT_FLAG_GRAVITY_SCALAR
            )
        )
        this.flag = flag
    }

    private fun shouldShootLazer() = sensor.overlaps(megaman.body.getBounds())

    private fun shootLazer() {
        val spawn = GameObjectPools.fetch(Vector2::class)
            .set(facing.value.toFloat(), 0.3f)
            .scl(ConstVals.PPM.toFloat()).add(body.getCenter())

        val trajectory = GameObjectPools.fetch(Vector2::class).set(LAZER_SPEED * ConstVals.PPM * facing.value, 0f)

        val props = props(
            ConstKeys.OWNER pairTo this,
            ConstKeys.POSITION pairTo spawn,
            ConstKeys.TRAJECTORY pairTo trajectory
        )

        val lazer =
            EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SUPER_COOL_ACTION_STAR_WARS_SPACE_LAZER)!!
        lazer.spawn(props)

        requestToPlaySound(SoundAsset.SPACE_LAZER_SOUND, false)

        GameLogger.debug(TAG, "shootLazer(): spawn=$spawn, trajectory=$trajectory")
    }

    private fun updateFacing() {
        when {
            megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
            megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (flag?.dead == true) flag = null

            if (currentState == AstroAssState.STAND) updateFacing()

            val timer = stateTimers[currentState]
            timer.update(delta)

            if (timer.isFinished()) stateMachine.next()
        }
    }

    private fun buildStateMachine(): StateMachine<AstroAssState> {
        val builder = StateMachineBuilder<AstroAssState>()
        builder.setOnChangeState(this::onChangeState)
        AstroAssState.entries.forEach { builder.state(it.name, it) }
        builder.initialState(AstroAssState.STAND.name)
            .transition(AstroAssState.STAND.name, AstroAssState.THROW.name) { shouldThrowFlag() }
            .transition(AstroAssState.STAND.name, AstroAssState.SHOOT.name) { shouldShootLazer() }
            .transition(AstroAssState.THROW.name, AstroAssState.STAND.name) { true }
            .transition(AstroAssState.SHOOT.name, AstroAssState.STAND.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: AstroAssState, previous: AstroAssState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        stateTimers[previous].reset()
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
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
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
                        "stand" pairTo Animation(regions["stand"], 2, 1, gdxArrayOf(1f, 0.15f), true),
                        "throw" pairTo Animation(regions["throw"], 2, 1, 0.1f, false),
                        "shoot" pairTo Animation(regions["shoot"], 2, 1, 0.125f, true),
                        "shoot_up" pairTo Animation(regions["shoot_up"], 2, 1, 0.125f, true)
                    )
                }
                .build()
        )
        .build()
}
