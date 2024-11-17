package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.getRandom
import com.mega.game.engine.common.getRandomBool
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.state.StateMachineBuilder
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs
import kotlin.reflect.KClass

class MoonMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable, IBodySenseListener,
    IScalableGravityEntity {

    companion object {
        const val TAG = "MoonMan"

        private const val BODY_WIDTH = 1.15f
        private const val BODY_HEIGHT = 1.5f
        private const val JUMP_IMPULSE_X = 10f
        private const val JUMP_IMPULSE_Y = 20f
        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f
        private const val DEFAULT_GRAVITY_SCALAR = 0.25f

        private const val SPRITE_SIZE = 3f

        private val STAND_SHOOT_DURS = gdxArrayOf(0.3f, 0.6f, 0.1f, 0.4f, 0.1f)
        private val JUMP_SHOOT_DURS = gdxArrayOf(0.2f, 0.6f, 0.2f)

        private const val MEGAMAN_STRAIGHT_Y_THRESHOLD = 1f
        private const val INIT_DUR = 1.5f
        private const val STAND_DUR = 1.5f
        private const val SPAWN_ASTEROIDS_DUR = 0.5f
        private const val GRAVITY_CHANGE_BEGIN_DUR = 0.3f
        private const val GRAVITY_CHANGE_CONTINUE_DUR = 1.2f
        private const val GRAVITY_CHANGE_END_DUR = 1f
        private const val GRAVITY_CHANGE_DELAY_DUR = 5f
        private const val GRAVITY_CHANGE_START_CHANCE = 0.25f
        private const val GRAVITY_CHANGE_CHANGE_DELTA = 0.1f

        private const val ASTEROIDS_TO_THROW = 4

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MoonManState { INIT, STAND, JUMP, THROW_ASTEROIDS, GRAVITY_CHANGE }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(1),
        Fireball::class pairTo dmgNeg(2),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        }
    )
    override lateinit var facing: Facing
    override var gravityScalar = 1f

    private lateinit var stateMachine: StateMachine<MoonManState>
    private val currentState: MoonManState
        get() = stateMachine.getCurrent()

    private val timers = ObjectMap<String, Timer>()

    private val canActivateGravityChange: Boolean
        get() = timers["gravity_change_delay"].isFinished()
    private lateinit var gravityChangeState: ProcessState
    private lateinit var currentGravityChangeDir: Direction
    private var gravityChangeChance = 0f

    private var shootIndex = 0
    private var standIndex = 0
    private var jumpIndex = 0

    private lateinit var asteroidsSpawnBounds: GameRectangle
    private val asteroids = Array<Asteroid>()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)
            gdxArrayOf(
                "defeated",
                "gravity_change_begin",
                "gravity_change_continue",
                "gravity_change_end",
                "jump",
                "shoot",
                "jump_shoot"
            ).forEach { key ->
                val atlasKey = "$TAG/$key"
                when (key) {
                    "shoot" -> for (i in 0 until STAND_SHOOT_DURS.size) {
                        val region = atlas.findRegion(atlasKey, i)
                        regions.put("${key}_$i", region)
                    }

                    "jump_shoot" -> for (i in 0 until JUMP_SHOOT_DURS.size) {
                        val region = atlas.findRegion(atlasKey, i)
                        regions.put("${key}_$i", region)
                    }

                    else -> {
                        val region = atlas.findRegion(atlasKey)
                        regions.put(key, region)
                    }
                }
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        stateMachine.reset()

        buildTimers()
        timers.forEach { it.value.reset() }

        updateFacing()

        shootIndex = 0
        standIndex = 0
        jumpIndex = 0
        gravityChangeState = ProcessState.BEGIN
        currentGravityChangeDir = getMegaman().directionRotation!!
        gravityScalar =
            spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", DEFAULT_GRAVITY_SCALAR, Float::class)
        asteroidsSpawnBounds = spawnProps.get(Asteroid.TAG, RectangleMapObject::class)!!.rectangle.toGameRectangle()
    }

    override fun onReady() {
        GameLogger.debug(TAG, "onReady()")
        super.onReady()
        body.physics.gravityOn = true
    }

    override fun onDefeated(delta: Float) {
        GameLogger.debug(TAG, "onDefeated()")
        super.onDefeated(delta)
        asteroids.forEach { it.destroy() }
        asteroids.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                return@add
            }
            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                explodeOnDefeat(delta)
                return@add
            }

            updateFacing()

            if (currentState != MoonManState.GRAVITY_CHANGE) {
                val gravityChangeDelayTimer = timers["gravity_change_delay"]
                gravityChangeDelayTimer.update(delta)

                if (gravityChangeDelayTimer.isJustFinished()) gravityChangeChance = GRAVITY_CHANGE_START_CHANCE
                else if (gravityChangeDelayTimer.isFinished()) {
                    gravityChangeChance += GRAVITY_CHANGE_CHANGE_DELTA * delta
                    if (gravityChangeChance > 1f) gravityChangeChance = 1f
                }
            }

            when (currentState) {
                MoonManState.INIT -> {
                    if (!body.isSensing(BodySense.FEET_ON_GROUND)) return@add
                    val timer = timers["init"]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }

                MoonManState.STAND -> {
                    val timer = timers["stand"]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }

                MoonManState.JUMP -> if (shouldGoToStandState()) stateMachine.next()
                MoonManState.THROW_ASTEROIDS -> {

                }
                MoonManState.GRAVITY_CHANGE -> {
                    val timer = timers["gravity_change_${gravityChangeState.name.lowercase()}"]
                    timer.update(delta)
                    if (timer.isFinished()) {
                        when (gravityChangeState) {
                            ProcessState.END -> {
                                gravityChangeState = ProcessState.BEGIN
                                stateMachine.next()
                                return@add
                            }

                            else -> {
                                val ordinal = gravityChangeState.ordinal + 1
                                gravityChangeState = ProcessState.entries[ordinal]

                                if (gravityChangeState == ProcessState.CONTINUE) {
                                    val clockwise = getRandomBool()
                                    activateGravityChange(clockwise)
                                }
                            }
                        }
                        timer.reset()
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setBodySenseListener(this)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.2f * ConstVals.PPM))
        headFixture.offsetFromBodyCenter.y = BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(headFixture)
        headFixture.rawShape.color = Color.ORANGE
        debugShapes.add { headFixture.getShape() }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyCenter.x = -BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(leftFixture)
        leftFixture.rawShape.color = Color.BLUE
        debugShapes.add { leftFixture.getShape() }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        rightFixture.offsetFromBodyCenter.x = BODY_WIDTH * ConstVals.PPM / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.rawShape.color = Color.BLUE
        debugShapes.add { rightFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            body.physics.gravity.y =
                (if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY) * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun listenToBodySense(bodySense: BodySense, current: Boolean, old: Boolean?) {

    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (defeated) "defeated" else currentState.name.lowercase() }
        val animations = objectMapOf<String, IAnimation>(
            "defeated" pairTo Animation(regions["defeated"], 2, 2, 0.1f, true),
            "gravity_change_begin" pairTo Animation(regions["gravity_change_begin"], 3, 1, 0.1f, false),
            "gravity_change_continue" pairTo Animation(regions["gravity_change_continue"], 2, 2, 0.1f, true),
            "gravity_change_end" pairTo Animation(regions["gravity_change_end"]),
            "jump" pairTo Animation(regions["jump"]),
            "jump_shoot_1" pairTo Animation(regions["jump_shoot_1"]),
            "jump_shoot_2" pairTo Animation(regions["jump_shoot_2"], 3, 1, 0.1f, true),
            "jump_shoot_3" pairTo Animation(regions["jump_shoot_3"]),
            "shoot_1" pairTo Animation(regions["shoot_1"], 3, 1, 0.1f, false),
            "shoot_2" pairTo Animation(regions["shoot_2"], 3, 1, 0.1f, true),
            "shoot_3" pairTo Animation(regions["shoot_3"]),
            "shoot_4" pairTo Animation(regions["shoot_4"]),
            "shoot_5" pairTo Animation(regions["shoot_5"]),
            "stand" pairTo Animation(regions["stand"], 2, 1, gdxArrayOf(1f, 0.15f), true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun onSpawnAsteroidListener(asteroid: Asteroid) {
        asteroids.add(asteroid)
    }

    private fun nextShootIndex(jumping: Boolean): Boolean {
        val max = if (jumping) JUMP_SHOOT_DURS.size else STAND_SHOOT_DURS.size
        if (shootIndex + 1 >= max) {
            shootIndex = 0
            return false
        }
        shootIndex++
        return true
    }

    private fun activateGravityChange(clockwise: Boolean) {
        currentGravityChangeDir =
            if (clockwise) currentGravityChangeDir.getRotatedClockwise()
            else currentGravityChangeDir.getRotatedCounterClockwise()
        game.eventsMan.submitEvent(
            Event(EventType.SET_GAME_CAM_ROTATION, props(ConstKeys.DIRECTION pairTo currentGravityChangeDir))
        )
        gravityChangeChance = 0f
        timers["gravity_change_delay"].reset()
    }

    private fun jump() {
        body.physics.velocity.x = JUMP_IMPULSE_X * movementScalar * facing.value * ConstVals.PPM
        body.physics.velocity.y = JUMP_IMPULSE_Y * movementScalar * ConstVals.PPM
    }

    private fun onChangeState(current: MoonManState, previous: MoonManState) {
        when (previous) {
            MoonManState.STAND -> {
                standIndex++
                GameLogger.debug(TAG, "onChangeState(): increment standIndex=$standIndex")
            }

            MoonManState.JUMP -> {
                jumpIndex++
                GameLogger.debug(TAG, "onChangeState(): increment jumpIndex=$jumpIndex")
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when previous=$previous")
        }

        when (current) {
            MoonManState.INIT -> TODO()
            MoonManState.STAND -> {
                timers["stand"].reset()

            }

            MoonManState.JUMP -> jump()
            MoonManState.THROW_ASTEROIDS -> TODO()
            MoonManState.GRAVITY_CHANGE -> TODO()
        }
    }

    private fun buildStateMachine(): StateMachine<MoonManState> {
        val builder = StateMachineBuilder<MoonManState>()
        MoonManState.entries.forEach { builder.state(it.name, it) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(MoonManState.INIT.name)
            .transition(MoonManState.INIT.name, MoonManState.STAND.name) { true }
            .transition(MoonManState.STAND.name, MoonManState.GRAVITY_CHANGE.name) { shouldEnterGravityChangeState() }
            .transition(MoonManState.STAND.name, MoonManState.JUMP.name) { true }
            .transition(MoonManState.GRAVITY_CHANGE.name, MoonManState.STAND.name) { true }
            .transition(MoonManState.JUMP.name, MoonManState.STAND.name) { shouldGoToStandState() }
        return builder.build()
    }

    private fun buildTimers() {
        timers.put("init", Timer(INIT_DUR))
        timers.put("stand", Timer(STAND_DUR))
        timers.put("gravity_change_begin", Timer(GRAVITY_CHANGE_BEGIN_DUR))
        timers.put("gravity_change_continue", Timer(GRAVITY_CHANGE_CONTINUE_DUR))
        timers.put("gravity_change_end", Timer(GRAVITY_CHANGE_END_DUR))
        timers.put("gravity_change_delay", Timer(GRAVITY_CHANGE_DELAY_DUR))

        for (i in 0 until STAND_SHOOT_DURS.size) {
            val key = "shoot_$i"
            val dur = STAND_SHOOT_DURS[i]
            timers.put(key, Timer(dur))
        }

        for (i in 0 until JUMP_SHOOT_DURS.size) {
            val key = "jump_shoot_$i"
            val dur = JUMP_SHOOT_DURS[i]
            timers.put(key, Timer(dur))
        }
    }

    private fun isMegamanStraightAhead() =
        abs(getMegaman().body.y - body.y) <= MEGAMAN_STRAIGHT_Y_THRESHOLD * ConstVals.PPM

    private fun updateFacing() {
        when {
            getMegaman().body.getMaxX() < body.x -> facing = Facing.LEFT
            getMegaman().body.x > body.getMaxX() -> facing = Facing.RIGHT
        }
    }

    private fun shouldEnterGravityChangeState(): Boolean {
        if (!canActivateGravityChange) return false
        val randomPercentage = getRandom(0f, 1f)
        return randomPercentage < gravityChangeChance
    }

    private fun shouldGoToStandState() = body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND)

    private fun canSpawnAsteroidsInStandState() = (standIndex + 1) % 3 == 0

    private fun canShootInStandState() = standIndex % 2 == 0

    private fun canShootInJumpState() = (jumpIndex + 1) % 2 == 0

    override fun getTag() = TAG
}
