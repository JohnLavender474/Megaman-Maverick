package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.IScalableGravityEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods

import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs
import kotlin.reflect.KClass

class MoonMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IScalableGravityEntity, IFaceable,
    IDirectional {

    companion object {
        const val TAG = "MoonMan"

        private const val TAG_SUFFIX_FOR_ATLAS = "_v2"

        // if false, Gravity Man's direction rotation will never change
        private const val CHANGE_GRAVITY = false

        private const val BODY_WIDTH = 1.15f
        private const val BODY_HEIGHT = 1.5f

        private const val JUMP_IMPULSE_Y = 6f
        private const val JUMP_MAX_IMPULSE_X = 8f
        private const val JUMP_MIN_HORIZONTAL_SCALAR = 0.5f
        private const val JUMP_MAX_HORIZONTAL_SCALAR = 0.75f
        private const val JUMP_HORIZONTAL_SCALAR_DENOMINATOR = 8

        private const val GRAVITY = 0.25f
        private const val GROUND_GRAVITY = 0.01f
        private const val DEFAULT_GRAVITY_SCALAR = 0.25f
        private const val DEFAULT_FRICTION_X = 6f
        private const val DEFAULT_FRICTION_Y = 1.015f

        private const val SPRITE_SIZE = 2.25f

        private const val INIT_DUR = 0.3f
        private const val STAND_DUR = 0.75f
        private const val SPAWN_ASTEROID_DELAY = 1.25f
        private const val THROW_ASTEROID_DELAY = 1f
        private const val ASTEROIDS_END_DUR = 1f

        private const val GRAVITY_CHANGE_BEGIN_DUR = 0.3f
        private const val GRAVITY_CHANGE_CONTINUE_DUR = 1.2f
        private const val GRAVITY_CHANGE_END_DUR = ConstVals.GAME_CAM_ROTATE_TIME

        private const val GRAVITY_CHANGE_DELAY_DUR = 5f
        private const val GRAVITY_CHANGE_START_CHANCE = 0.1f
        private const val GRAVITY_CHANGE_CHANGE_DELTA = 0.1f

        private const val ASTEROIDS_TO_SPAWN = 3
        private const val ASTEROID_SPEED = 6f

        private const val SHARP_STAR_SPEED = 8f
        private const val SHARP_STAR_MOVEMENT_SCALAR = 0.75f

        private const val MOON_SCYTHE_SPEED = 8f
        private const val MOON_SCYTHE_MOVEMENT_SCALAR = 0.75f
        private val MOON_SCYTHE_DEG_OFFSETS = gdxArrayOf(7.5f, 35f, 62.5f)

        private val STAND_SHOOT_DURS = gdxArrayOf(0.5f, 0.5f, 1f, 0.5f, 1f, 0.5f, 0.6f)

        private val ANIM_DEFS = orderedMapOf<String, AnimationDef>(
            "defeated" pairTo AnimationDef(2, 2, 0.1f),
            "gravity_change_begin" pairTo AnimationDef(3, duration = 0.1f, loop = false),
            "gravity_change_continue" pairTo AnimationDef(2, 2, 0.1f),
            "gravity_change_end" pairTo AnimationDef(),
            "jump" pairTo AnimationDef(),
            "jump_land" pairTo AnimationDef(2, duration = 0.1f, loop = false),
            "shoot_0" pairTo AnimationDef(2, 2, 0.1f, false),
            "shoot_1" pairTo AnimationDef(3, duration = 0.1f, loop = false),
            "shoot_2" pairTo AnimationDef(3, duration = 0.1f),
            "shoot_3" pairTo AnimationDef(2, duration = 0.1f, loop = false),
            "shoot_4" pairTo AnimationDef(3, duration = 0.1f),
            "shoot_5" pairTo AnimationDef(),
            "shoot_6" pairTo AnimationDef(2, 2, 0.1f, false),
            "throw_asteroids" pairTo AnimationDef(2, 2, duration = 0.1f),
            "stand" pairTo AnimationDef(2, 1, gdxArrayOf(0.3f, 0.1f))
        )

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
        })
    override lateinit var facing: Facing
    override var direction: Direction
        get() = body.direction
        set(value) {
            if (CHANGE_GRAVITY) body.direction = value
        }
    override var gravityScalar = DEFAULT_GRAVITY_SCALAR

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

    private val asteroidSpawnBounds = GameRectangle()
    private val asteroidsToThrow = Array<GamePair<Asteroid, Timer>>()
    private var asteroidsSpawned = 0

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)
            ANIM_DEFS.keys().forEach { key ->
                val atlasKey = "${TAG}${TAG_SUFFIX_FOR_ATLAS}/${key}"
                when {
                    key.contains("shoot") -> {
                        val index = key.split("_")[1].toInt()
                        val shootKey = "${TAG}${TAG_SUFFIX_FOR_ATLAS}/shoot"
                        val region = atlas.findRegion(shootKey, index)
                        regions.put(key, region)
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

        asteroidsSpawned = 0
        asteroidSpawnBounds.set(
            spawnProps.get(
                Asteroid.TAG.lowercase(),
                RectangleMapObject::class
            )!!.rectangle.toGameRectangle()
        )

        gravityChangeState = ProcessState.BEGIN

        val direction = megaman().direction
        this.direction = direction
        currentGravityChangeDir = direction

        gravityScalar =
            spawnProps.getOrDefault("${ConstKeys.GRAVITY}_${ConstKeys.SCALAR}", DEFAULT_GRAVITY_SCALAR, Float::class)
    }

    override fun isReady(delta: Float) = timers["init"].isFinished()

    override fun onReady() {
        GameLogger.debug(TAG, "onReady()")
        super.onReady()
        body.physics.gravityOn = true
    }

    override fun onDefeated(delta: Float) {
        GameLogger.debug(TAG, "onDefeated()")

        super.onDefeated(delta)

        asteroidsToThrow.forEach { it.first.destroy() }
        asteroidsToThrow.clear()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()

        asteroidsToThrow.forEach { it.first.destroy() }
        asteroidsToThrow.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (betweenReadyAndEndBossSpawnEvent) return@add

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

            val iter = asteroidsToThrow.iterator()
            while (iter.hasNext()) {
                val (asteroid, throwTimer) = iter.next()
                throwTimer.update(delta)
                if (throwTimer.isFinished()) {
                    val impulse = megaman().body.getCenter()
                        .sub(asteroid.body.getCenter())
                        .nor().scl(ASTEROID_SPEED * ConstVals.PPM)
                    asteroid.body.physics.velocity.set(impulse)
                    iter.remove()
                }
            }

            when (currentState) {
                MoonManState.INIT, MoonManState.STAND -> {
                    if (!body.isSensing(BodySense.FEET_ON_GROUND)) return@add

                    if (currentState == MoonManState.STAND && canShootInStandState() &&
                        shootIndex < STAND_SHOOT_DURS.size
                    ) {
                        val timer = timers["shoot_${shootIndex}"]
                        timer.update(delta)
                        if (timer.isFinished()) {
                            timer.reset()
                            shootIndex++
                        }
                        return@add
                    }

                    val timerKey = currentState.name.lowercase()
                    val timer = timers[timerKey]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }

                MoonManState.JUMP -> if (shouldGoToStandState()) stateMachine.next()
                MoonManState.THROW_ASTEROIDS -> {
                    if (asteroidsSpawned < ASTEROIDS_TO_SPAWN) {
                        val timer = timers["spawn_asteroid_delay"]
                        timer.update(delta)
                        if (timer.isFinished()) {
                            spawnAsteroid()
                            asteroidsSpawned++
                            timer.reset()
                        }
                    } else {
                        val timer = timers["spawn_asteroids_end"]
                        timer.update(delta)
                        if (timer.isFinished()) {
                            timer.reset()
                            stateMachine.next()
                        }
                    }
                }

                MoonManState.GRAVITY_CHANGE -> {
                    val timer = timers["gravity_change_${gravityChangeState.name.lowercase()}"]
                    timer.update(delta)
                    if (timer.isFinished()) {
                        timer.reset()

                        when (gravityChangeState) {
                            ProcessState.END -> {
                                gravityChangeState = ProcessState.BEGIN
                                direction = currentGravityChangeDir

                                val next = stateMachine.next()
                                GameLogger.debug(TAG, "update(): GRAVITY_CHANGE: end state, go to next=$next")
                            }

                            else -> {
                                GameLogger.debug(TAG, "update(): GRAVITY_CHANGE: current=$gravityChangeState")
                                val ordinal = gravityChangeState.ordinal + 1
                                gravityChangeState = ProcessState.entries[ordinal]
                                GameLogger.debug(TAG, "update(): GRAVITY_CHANGE: next=$gravityChangeState")

                                if (gravityChangeState == ProcessState.END) activateGravityChange()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.receiveFrictionY = false
        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
        body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.2f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -BODY_WIDTH * ConstVals.PPM / 2f
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        rightFixture.offsetFromBodyAttachment.x = BODY_WIDTH * ConstVals.PPM / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) {
                val velocity = body.physics.velocity
                when (direction) {
                    Direction.UP -> if (velocity.y > 0f) velocity.y = 0f
                    Direction.DOWN -> if (velocity.y < 0f) velocity.y = 0f
                    Direction.LEFT -> if (velocity.x < 0f) velocity.x = 0f
                    Direction.RIGHT -> if (velocity.x > 0f) velocity.x = 0f
                }
            }

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            val gravityVec = GameObjectPools.fetch(Vector2::class)
            when (direction) {
                Direction.UP -> gravityVec.set(0f, -gravity)
                Direction.DOWN -> gravityVec.set(0f, gravity)
                Direction.LEFT -> gravityVec.set(-gravity, 0f)
                Direction.RIGHT -> gravityVec.set(gravity, 0f)
            }.scl(ConstVals.PPM * gravityScalar)
            body.physics.gravity.set(gravityVec)

            body.physics.applyFrictionX = body.isSensing(BodySense.FEET_ON_GROUND)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            val position = DirectionPositionMapper.getInvertedPosition(direction)
            sprite.setPosition(body.getPositionPoint(position), position)

            val flipX = if (direction == Direction.UP) isFacing(Facing.LEFT) else isFacing(Facing.RIGHT)
            sprite.setFlip(flipX, false)

            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (defeated) "defeated" else when (currentState) {
                MoonManState.INIT -> if (body.isSensing(BodySense.FEET_ON_GROUND)) "gravity_change_begin" else "jump"
                MoonManState.STAND -> when {
                    body.isSensing(BodySense.FEET_ON_GROUND) ->
                        if (canShootInStandState()) "shoot_$shootIndex" else "stand"

                    else -> "jump"
                }

                MoonManState.GRAVITY_CHANGE -> "gravity_change_${gravityChangeState.name.lowercase()}"
                else -> currentState.name.lowercase()
            }
        }
        val animations = ObjectMap<String, IAnimation>()
        ANIM_DEFS.forEach {
            val key = it.key
            val def = it.value
            val region = regions[key]
            if (region == null) throw IllegalStateException("Region with key=$key is null")
            animations.put(key, Animation(region, def.rows, def.cols, def.durations, def.loop))
        }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun activateGravityChange() {
        currentGravityChangeDir = currentGravityChangeDir.getOpposite()
        if (!megaman().dead) megaman().direction = currentGravityChangeDir

        gravityChangeChance = 0f
        timers["gravity_change_delay"].reset()

        requestToPlaySound(SoundAsset.TIME_STOPPER_SOUND, false)

        GameLogger.debug(TAG, "activateGravityChange(): gravity=$currentGravityChangeDir")
    }

    private fun jump(target: Vector2) {
        var jumpImpulseY = JUMP_IMPULSE_Y * ConstVals.PPM
        if (direction == Direction.DOWN) jumpImpulseY *= -1f

        val yDiff = abs(megaman().body.getY() - body.getY())
        val horizontalScalar = (yDiff / (JUMP_HORIZONTAL_SCALAR_DENOMINATOR * ConstVals.PPM))
            .coerceIn(JUMP_MIN_HORIZONTAL_SCALAR, JUMP_MAX_HORIZONTAL_SCALAR)
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getCenter(),
            target,
            jumpImpulseY,
            horizontalScalar = horizontalScalar
        )
        impulse.x = impulse.x.coerceIn(-JUMP_MAX_IMPULSE_X * ConstVals.PPM, JUMP_MAX_IMPULSE_X * ConstVals.PPM)

        body.physics.velocity.set(impulse.x, impulse.y)
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
            MoonManState.STAND -> {
                timers["stand"].reset()
                if (canShootInStandState()) shootIndex = 0
            }

            MoonManState.JUMP -> {
                jump(megaman().body.getCenter())
                if (canShootInJumpState()) shootIndex = 0
            }

            MoonManState.THROW_ASTEROIDS -> {
                timers["spawn_asteroid_delay"].reset()
                asteroidsSpawned = 0
            }

            MoonManState.GRAVITY_CHANGE -> {
                ProcessState.entries.forEach {
                    val key = "gravity_change_${it.name.lowercase()}"
                    timers[key].reset()
                }
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when current=$current")
        }
    }

    private fun buildStateMachine(): StateMachine<MoonManState> {
        val builder = StateMachineBuilder<MoonManState>()
        MoonManState.entries.forEach { builder.state(it.name, it) }
        builder.setOnChangeState(this::onChangeState)
        builder.setTriggerChangeWhenSameElement(false)
        builder.initialState(MoonManState.INIT.name)
            .transition(MoonManState.INIT.name, MoonManState.STAND.name) { ready && shouldGoToStandState() }
            .transition(MoonManState.STAND.name, MoonManState.THROW_ASTEROIDS.name) { shouldEnterThrowAsteroidsState() }
            .transition(MoonManState.STAND.name, MoonManState.GRAVITY_CHANGE.name) { shouldEnterGravityChangeState() }
            .transition(MoonManState.STAND.name, MoonManState.JUMP.name) { body.isSensing(BodySense.FEET_ON_GROUND) }
            .transition(MoonManState.GRAVITY_CHANGE.name, MoonManState.STAND.name) { true }
            .transition(MoonManState.THROW_ASTEROIDS.name, MoonManState.STAND.name) { true }
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
        timers.put("spawn_asteroid_delay", Timer(SPAWN_ASTEROID_DELAY))
        timers.put("spawn_asteroids_end", Timer(ASTEROIDS_END_DUR))

        for (i in 0 until STAND_SHOOT_DURS.size) {
            val key = "shoot_$i"
            val dur = STAND_SHOOT_DURS[i]
            val timer = Timer(dur)

            timer.runOnFinished = when (i) {
                2 -> this::shootMoon
                4 -> this::shootStar
                else -> null
            }

            timers.put(key, timer)
        }
    }

    private fun updateFacing() {
        when {
            megaman().body.getMaxX() < body.getX() -> facing = Facing.LEFT
            megaman().body.getX() > body.getMaxX() -> facing = Facing.RIGHT
        }
    }

    private fun shouldEnterGravityChangeState(): Boolean {
        if (!canActivateGravityChange || shouldEnterThrowAsteroidsState()) return false
        val randomPercentage = getRandom(0f, 1f)
        return randomPercentage < gravityChangeChance
    }

    private fun shouldEnterThrowAsteroidsState() = (standIndex + 1) % 3 == 0

    private fun shouldGoToStandState() = body.isSensing(BodySense.FEET_ON_GROUND) &&
        (if (direction == Direction.UP) body.physics.velocity.y <= 0f else body.physics.velocity.y >= 0f)

    private fun canShootInStandState() = standIndex % 2 == 0

    private fun canShootInJumpState() = (jumpIndex + 1) % 2 == 0

    private fun shootMoon() {
        for (i in 0 until MOON_SCYTHE_DEG_OFFSETS.size) {
            val trajectory = GameObjectPools.fetch(Vector2::class).set(0f, MOON_SCYTHE_SPEED * ConstVals.PPM)

            var rotOffset = MOON_SCYTHE_DEG_OFFSETS[i] * facing.value
            if (direction == Direction.DOWN) rotOffset *= -1f
            val rotation = (if (isFacing(Facing.LEFT)) 90f else 270f) + rotOffset
            trajectory.rotateDeg(rotation)

            val scythe = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.MOON_SCYTHE)!!
            scythe.spawn(
                props(
                    ConstKeys.POSITION pairTo body.getCenter().add(0.5f * ConstVals.PPM * facing.value, 0f),
                    "${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}" pairTo MOON_SCYTHE_MOVEMENT_SCALAR,
                    ConstKeys.TRAJECTORY pairTo trajectory,
                    ConstKeys.ROTATION pairTo rotation,
                    ConstKeys.FADE pairTo false
                )
            )
        }

        requestToPlaySound(SoundAsset.WHIP_SOUND, false)
    }

    private fun shootStar() {
        val position = if (megaman().direction == Direction.UP) Position.TOP_CENTER else Position.BOTTOM_CENTER
        val trajectory =
            megaman().body.getPositionPoint(position).sub(body.getCenter()).nor().scl(SHARP_STAR_SPEED * ConstVals.PPM)
        val sharpStar = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SHARP_STAR)!!
        sharpStar.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter().add(0.5f * ConstVals.PPM * facing.value, 0f),
                "${ConstKeys.MOVEMENT}_${ConstKeys.SCALAR}" pairTo SHARP_STAR_MOVEMENT_SCALAR,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.ROTATION pairTo trajectory.angleDeg(),
            )
        )

        requestToPlaySound(SoundAsset.SOLAR_BLAZE_SOUND, false)
    }

    private fun spawnAsteroid() {
        val spawn = GameObjectPools.fetch(Vector2::class)
        spawn.x = getRandom(asteroidSpawnBounds.getMaxX(), asteroidSpawnBounds.getMaxX())
        spawn.y = getRandom(asteroidSpawnBounds.getY(), asteroidSpawnBounds.getMaxY())

        val asteroid = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ASTEROID)!! as Asteroid
        asteroid.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.DELAY pairTo THROW_ASTEROID_DELAY,
                ConstKeys.TYPE pairTo Asteroid.REGULAR
            )
        )

        asteroidsToThrow.add(asteroid pairTo Timer(THROW_ASTEROID_DELAY))
    }

    override fun getTag() = TAG
}
