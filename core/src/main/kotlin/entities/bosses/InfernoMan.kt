package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
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
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.getRandom
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntitiesMap
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs
import kotlin.reflect.KClass

class InfernoMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "InfernoMan"

        private const val SPRITE_SIZE = 3f

        private const val INIT_DUR = 1.5f
        private const val STAND_DUR = 1.5f
        private const val WALL_SLIDE_DUR = 0.75f
        private const val SHOOT_DUR = 0.25f
        private const val SHOOT_COOLDOWN_DUR = 0.5f
        private const val SHOOT_DELAY = 0.25f
        private const val MEGA_SHOOT_DUR = 1f
        private const val MEGA_SHOOT_TIME = 0.5f

        private const val FLAME_HEAD_DUR = 3f
        private const val FLAME_HEAD_SHOTS = 4
        private const val FLAME_HEAD_SHOOT_DELAY = 0.2f

        private const val BODY_WIDTH = 1.15f
        private const val BODY_HEIGHT = 1.5f
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

        private const val MEGAMAN_STRAIGHT_Y_THRESHOLD = 1f

        private const val ORB_SPEED = 15f
        private const val GOOP_SPEED = 10f
        private const val WAVE_SPEED = 12f
        private const val METEOR_SPEED = 10f
        private const val SPAWN_METEOR_DELAY = 1f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class InfernoManState { INIT, STAND, JUMP, WALL_SLIDE, FLAME_HEAD }
    private enum class ShootMethod { STRAIGHT, UP, DOWN, MEGA }

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

    private val timers = ObjectMap<String, Timer>()
    private val shooting: Boolean
        get() = !timers["shoot"].isFinished()
    private val meteorSpawnDelays = Array<Timer>()

    private lateinit var meteorSpawner: GameRectangle
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
                var key = state.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
            gdxArrayOf(
                "jump_shoot_down",
                "jump_shoot_straight",
                "jump_shoot_up",
                "stand_shoot_mega",
                "stand_shoot_straight",
                "stand_shoot_straight_akimbo",
                "stand_shoot_up",
                "slide",
                "wall_slide",
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

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
        body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y

        updateFacing()

        stateMachine.reset()
        stateIndex = 0

        shootMethod = ShootMethod.STRAIGHT

        buildTimers()
        timers.forEach {
            if (it.key.equalsAny("shoot_cooldown", "shoot_delay")) it.value.setToEnd()
            else it.value.reset()
        }

        meteorSpawner = spawnProps.get(ConstKeys.SPAWNER, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        meteorCollideBlockId =
            spawnProps.get(ConstKeys.COLLIDE, RectangleMapObject::class)!!
                .properties.get(ConstKeys.ID, Int::class.java)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        meteorSpawnDelays.clear()
    }

    override fun onReady() {
        super.onReady()
        body.physics.gravityOn = true
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

                    if (currentState == InfernoManState.WALL_SLIDE) {
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
        body.color = Color.GRAY

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

            body.physics.gravity.y = when {
                currentState == InfernoManState.WALL_SLIDE -> WALL_SLIDE_GRAVITY * ConstVals.PPM
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
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            val flipX = when (currentState) {
                InfernoManState.WALL_SLIDE -> body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
                else -> isFacing(Facing.LEFT)
            }
            sprite.setFlip(flipX, false)
            sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animations = objectMapOf<String, IAnimation>(
            "init" pairTo Animation(regions["init"], 3, 3, 0.1f, false),
            "flame_head" pairTo Animation(regions["flame_head"], 2, 2, 0.1f, true),
            "jump" pairTo Animation(regions["jump"]),
            "jump_shoot_down" pairTo Animation(regions["jump_shoot_down"], 2, 1, 0.1f, false),
            "jump_shoot_straight" pairTo Animation(regions["jump_shoot_straight"], 2, 1, 0.1f, false),
            "jump_shoot_up" pairTo Animation(regions["jump_shoot_up"], 2, 1, 0.1f, false),
            "wall_slide" pairTo Animation(regions["wall_slide"]),
            "slide" pairTo Animation(regions["slide"]),
            "stand" pairTo Animation(regions["stand"]),
            "stand_shoot_mega" pairTo Animation(regions["stand_shoot_mega"], 3, 2, 0.1f, false),
            "stand_shoot_straight" pairTo Animation(regions["stand_shoot_straight"], 2, 1, 0.1f, false),
            "stand_shoot_straight_akimbo" pairTo Animation(regions["stand_shoot_straight_akimbo"], 2, 1, 0.1f, true),
            "stand_shoot_up" pairTo Animation(regions["stand_shoot_up"], 2, 1, 0.1f, false),
            "defeated" pairTo Animation(regions["defeated"], 3, 1, 0.1f, true)
        )
        val keySupplier: () -> String? = {
            if (defeated) "defeated" else when (currentState) {
                InfernoManState.INIT -> if (body.isSensing(BodySense.FEET_ON_GROUND)) "init" else "jump"
                else -> {
                    var key = currentState.name.lowercase()
                    if (currentState.equalsAny(InfernoManState.STAND, InfernoManState.JUMP) && shooting) {
                        val temp = key + "_shoot_${shootMethod.name.lowercase()}"
                        if (animations.containsKey(temp)) key = temp
                    }
                    key
                }
            }
        }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun buildTimers() {
        timers.put("init", Timer(INIT_DUR))
        timers.put("stand", Timer(STAND_DUR))
        timers.put("wall_slide", Timer(WALL_SLIDE_DUR))
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
        flameHeadTimer.setRunnables(flameHeadRunnables)
        timers.put("flame_head", flameHeadTimer)
    }

    private fun buildStateMachine(): StateMachine<InfernoManState> {
        val builder = StateMachineBuilder<InfernoManState>()
        InfernoManState.entries.forEach { builder.state(it.name, it) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(InfernoManState.INIT.name)
            .transition(InfernoManState.INIT.name, InfernoManState.STAND.name) { true }
            .transition(InfernoManState.STAND.name, InfernoManState.FLAME_HEAD.name) { stateIndex % 3 == 0 }
            .transition(InfernoManState.STAND.name, InfernoManState.JUMP.name) { true }
            .transition(InfernoManState.JUMP.name, InfernoManState.STAND.name) { shouldGoToStandState() }
            .transition(InfernoManState.JUMP.name, InfernoManState.WALL_SLIDE.name) { isWallSliding() }
            .transition(InfernoManState.WALL_SLIDE.name, InfernoManState.STAND.name) { shouldGoToStandState() }
            .transition(InfernoManState.WALL_SLIDE.name, InfernoManState.JUMP.name) { true }
            .transition(InfernoManState.FLAME_HEAD.name, InfernoManState.STAND.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: InfernoManState, previous: InfernoManState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        when (previous) {
            InfernoManState.JUMP -> {
                body.physics.applyFrictionX = true
                timers["shoot_delay"].setToEnd()
            }

            InfernoManState.WALL_SLIDE -> body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y
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
                jump(getMegaman().body.getCenter())
                resetShootTimer()
            }

            InfernoManState.FLAME_HEAD -> timers["flame_head"].reset()
            InfernoManState.WALL_SLIDE -> {
                timers["wall_slide"].reset()
                body.physics.defaultFrictionOnSelf.y = WALL_SLIDE_FRICTION_Y
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when current=$current")
        }
    }

    private fun getShootMethod() =
        when (currentState) {
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
                getMegaman().body.getMaxY() < body.y -> ShootMethod.DOWN
                isMegamanStraightAhead() -> ShootMethod.STRAIGHT
                else -> ShootMethod.UP
            }

            else -> throw IllegalStateException("Cannot set shoot method when state=$currentState")
        }

    private fun resetShootTimer() {
        val timer = timers["shoot"]
        timer.clearRunnables()
        timer.runOnFirstUpdate = null
        shootMethod = getShootMethod()
        when (shootMethod) {
            ShootMethod.MEGA -> {
                timer.resetDuration(MEGA_SHOOT_DUR)
                val runnables = gdxArrayOf(TimeMarkedRunnable(MEGA_SHOOT_TIME) { shootWave() })
                timer.setRunnables(runnables)
            }

            else -> {
                timer.resetDuration(SHOOT_DUR)
                timer.runOnFirstUpdate = { shootGoop() }
                if (currentState == InfernoManState.JUMP) timers["shoot_delay"].reset()
            }
        }
    }

    private fun shootWave() {
        val spawn = body.getBottomCenterPoint().add(0.75f * ConstVals.PPM * facing.value, 0f)
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
                offsetX = 0.65f * ConstVals.PPM * facing.value
                offsetY = 0.4f * ConstVals.PPM
                rotation = if (isFacing(Facing.LEFT)) 45f else 315f
            }

            ShootMethod.DOWN -> {
                offsetX = 0.85f * ConstVals.PPM * facing.value
                offsetY = -0.2f * ConstVals.PPM
                rotation = if (isFacing(Facing.LEFT)) 135f else 225f
            }

            else -> {
                offsetX = 0.75f * ConstVals.PPM * facing.value
                offsetY = 0.2f * ConstVals.PPM
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
        val spawn = body.getTopCenterPoint().add(0.1f * ConstVals.PPM * facing.value, 0.1f * ConstVals.PPM)
        val trajectory = Vector2(0f, ORB_SPEED * ConstVals.PPM)
        val orb = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.MAGMA_ORB)!!
        orb.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.TRAJECTORY pairTo trajectory))
        requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    private fun setMeteorToBeSpawned(targetMegaman: Boolean) {
        val spawnMeteorTimer = Timer(SPAWN_METEOR_DELAY)
        spawnMeteorTimer.runOnFinished = { spawnMeteor(targetMegaman) }
        meteorSpawnDelays.add(spawnMeteorTimer)
    }

    private fun spawnMeteor(targetMegaman: Boolean) {
        var x = when {
            targetMegaman -> getMegaman().body.getCenter().x
            else -> getRandom(meteorSpawner.x, meteorSpawner.getMaxX())
        }
        x = x.coerceIn(meteorSpawner.x, meteorSpawner.getMaxX())
        val spawn = Vector2(x, meteorSpawner.y)
        val trajectory = Vector2(0f, -METEOR_SPEED * ConstVals.PPM)
        val floor = MegaGameEntitiesMap.getEntitiesOfMapObjectId(meteorCollideBlockId).first() as Block
        val meteor = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.MAGMA_METEOR)!!
        meteor.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
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
        abs(getMegaman().body.y - body.y) <= MEGAMAN_STRAIGHT_Y_THRESHOLD * ConstVals.PPM

    private fun updateFacing() {
        when {
            getMegaman().body.getMaxX() < body.x -> facing = Facing.LEFT
            getMegaman().body.x > body.getMaxX() -> facing = Facing.RIGHT
        }
    }
}