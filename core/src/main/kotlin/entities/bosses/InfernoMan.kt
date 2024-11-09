package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
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
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
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
        private const val STAND_DUR = 0.75f
        private const val WALL_SLIDE_DUR = 0.5f
        private const val SHOOT_DUR = 0.25f
        private const val SHOOT_AKIMBO_DUR = 1f
        private const val FLAME_HEAD_DUR = 2f
        private const val MEGA_SHOOT_DUR = 1f
        private const val MEGA_SHOOT_TIME = 0.5f

        private const val BODY_WIDTH = 1.15f
        private const val BODY_HEIGHT = 1.5f
        private const val VEL_CLAMP_X = 50f
        private const val VEL_CLAMP_Y = 25f
        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f
        private const val DEFAULT_FRICTION_X = 6f
        private const val DEFAULT_FRICTION_Y = 1f
        private const val WALL_SLIDE_FRICTION_Y = 6f

        private const val JUMP_MAX_IMPULSE_X = 10f
        private const val JUMP_IMPULSE_Y = 16f

        private const val AKIMBO_SHOTS = 10
        private const val MEGAMAN_STRAIGHT_Y_THRESHOLD = 1.5f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class InfernoManState { INIT, STAND, JUMP, WALL_SLIDE, FLAME_HEAD }
    private enum class ShootMethod { STRAIGHT, STRAIGHT_AKIMBO, UP, DOWN, MEGA }
    private enum class ShootType { ORB, METEOR, SPLASHY, WAVE }

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
    private lateinit var shootMethod: ShootMethod
    private lateinit var shootType: ShootType
    private lateinit var stateMachine: StateMachine<InfernoManState>
    private val currentState: InfernoManState
        get() = stateMachine.getCurrent()
    private var previousState: InfernoManState? = null
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
                "wall_slide"
            ).forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
        buildTimers()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
        body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y

        stateMachine.reset()
        previousState = null
        timers.forEach { if (it.key == "shoot") it.value.setToEnd() else it.value.reset() }
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        shootMethod = ShootMethod.STRAIGHT
        shootType = ShootType.ORB
        stateIndex = 0
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
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

            val shootTimer = timers["shoot"]
            shootTimer.update(delta)

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
                    if (stateTimer.isFinished() && shootTimer.isFinished()) stateMachine.next()
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
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyCenter.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(feetFixture)
        feetFixture.rawShape.color = Color.GREEN
        debugShapes.add { feetFixture.getShape() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
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
                currentState == InfernoManState.WALL_SLIDE -> 0f
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
            val flipX =
                if (currentState == InfernoManState.WALL_SLIDE) body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)
                else isFacing(Facing.LEFT)
            sprite.setFlip(flipX, false)
            sprite.hidden = damageBlink || !ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (currentState) {
                InfernoManState.INIT -> if (body.isSensing(BodySense.FEET_ON_GROUND)) "init" else "jump"
                else -> {
                    var key = currentState.name.lowercase()
                    if (currentState.equalsAny(InfernoManState.STAND, InfernoManState.JUMP) && shooting)
                        key += "_shoot_${shootMethod.name.lowercase()}"
                    key
                }
            }
        }
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
            "stand_shoot_up" pairTo Animation(regions["stand_shoot_up"], 2, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun buildTimers() {
        timers.put("init", Timer(INIT_DUR))
        timers.put("stand", Timer(STAND_DUR))
        timers.put("wall_slide", Timer(WALL_SLIDE_DUR))
        timers.put("flame_head", Timer(FLAME_HEAD_DUR))
        timers.put("shoot", Timer())
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
            InfernoManState.JUMP -> body.physics.applyFrictionX = true
            InfernoManState.WALL_SLIDE -> body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y
            else -> GameLogger.debug(TAG, "onChangeState(): no action when previous=$previous")
        }

        when (current) {
            InfernoManState.STAND -> {
                stateIndex++
                timers["stand"].reset()
                setShootMethod()
                resetShootTimer()
            }

            InfernoManState.JUMP -> {
                setShootMethod()
                resetShootTimer()
                val impulse = jump(getMegaman().body.getCenter())
                facing = if (impulse.x < 0f) Facing.LEFT else Facing.RIGHT
                body.physics.applyFrictionX = false
            }

            InfernoManState.FLAME_HEAD -> timers["flame_head"].reset()
            InfernoManState.WALL_SLIDE -> {
                timers["wall_slide"].reset()
                body.physics.defaultFrictionOnSelf.y = WALL_SLIDE_FRICTION_Y
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no action when current=$current")
        }
    }

    private fun setShootMethod() {
        // TODO
        /*
        val oldShootMethod = shootMethod
        shootMethod = when (currentState) {
            InfernoManState.STAND -> {
                val random = getRandom(0f, 1f)
                when {
                    isMegamanStraightAhead() ->
                        if (random < 0.6f || !timers["shoot"].isFinished()) ShootMethod.STRAIGHT
                        else if (random < 0.85f) ShootMethod.STRAIGHT_AKIMBO
                        else ShootMethod.MEGA

                    else -> ShootMethod.UP
                }
            }

            InfernoManState.JUMP -> when {
                isMegamanStraightAhead() -> ShootMethod.STRAIGHT
                getMegaman().body.getMaxY() < body.y -> ShootMethod.DOWN
                else -> ShootMethod.UP
            }

            else -> throw IllegalStateException("Cannot set shoot method when state=$currentState")
        }
        GameLogger.debug(TAG, "setShootMethod(): new=$shootMethod, old=$oldShootMethod")
         */
    }

    private fun setShootType() {

    }

    private fun resetShootTimer() {
        timers["shoot"].resetDuration(SHOOT_DUR)
        // TODO
        /*
        val timer = timers["shoot"]
        timer.clearRunnables()
        when (shootMethod) {
            ShootMethod.STRAIGHT_AKIMBO -> {
                timer.resetDuration(SHOOT_AKIMBO_DUR)
                val runnables = Array<TimeMarkedRunnable>()
                for (i in 0..AKIMBO_SHOTS) {
                    val time = i * (SHOOT_AKIMBO_DUR / AKIMBO_SHOTS)
                    val runnable = TimeMarkedRunnable(time) { shoot() }
                    runnables.add(runnable)
                }
                timer.setRunnables(runnables)
                timer.runOnFirstUpdate = null
            }

            ShootMethod.MEGA -> {
                timer.resetDuration(MEGA_SHOOT_DUR)
                val runnables = gdxArrayOf(TimeMarkedRunnable(MEGA_SHOOT_TIME) { shoot() })
                timer.setRunnables(runnables)
            }

            else -> {
                timer.resetDuration(SHOOT_DUR)
                timer.runOnFirstUpdate = { shoot() }
            }
        }
         */
    }

    private fun shoot() {
        val spawn: Vector2
        val trajectory: Vector2
        val projectile: MegaGameEntity
        when (shootMethod) {
            ShootMethod.STRAIGHT -> TODO()
            ShootMethod.STRAIGHT_AKIMBO -> TODO()
            ShootMethod.UP -> TODO()
            ShootMethod.DOWN -> TODO()
            ShootMethod.MEGA -> TODO()
        }
        // TODO
        /*
        projectile.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )
         */
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
        if (getMegaman().body.getMaxX() < body.x) facing = Facing.LEFT
        else if (getMegaman().body.x > body.getMaxX()) facing = Facing.RIGHT
    }
}
