package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
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
import com.mega.game.engine.common.extensions.*
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

/*
- From stand:
    - if (abs(x - megaman.x) >= threshold) --> run
    - if random and last "from stand" states aren't throw --> throw gems
    - else --> jump
- From run:
    - after x time or hit wall, if megaman on ground --> ground slide, else --> jump
- From ground slide:
    - after x time or hit wall, stop, then after short delay --> jump
- From jump:
    - if hit and facing wall --> wall slide
    - if megaman in scanner area --> air punch
    - after short delay --> throw gems
    - when land on feet --> stand
 */
class PreciousWoman(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "PreciousWoman"

        private const val BODY_WIDTH = 1.5f
        private const val BODY_HEIGHT = 2f

        private const val RUN_IMPULSE_X = 30f
        private const val MAX_RUN_SPEED = 12f

        private const val VEL_CLAMP_X = 50f
        private const val VEL_CLAMP_Y = 25f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val DEFAULT_FRICTION_X = 6f
        private const val DEFAULT_FRICTION_Y = 1.015f

        private const val SPRITE_SIZE = 3f

        private const val INIT_DUR = 1f
        private const val STAND_DUR = 1.5f
        private const val LAUGH_1_DUR = 0.2f
        private const val LAUGH_2_DUR = 0.8f
        private const val THROW_DUR = 0.5f
        private const val THROW_TIME = 0.25f
        private const val AIRPUNCH_DELAY = 0.25f
        private const val AIR_PUNCH_DUR = 1f

        private const val AIRPUNCH_VEL_X = 12f

        private const val THROW_SPEED = 10f
        private val THROW_OFFSETS = gdxArrayOf(Vector2(2f, 2f), Vector2(2f, 0f), Vector2(2f, -2f))

        // the amount of times Precious Woman should enter stand/throw state before throwing gems
        private const val STATES_BETWEEN_THROW = 3
        private const val MIN_THROW_COOLDOWN = 2f

        private val animDefs = orderedMapOf(
            "airpunch1" pairTo AnimationDef(3, 1, 0.1f, false),
            "airpunch2" pairTo AnimationDef(2, 1, 0.1f, true),
            "damaged" pairTo AnimationDef(2, 2, 0.1f, true),
            "defeated" pairTo AnimationDef(2, 2, 0.1f, true),
            "groundslide" pairTo AnimationDef(),
            "jump" pairTo AnimationDef(),
            "jump_throw" pairTo AnimationDef(),
            "run" pairTo AnimationDef(2, 2, 0.1f, true),
            "stand" pairTo AnimationDef(2, 1, gdxArrayOf(1f, 0.15f), true),
            "stand_laugh1" pairTo AnimationDef(2, 1, 0.1f, false),
            "stand_laugh2" pairTo AnimationDef(2, 1, 0.1f, true),
            "stand_throw" pairTo AnimationDef(),
            "wallslide" pairTo AnimationDef(),
            "wink" pairTo AnimationDef(3, 3, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class PreciousWomanState { INIT, STAND, RUN, JUMP, GROUNDSLIDE, WALLSLIDE, AIRPUNCH }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<PreciousWomanState>
    private val currentState: PreciousWomanState
        get() = stateMachine.getCurrent()
    private val stateTimers = OrderedMap<PreciousWomanState, Timer>()

    private val laughTimer = Timer(LAUGH_1_DUR + LAUGH_2_DUR)
    private val laughing: Boolean
        get() = !laughTimer.isFinished()

    private val throwingTimer = Timer(THROW_DUR).setRunnables(TimeMarkedRunnable(THROW_TIME) { throwGems() })
    private val throwing: Boolean
        get() = !throwingTimer.isFinished()
    private val throwMinCooldown = Timer(MIN_THROW_COOLDOWN)

    private var statesSinceLastThrow = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }

        if (stateTimers.isEmpty) stateTimers.putAll(
            PreciousWomanState.INIT pairTo Timer(INIT_DUR),
            PreciousWomanState.STAND pairTo Timer(STAND_DUR),
            PreciousWomanState.AIRPUNCH pairTo Timer(AIRPUNCH_DELAY + AIR_PUNCH_DUR)
        )

        super.init()

        stateMachine = buildStateMachine()

        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(position)

        stateMachine.reset()

        stateTimers.values().forEach { timer -> timer.reset() }

        laughTimer.setToEnd()
        throwingTimer.setToEnd()

        updateFacing()

        statesSinceLastThrow = 0
    }

    override fun isReady(delta: Float) = stateTimers[PreciousWomanState.INIT].isFinished()

    override fun onReady() {
        GameLogger.debug(TAG, "onReady()")

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

            if (!throwingTimer.isFinished()) throwingTimer.update(delta)
            if (!throwMinCooldown.isFinished()) throwMinCooldown.update(delta)
            if (!laughTimer.isFinished()) laughTimer.update(delta)

            if (stateTimers.containsKey(currentState)) {
                val stateTimer = stateTimers[currentState]
                if (shouldUpdateStateTimer()) stateTimer.update(delta)
                if (stateTimer.isFinished()) stateMachine.next()
            }

            when (currentState) {
                PreciousWomanState.AIRPUNCH -> {
                    val time = stateTimers[PreciousWomanState.AIRPUNCH].time
                    body.physics.velocity.x = when {
                        time <= AIRPUNCH_DELAY -> 0f
                        else -> AIRPUNCH_VEL_X * ConstVals.PPM * facing.value
                    }
                    body.physics.velocity.y = 0f
                }

                PreciousWomanState.INIT, PreciousWomanState.STAND -> {
                    updateFacing()

                    if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.x = 0f
                }

                PreciousWomanState.RUN -> {
                    updateFacing()

                    body.physics.velocity.let { velocity ->
                        if ((isFacing(Facing.LEFT) && velocity.x > 0f) || (isFacing(Facing.RIGHT) && velocity.x < 0f))
                            velocity.x = 0f

                        if (abs(velocity.x) < MAX_RUN_SPEED * ConstVals.PPM)
                            velocity.x += RUN_IMPULSE_X * ConstVals.PPM * facing.value * delta

                        velocity.x = velocity.x.coerceIn(-MAX_RUN_SPEED * ConstVals.PPM, MAX_RUN_SPEED * ConstVals.PPM)

                        if (velocity.y > 0f) velocity.y = 0f
                    }
                }

                PreciousWomanState.JUMP -> if (shouldEndJump()) stateMachine.next()
                PreciousWomanState.WALLSLIDE -> if (shouldEndWallslide()) stateMachine.next()
                PreciousWomanState.GROUNDSLIDE -> if (shouldEndGroundslide()) stateMachine.next()
            }
        }
    }

    private fun shouldUpdateStateTimer() = when (currentState) {
        PreciousWomanState.INIT -> body.isSensing(BodySense.FEET_ON_GROUND)
        PreciousWomanState.STAND -> !laughing && !throwing
        else -> true
    }

    private fun throwGems() {
        THROW_OFFSETS.forEach { offset ->
            val spawn = body.getCenter().add(0.5f * ConstVals.PPM * facing.value, 0.25f * ConstVals.PPM)

            val target = GameObjectPools.fetch(Vector2::class)
                .set(spawn)
                .add(offset.x * ConstVals.PPM * facing.value, offset.y * ConstVals.PPM)

            val gem = MegaEntityFactory.fetch(Bullet::class)!! // TODO: replace with gem
            gem.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.TARGET pairTo target,
                    ConstKeys.SPEED pairTo THROW_SPEED
                )
            )
        }

        throwingTimer.reset()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.receiveFrictionY = false
        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
        body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.set(VEL_CLAMP_X, VEL_CLAMP_Y).scl(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
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

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.setFlip(isFacing(Facing.LEFT), false)

            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        defeated -> "defeated"

                        !ready || betweenReadyAndEndBossSpawnEvent -> when {
                            !body.isSensing(BodySense.FEET_ON_GROUND) -> "jump"
                            else -> "wink"
                        }

                        else -> when (currentState) {
                            PreciousWomanState.STAND -> when {
                                laughing -> "stand${if (laughTimer.time <= LAUGH_1_DUR) "_laugh1" else "_laugh2"}"
                                throwing -> "stand_throw"
                                else -> "stand"
                            }

                            PreciousWomanState.JUMP -> when {
                                throwing -> "jump_throw"
                                else -> "jump"
                            }

                            PreciousWomanState.RUN -> "run"

                            PreciousWomanState.WALLSLIDE -> "wallslide"

                            PreciousWomanState.GROUNDSLIDE -> "groundslide"

                            else -> null
                        }
                    }
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val def = entry.value
                        try {
                            val animation = Animation(regions[key], def.rows, def.cols, def.durations, def.loop)
                            animations.put(key, animation)
                        } catch (e: Exception) {
                            throw IllegalStateException("Failed to create animation for region $key and def $def", e)
                        }
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = StateMachineBuilder<PreciousWomanState>()
        .states { states -> PreciousWomanState.entries.forEach { state -> states.put(state.name, state) } }
        .initialState(PreciousWomanState.INIT.name)
        .setOnChangeState(this::onChangeState)
        // init
        .transition(PreciousWomanState.INIT.name, PreciousWomanState.STAND.name) { true }
        // stand
        .transition(PreciousWomanState.STAND.name, PreciousWomanState.RUN.name) { true }
        // run
        .transition(PreciousWomanState.RUN.name, PreciousWomanState.JUMP.name) { true }
        // jump
        .transition(PreciousWomanState.JUMP.name, PreciousWomanState.WALLSLIDE.name) { shouldStartWallSliding() }
        .transition(PreciousWomanState.JUMP.name, PreciousWomanState.AIRPUNCH.name) { shouldAirPunch() }
        .transition(PreciousWomanState.JUMP.name, PreciousWomanState.STAND.name) { shouldGoFromJumpToStand() }
        // wallslide
        .transition(PreciousWomanState.WALLSLIDE.name, PreciousWomanState.JUMP.name) {
            !body.isSensing(BodySense.FEET_ON_GROUND)
        }
        .transition(PreciousWomanState.WALLSLIDE.name, PreciousWomanState.STAND.name) { true }
        // air punch
        .transition(PreciousWomanState.AIRPUNCH.name, PreciousWomanState.JUMP.name) {
            !body.isSensing(BodySense.FEET_ON_GROUND)
        }
        .transition(PreciousWomanState.AIRPUNCH.name, PreciousWomanState.STAND.name) { true }
        // build
        .build()

    private fun onChangeState(current: PreciousWomanState, previous: PreciousWomanState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        if (current.equalsAny(PreciousWomanState.STAND, PreciousWomanState.JUMP)) {
            statesSinceLastThrow++

            if (throwMinCooldown.isFinished() && statesSinceLastThrow >= STATES_BETWEEN_THROW) {
                throwingTimer.reset()
                throwMinCooldown.reset()
                statesSinceLastThrow = 0
            }
        }

        stateTimers[current]?.reset()
    }

    private fun shouldAirPunch() = false // TODO

    private fun shouldEndJump() = shouldGoFromJumpToStand() || shouldStartWallSliding() || shouldAirPunch()

    private fun shouldGoFromJumpToStand() = body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f

    private fun shouldStartWallSliding() = !body.isSensing(BodySense.FEET_ON_GROUND) &&
        ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)))

    private fun shouldEndGroundslide() =
        (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

    private fun shouldEndWallslide() =
        !body.isSensingAny(BodySense.SIDE_TOUCHING_BLOCK_LEFT, BodySense.SIDE_TOUCHING_BLOCK_RIGHT) ||
            body.isSensing(BodySense.FEET_ON_GROUND)

    private fun updateFacing() {
        when {
            megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
            megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
        }
    }

    override fun getTag() = TAG
}
