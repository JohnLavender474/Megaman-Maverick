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
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.*
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class InfernoMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "InfernoMan"

        private const val SPRITE_SIZE = 3.5f

        private const val INIT_DUR = 1f
        private const val STAND_DUR = 1.25f
        private const val WALL_SLIDE_DUR = 0.75f
        private const val SHOOT_DUR = 0.25f
        private const val SHOOT_COOLDOWN_DUR = 0.5f
        private const val SHOOT_DELAY = 0.25f
        private const val MEGA_SHOOT_DUR = 1f
        private const val MEGA_SHOOT_TIME = 0.5f

        private const val FLAME_HEAD_DUR = 3f
        private const val FLAME_HEAD_SHOTS = 4
        private const val FLAME_HEAD_SHOOT_DELAY = 0.2f

        private const val BODY_WIDTH = 1.5f
        private const val BODY_HEIGHT = 1.75f
        private const val VEL_CLAMP_X = 50f
        private const val VEL_CLAMP_Y = 25f
        private const val GRAVITY = -0.15f
        private const val WALL_SLIDE_GRAVITY = -0.075f
        private const val GROUND_GRAVITY = -0.01f
        private const val DEFAULT_FRICTION_X = 6f
        private const val DEFAULT_FRICTION_Y = 1f
        private const val WALL_SLIDE_FRICTION_Y = 6f

        private const val JUMP_MAX_IMPULSE_X = 10f
        private const val JUMP_IMPULSE_Y = 16f

        private const val WALL_JUMP_IMPULSE_X = 5f

        private const val MEGAMAN_STRAIGHT_Y_THRESHOLD = 1f

        private const val ORB_SPEED = 15f
        private const val GOOP_SPEED = 10f
        private const val WAVE_SPEED = 12f
        private const val SPAWN_METEOR_DELAY = 1f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class InfernoManState { INIT, STAND, JUMP, WALLSLIDE, FLAMEHEAD }

    private enum class ShootMethod { STRAIGHT, UP, DOWN, MEGA }

    override lateinit var facing: Facing

    private val timers = ObjectMap<String, Timer>()
    private val shooting: Boolean
        get() = !timers["shoot"].isFinished()
    private val meteorSpawnDelays = Array<Timer>()

    private val meteorSpawnBounds = GameRectangle()
    private val meteorSpawners = ObjectMap<Int, GameRectangle>()
    private val poppedMeteorSpawners = ObjectMap<Int, GameRectangle>()
    private val randomMeteorKeys = Array<Int>()
    private var meteorCollideBlockId = 0

    private var shootMethod = ShootMethod.STRAIGHT

    private lateinit var stateMachine: StateMachine<InfernoManState>
    private val currentState: InfernoManState
        get() = stateMachine.getCurrent()

    private var stateIndex = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_2.source)
            InfernoManState.entries.forEach { state ->
                val key = state.name.lowercase()
                if (atlas.containsRegion("$TAG/$key")) regions.put(key, atlas.findRegion("$TAG/$key"))
            }
            gdxArrayOf(
                "jump_up",
                "jump_down",
                "jump_shoot_down",
                "jump_shoot_straight",
                "jump_shoot_up",
                "stand_shoot_mega",
                "stand_shoot_straight",
                "stand_shoot_up",
                "slide",
                "defeated"
            ).forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        body.physics.gravityOn = true
        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
        body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y

        updateFacing()

        stateMachine.reset()
        stateIndex = 0

        shootMethod = ShootMethod.STRAIGHT

        buildTimers()
        timers.forEach {
            if (it.key.equalsAny("shoot_cooldown", "shoot_delay")) it.value.setToEnd() else it.value.reset()
        }

        meteorSpawnBounds.set(
            spawnProps.get(
                ConstKeys.SPAWNER,
                RectangleMapObject::class
            )!!.rectangle.toGameRectangle()
        )
        val max = meteorSpawnBounds.getWidth().div(ConstVals.PPM.toFloat()).toInt()
        for (i in 0 until max) {
            val x = meteorSpawnBounds.getX() + i * ConstVals.PPM
            val y = meteorSpawnBounds.getY()

            val spawner = GameRectangle(x, y, ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
            meteorSpawners.put(i, spawner)

            randomMeteorKeys.add(i)
        }

        meteorCollideBlockId =
            spawnProps.get(ConstKeys.COLLIDE, RectangleMapObject::class)!!.properties.get(ConstKeys.ID, Int::class.java)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        meteorSpawners.clear()
        poppedMeteorSpawners.clear()
        randomMeteorKeys.clear()
        meteorSpawnDelays.clear()
    }

    override fun isReady(delta: Float) = timers["init"].isFinished()

    override fun onReady() {
        super.onReady()
        body.physics.gravityOn = true
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

            val meteorSpawnIter = meteorSpawnDelays.iterator()
            while (meteorSpawnIter.hasNext()) {
                val meteorSpawnDelay = meteorSpawnIter.next()
                meteorSpawnDelay.update(delta)
                if (meteorSpawnDelay.isFinished()) meteorSpawnIter.remove()
            }

            val shootTimer = timers["shoot"]

            val shootDelay = timers["shoot_delay"]
            shootDelay.update(delta)
            if (shootDelay.isFinished()) shootTimer.update(delta)

            val shootCooldown = timers["shoot_cooldown"]
            if (shootTimer.isJustFinished()) shootCooldown.reset()
            shootCooldown.update(delta)

            when (currentState) {
                InfernoManState.JUMP -> if (shouldFinishJumping()) stateMachine.next()
                else -> {
                    if (currentState == InfernoManState.INIT && !body.isSensing(BodySense.FEET_ON_GROUND)) return@add

                    if (currentState == InfernoManState.WALLSLIDE) {
                        body.physics.velocity.x = 0f
                        if (shouldGoToStandState()) {
                            stateMachine.next()
                            return@add
                        }
                    }

                    val stateTimer = timers[currentState.name.lowercase()]
                    stateTimer.update(delta)
                    if (stateTimer.isFinished() &&
                        shootTimer.isFinished() &&
                        shootCooldown.isFinished()
                    ) stateMachine.next()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.set(VEL_CLAMP_X * ConstVals.PPM, VEL_CLAMP_Y * ConstVals.PPM)
        body.physics.receiveFrictionX = false

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
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            body.physics.gravity.y = when {
                currentState == InfernoManState.WALLSLIDE -> WALL_SLIDE_GRAVITY * ConstVals.PPM
                body.isSensing(BodySense.FEET_ON_GROUND) -> GROUND_GRAVITY * ConstVals.PPM
                else -> GRAVITY * ConstVals.PPM
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            val flipX = when (currentState) {
                InfernoManState.WALLSLIDE -> body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
                else -> isFacing(Facing.LEFT)
            }
            sprite.setFlip(flipX, false)
            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animations = objectMapOf<String, IAnimation>(
            "init" pairTo Animation(regions["init"], 3, 3, 0.125f, false),
            "flamehead" pairTo Animation(regions["flamehead"], 2, 2, 0.1f, true),
            "jump_up" pairTo Animation(regions["jump_up"]),
            "jump_down" pairTo Animation(regions["jump_down"]),
            "jump_shoot_down" pairTo Animation(regions["jump_shoot_down"]),
            "jump_shoot_straight" pairTo Animation(regions["jump_shoot_straight"]),
            "jump_shoot_up" pairTo Animation(regions["jump_shoot_up"]),
            "wallslide" pairTo Animation(regions["wallslide"]),
            "slide" pairTo Animation(regions["slide"]),
            "stand" pairTo Animation(regions["stand"]),
            "stand_shoot_mega" pairTo Animation(regions["stand_shoot_mega"], 3, 2, 0.1f, false),
            "stand_shoot_straight" pairTo Animation(regions["stand_shoot_straight"]),
            // "stand_shoot_straight_akimbo" pairTo Animation(regions["stand_shoot_straight_akimbo"], 2, 1, 0.1f, true),
            "stand_shoot_up" pairTo Animation(regions["stand_shoot_up"]),
            "defeated" pairTo Animation(regions["defeated"], 3, 1, 0.1f, true)
        )
        val keySupplier: () -> String? = {
            if (defeated) "defeated" else when (currentState) {
                InfernoManState.INIT -> if (body.isSensing(BodySense.FEET_ON_GROUND)) "init" else "jump_down"

                InfernoManState.JUMP -> "jump" + when {
                    shooting -> "_shoot_${shootMethod.name.lowercase()}"
                    else -> "_${if (body.physics.velocity.y > 0f) "up" else "down"}"
                }

                InfernoManState.STAND -> "stand" + when {
                    shooting -> "_shoot_${shootMethod.name.lowercase()}"
                    else -> ""
                }

                else -> currentState.name.lowercase()
            }
        }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun buildTimers() {
        timers.put("init", Timer(INIT_DUR))
        timers.put("stand", Timer(STAND_DUR))
        timers.put("wallslide", Timer(WALL_SLIDE_DUR))
        timers.put("shoot", Timer())
        timers.put("shoot_cooldown", Timer(SHOOT_COOLDOWN_DUR))
        timers.put("shoot_delay", Timer(SHOOT_DELAY))

        val flameHeadTimer = Timer(FLAME_HEAD_DUR)
        val flameHeadRunnables = Array<TimeMarkedRunnable>()
        val flameHeadOffsetTime = FLAME_HEAD_DUR / FLAME_HEAD_SHOTS
        val randomIndex = getRandom(0, FLAME_HEAD_SHOTS - 1)
        for (i in 0 until FLAME_HEAD_SHOTS) {
            val time = FLAME_HEAD_SHOOT_DELAY + flameHeadOffsetTime * i
            val runnable = TimeMarkedRunnable(time) { launchOrb(i == randomIndex) }
            flameHeadRunnables.add(runnable)
        }
        flameHeadTimer.addRunnables(flameHeadRunnables)
        timers.put("flamehead", flameHeadTimer)
    }

    private fun buildStateMachine(): StateMachine<InfernoManState> {
        val builder = StateMachineBuilder<InfernoManState>()
        InfernoManState.entries.forEach { builder.state(it.name, it) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(InfernoManState.INIT.name)
            .transition(InfernoManState.INIT.name, InfernoManState.STAND.name) { ready }
            .transition(InfernoManState.STAND.name, InfernoManState.FLAMEHEAD.name) { stateIndex % 3 == 0 }
            .transition(InfernoManState.STAND.name, InfernoManState.JUMP.name) { true }
            .transition(InfernoManState.JUMP.name, InfernoManState.STAND.name) { shouldGoToStandState() }
            .transition(InfernoManState.JUMP.name, InfernoManState.WALLSLIDE.name) { isWallSliding() }
            .transition(InfernoManState.WALLSLIDE.name, InfernoManState.STAND.name) { shouldGoToStandState() }
            .transition(InfernoManState.WALLSLIDE.name, InfernoManState.JUMP.name) { true }
            .transition(InfernoManState.FLAMEHEAD.name, InfernoManState.STAND.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: InfernoManState, previous: InfernoManState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        when (previous) {
            InfernoManState.JUMP -> {
                body.physics.applyFrictionX = true
                timers["shoot_delay"].setToEnd()
            }

            InfernoManState.WALLSLIDE -> body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y
            InfernoManState.FLAMEHEAD -> {
                val iter = poppedMeteorSpawners.iterator()
                while (iter.hasNext) {
                    val entry = iter.next()

                    val key = entry.key
                    val value = entry.value

                    meteorSpawners.put(key, value)
                    randomMeteorKeys.add(key)

                    iter.remove()
                }
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when previous=$previous")
        }

        when (current) {
            InfernoManState.STAND -> {
                stateIndex++
                timers["stand"].reset()
                resetShootTimer()
            }

            InfernoManState.JUMP -> {
                body.physics.applyFrictionX = false
                jump(megaman.body.getCenter())

                if (previous == InfernoManState.WALLSLIDE) {
                    var impulseX = WALL_JUMP_IMPULSE_X * ConstVals.PPM
                    if (body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) impulseX *= -1f
                    body.physics.velocity.x = impulseX
                }

                resetShootTimer()
            }

            InfernoManState.FLAMEHEAD -> {
                timers["flamehead"].reset()
                randomMeteorKeys.shuffle()
            }

            InfernoManState.WALLSLIDE -> {
                timers["wallslide"].reset()
                body.physics.defaultFrictionOnSelf.y = WALL_SLIDE_FRICTION_Y
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when current=$current")
        }
    }

    private fun getShootMethod() = when (currentState) {
        InfernoManState.STAND -> {
            val random = getRandom(0f, 1f)
            when {
                isMegamanStraightAhead() -> when {
                    random <= 0.75f || !timers["shoot"].isFinished() -> ShootMethod.STRAIGHT
                    else -> ShootMethod.MEGA
                }

                random <= 0.25f -> ShootMethod.MEGA
                else -> ShootMethod.UP
            }
        }

        InfernoManState.JUMP -> when {
            megaman.body.getMaxY() < body.getY() -> ShootMethod.DOWN
            isMegamanStraightAhead() -> ShootMethod.STRAIGHT
            else -> ShootMethod.UP
        }

        else -> ShootMethod.STRAIGHT
    }

    private fun resetShootTimer() {
        val timer = timers["shoot"]
        timer.clearRunnables().setRunOnFirstupdate(null)

        shootMethod = getShootMethod()

        when (shootMethod) {
            ShootMethod.MEGA -> {
                timer.resetDuration(MEGA_SHOOT_DUR)
                val runnables = gdxArrayOf(TimeMarkedRunnable(MEGA_SHOOT_TIME) { shootWave() })
                timer.addRunnables(runnables)
            }

            else -> {
                timer.resetDuration(SHOOT_DUR).setRunOnFirstupdate { shootGoop() }
                if (currentState == InfernoManState.JUMP) timers["shoot_delay"].reset()
            }
        }
    }

    private fun shootWave() {
        val spawn = body.getPositionPoint(Position.BOTTOM_CENTER).add(0.75f * ConstVals.PPM * facing.value, 0f)

        val wave = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.MAGMA_WAVE)!!
        wave.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo Vector2(WAVE_SPEED * ConstVals.PPM * facing.value, 0f)
            )
        )

        requestToPlaySound(SoundAsset.ATOMIC_FIRE_SOUND, false)
    }

    private fun shootGoop() {
        val offsetX: Float
        val offsetY: Float
        val rotation: Float

        shootMethod = getShootMethod()

        when (shootMethod) {
            ShootMethod.UP -> {
                offsetX = 0.5f * ConstVals.PPM * facing.value
                offsetY = 0.5f * ConstVals.PPM
                rotation = if (isFacing(Facing.LEFT)) 45f else 315f
            }

            ShootMethod.DOWN -> {
                offsetX = 1.5f * ConstVals.PPM * facing.value
                offsetY = -0.25f * ConstVals.PPM
                rotation = if (isFacing(Facing.LEFT)) 135f else 225f
            }

            else -> {
                offsetX = ConstVals.PPM.toFloat() * facing.value
                offsetY = 0.25f * ConstVals.PPM
                rotation = if (isFacing(Facing.LEFT)) 90f else 270f
            }
        }

        val trajectory = Vector2(0f, GOOP_SPEED * ConstVals.PPM).rotateDeg(rotation)
        val spawn = body.getCenter().add(offsetX, offsetY)

        val goop = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.MAGMA_GOOP)!!
        goop.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.ROTATION pairTo rotation
            )
        )

        requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)
    }

    private fun launchOrb(targetMegaman: Boolean) {
        setMeteorToBeSpawned(targetMegaman)

        val spawn = body.getPositionPoint(Position.TOP_CENTER).add(0f, 0.25f * ConstVals.PPM)
        val trajectory = GameObjectPools.fetch(Vector2::class).set(0f, ORB_SPEED * ConstVals.PPM)

        val orb = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.MAGMA_ORB)!!
        orb.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.TRAJECTORY pairTo trajectory))

        requestToPlaySound(SoundAsset.WHIP_SOUND, false)
    }

    private fun setMeteorToBeSpawned(targetMegaman: Boolean) {
        val spawnMeteorTimer = Timer(SPAWN_METEOR_DELAY)
        spawnMeteorTimer.setRunOnJustFinished { spawnMeteor(targetMegaman) }
        meteorSpawnDelays.add(spawnMeteorTimer)
    }

    private fun spawnMeteor(targetMegaman: Boolean) {
        var x = when {
            targetMegaman -> megaman.body.getCenter().x
            else -> {
                val poppedKey = randomMeteorKeys.pop()
                val popped = meteorSpawners.remove(poppedKey)
                poppedMeteorSpawners.put(poppedKey, popped)
                popped.getCenter().x
            }
        }
        val spawn = GameObjectPools.fetch(Vector2::class).set(x, meteorSpawnBounds.getY())

        val floor = MegaGameEntities.getOfMapObjectId(meteorCollideBlockId).first() as Block

        val meteor = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.MAGMA_METEOR)!!
        meteor.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.DIRECTION pairTo ConstKeys.DOWN,
                "${ConstKeys.COLLIDE}_${ConstKeys.BODIES}" pairTo floor
            )
        )
    }

    private fun jump(target: Vector2): Vector2 {
        val impulse = MegaUtilMethods.calculateJumpImpulse(body.getCenter(), target, JUMP_IMPULSE_Y * ConstVals.PPM)
        impulse.x = impulse.x.coerceIn(-JUMP_MAX_IMPULSE_X * ConstVals.PPM, JUMP_MAX_IMPULSE_X * ConstVals.PPM)
        body.physics.velocity.set(impulse.x, impulse.y)
        return impulse
    }

    private fun shouldGoToStandState() = body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND)

    private fun isHittingAgainstLeftWall() =
        body.physics.velocity.x <= 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)

    private fun isHittingAgainstRightWall() =
        body.physics.velocity.x >= 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)

    private fun isWallSliding() = body.physics.velocity.y <= 0f && !body.isSensing(BodySense.FEET_ON_GROUND) &&
        (isHittingAgainstLeftWall() || isHittingAgainstRightWall())

    private fun shouldFinishJumping() = isWallSliding() || shouldGoToStandState()

    private fun isMegamanStraightAhead() =
        abs(megaman.body.getY() - body.getY()) <= MEGAMAN_STRAIGHT_Y_THRESHOLD * ConstVals.PPM

    private fun updateFacing() {
        when {
            megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
            megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
        }
    }
}
