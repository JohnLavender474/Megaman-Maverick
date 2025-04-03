package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.coerceIn
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
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
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.state.EnumStateMachineBuilder
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.SlashDissipation
import com.megaman.maverick.game.entities.enemies.RatRobot
import com.megaman.maverick.game.entities.projectiles.SlashWave
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class RodentMan(game: MegamanMaverickGame) : AbstractBoss(game), IParentEntity, IAnimatedEntity, IFaceable,
    IDirectional {

    companion object {
        const val TAG = "RodentMan"

        private const val INIT_DUR = 1f
        private const val STAND_DUR = 0.75f
        private const val SHIELDED_DUR = 1.5f
        private const val SPAWN_RATS_TIME = 0.75f
        private const val SLASH_DUR = 0.5f
        private const val SLASH_TIME = 0.1f
        private const val SLASH_DELAY = 0.75f
        private const val RUN_DUR = 3f

        private const val BODY_STANDARD_WIDTH = 1.25f
        private const val BODY_STANDARD_HEIGHT = 1.75f
        private const val BODY_RUNNING_WIDTH = 1.75f
        private const val BODY_RUNNING_HEIGHT = 1.25f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val STAND_FRICTION_X = 7.5f
        private const val DEFAULT_FRICTION_X = 1.05f
        private const val DEFAULT_FRICTION_Y = 1.25f

        private const val RUN_SPEED = 8f

        private const val JUMP_FROM_GROUND_IMPULSE_Y = 18f
        private const val JUMP_FROM_WALL_IMPULSE_X = 8f
        private const val JUMP_FROM_WALL_IMPULSE_Y = 12f
        private const val JUMP_MAX_IMPULSE_X = 20f

        // When Rodent Man is running up a wall, then if Megaman is above Rodent Man and is within the given threshold
        // x-distance from the same wall, then Rodent man should keep running up
        private const val WALL_RUN_MEGAMAN_X_DIST_THRESHOLD = 3.5f
        private const val WALL_JUMP_CHANCE_SCALAR = 0.5f

        private const val SLASH_WAVE_SPEED = 12f
        private val IN_AIR_UP_SLASH_WAVE_ANGLES = gdxArrayOf(135f, 180f)
        private val IN_AIR_DOWN_SLASH_WAVE_ANGLES = gdxArrayOf(180f, 225f)
        private val STAND_LEFT_SLASH_WAVE_ANGLES = gdxArrayOf(135f, 180f)
        private val STAND_RIGHT_SLASH_WAVE_ANGLES = gdxArrayOf(0f, 45f)

        private const val STAND_TO_STATE_MODULO = 2
        private const val STAND_TO_RUN = 0
        private const val STAND_TO_JUMP = 1
        private const val STAND_SLASH = 2
        private const val STAND_SHIELDED = 5

        private const val SPAWN_RAT_DELAY = 3f
        private const val MAX_RATS = 2

        private const val SPRITE_WIDTH = 4f
        private const val SPRITE_HEIGHT = 3f
        private const val SPRITE_OFFSET_DIR_UP = -0.1f
        private const val SPRITE_OFFSET_DIR_LEFT = 0.5f
        private const val SPRITE_OFFSET_DIR_RIGHT = -0.5f

        private val animDefs = orderedMapOf<String, AnimationDef>(
            "stand" pairTo AnimationDef(2, 1, gdxArrayOf(0.5f, 0.15f), true),
            "stand_slash" pairTo AnimationDef(2, 1, 0.1f, false),
            "run" pairTo AnimationDef(1, 5, 0.1f, true),
            "jump_down_look_down" pairTo AnimationDef(),
            "jump_down_look_straight" pairTo AnimationDef(),
            "jump_up_look_down" pairTo AnimationDef(),
            "jump_up_look_up" pairTo AnimationDef(),
            "jump_slash" pairTo AnimationDef(2, 1, 0.1f, false),
            "shielded" pairTo AnimationDef(2, 1, 0.1f, true),
            "defeated" pairTo AnimationDef(3, 1, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class RodentManState { INIT, STAND, JUMP, RUN, SHIELDED }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override lateinit var facing: Facing
    override var children = Array<IGameEntity>()

    private lateinit var stateMachine: StateMachine<RodentManState>
    private val currentState: RodentManState
        get() = stateMachine.getCurrent()
    private val stateTimers = orderedMapOf<RodentManState, Timer>(
        RodentManState.INIT pairTo Timer(INIT_DUR),
        RodentManState.RUN pairTo Timer(RUN_DUR),
        RodentManState.STAND pairTo Timer(STAND_DUR),
        RodentManState.SHIELDED pairTo Timer(SHIELDED_DUR)
            .addRunnable(TimeMarkedRunnable(SPAWN_RATS_TIME) { spawnRats() })
    )

    private val slashTimer = Timer(SLASH_DUR)
        .addRunnable(TimeMarkedRunnable(SLASH_TIME) {
            FacingUtils.setFacingOf(this)
            spawnSlashWaves()
        })
    private val slashing: Boolean
        get() = !slashTimer.isFinished()
    private val slashDelay = Timer(SLASH_DELAY)

    private var onWallState: ProcessState? = null
    private val leftWall = GameRectangle()
    private val rightWall = GameRectangle()

    private var minWallRunJumpY = 0f
    private var maxWallRunJumpY = 0f

    private val ratSpawns = Array<Vector2>()
    private val spawnRatDelay = Timer(SPAWN_RAT_DELAY)

    private var timesAtStandState = 0

    private val reusableAnglesArray = Array<Float>()
    private val reusableFixturesArray = Array<IFixture>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        stateMachine = buildStateMachine()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        body.physics.gravityOn = true

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        slashTimer.setToEnd()
        slashDelay.setToEnd()

        spawnRatDelay.reset()

        onWallState = null
        direction = Direction.UP
        FacingUtils.setFacingOf(this)

        timesAtStandState = 0

        spawnProps.forEach { key, value ->
            key.toString().let {
                when {
                    it.contains(ConstKeys.CHILD) -> {
                        val ratSpawn = (value as RectangleMapObject).rectangle.getCenter(false)
                        ratSpawns.add(ratSpawn)
                    }

                    it == ConstKeys.LEFT -> {
                        val leftWall = (value as RectangleMapObject).rectangle
                        this.leftWall.set(leftWall)
                    }

                    it == ConstKeys.RIGHT -> {
                        val rightWall = (value as RectangleMapObject).rectangle
                        this.rightWall.set(rightWall)
                    }

                    it == "${ConstKeys.MIN}_${ConstKeys.WALL}_${ConstKeys.RUN}_${ConstKeys.JUMP}_${ConstKeys.Y}" ->
                        minWallRunJumpY = (value as RectangleMapObject).rectangle.y

                    it == "${ConstKeys.MAX}_${ConstKeys.WALL}_${ConstKeys.RUN}_${ConstKeys.JUMP}_${ConstKeys.Y}" ->
                        maxWallRunJumpY = (value as RectangleMapObject).rectangle.toGameRectangle().getMaxY()
                }
            }
        }
    }

    override fun isReady(delta: Float) = stateTimers[RodentManState.INIT].isFinished()

    override fun onReady() {
        GameLogger.debug(TAG, "onReady()")
        super.onReady()
        body.physics.gravityOn = true
    }

    override fun triggerDefeat() {
        GameLogger.debug(TAG, "triggerDefeat()")
        super.triggerDefeat()

        body.physics.velocity.setZero()
        body.physics.gravityOn = false

        onEndRunning()
    }

    override fun onDefeated(delta: Float) {
        GameLogger.debug(TAG, "onDefeated()")
        super.onDefeated(delta)

        ratSpawns.clear()

        children.forEach { (it as MegaGameEntity).destroy() }
        children.clear()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        ratSpawns.clear()

        children.forEach { (it as MegaGameEntity).destroy() }
        children.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add update@{ delta ->
            if (betweenReadyAndEndBossSpawnEvent) return@update

            if (defeated) {
                body.physics.velocity.setZero()
                body.physics.gravityOn = false
                explodeOnDefeat(delta)
                return@update
            }

            when (currentState) {
                RodentManState.RUN -> {
                    when (onWallState) {
                        ProcessState.BEGIN -> {
                            GameLogger.debug(TAG, "update(): onWallState: BEGIN")

                            body.physics.velocity.y = RUN_SPEED * ConstVals.PPM

                            // If Rodent Man was running to the right, then his body should be rotated left,
                            // and vice versa. For the remainder of the running state, the sides will be checked
                            // to determine if the state should end before the timer.
                            direction = if (isFacing(Facing.RIGHT)) Direction.LEFT else Direction.RIGHT
                            onWallState = ProcessState.CONTINUE
                        }

                        ProcessState.CONTINUE -> {
                            body.physics.velocity.y = RUN_SPEED * ConstVals.PPM

                            when {
                                shouldJumpFromWallRunning() -> {
                                    GameLogger.debug(
                                        TAG,
                                        "update(): onWallState: CONTINUE --> END: should jump from wall running"
                                    )
                                    onWallState = ProcessState.END
                                }

                                else -> {
                                    val senseToCheck = when (direction) {
                                        Direction.LEFT -> BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                                        else -> BodySense.SIDE_TOUCHING_BLOCK_LEFT
                                    }
                                    if (body.isSensing(senseToCheck)) {
                                        GameLogger.debug(
                                            TAG,
                                            "update(): onWallState: CONTINUE --> END: side touching block"
                                        )
                                        onWallState = ProcessState.END
                                    }
                                }
                            }
                        }

                        ProcessState.END -> {
                            GameLogger.debug(TAG, "update(): onWallState: END")

                            val centerX = when (facing) {
                                Facing.LEFT -> leftWall.getMaxX() + ((BODY_STANDARD_WIDTH * ConstVals.PPM) / 2f)
                                Facing.RIGHT -> rightWall.getX() - ((BODY_STANDARD_WIDTH * ConstVals.PPM) / 2f)
                            }
                            body.setCenterX(centerX)

                            stateMachine.next()
                        }

                        // If null, then Rodent Man hasn't started running up a wall yet.
                        // If he is facing a block (wall), then set the process state to BEGIN.
                        // Wait until the next update to handle the process (to allow body to update).
                        null -> {
                            body.physics.velocity.x = RUN_SPEED * ConstVals.PPM * facing.value

                            if (FacingUtils.isFacingBlock(this)) {
                                GameLogger.debug(TAG, "update(): onWallState: null --> BEGIN")
                                onWallState = ProcessState.BEGIN
                            }
                        }
                    }
                }

                RodentManState.JUMP -> {
                    if (!slashing) FacingUtils.setFacingOf(this)

                    if (shouldEndJump()) {
                        GameLogger.debug(TAG, "update(): end jump")
                        stateMachine.next()
                    }
                }

                else -> {}
            }

            val timer = stateTimers[currentState]
            if (timer != null && shouldUpdateStateTimer(currentState)) {
                timer.update(delta)
                if (timer.isFinished()) {
                    GameLogger.debug(TAG, "update(): timer finished for currentState=$currentState")
                    stateMachine.next()
                }
            }

            slashDelay.update(delta)
            if (slashDelay.isJustFinished()) slashTimer.reset()
            slashTimer.update(delta)

            val childIter = children.iterator()
            while (childIter.hasNext()) {
                val next = childIter.next() as MegaGameEntity
                if (next.dead) childIter.remove()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.velocityClamp.set(10f * ConstVals.PPM, 25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM)
        )
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM)
        )
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM)
        )
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM)
        )
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val center = body.getCenter()
            val width: Float
            val height: Float
            if (currentState == RodentManState.RUN) {
                width = BODY_RUNNING_WIDTH
                height = BODY_RUNNING_HEIGHT
            } else {
                width = BODY_STANDARD_WIDTH
                height = BODY_STANDARD_HEIGHT
            }
            body.setSize(width * ConstVals.PPM, height * ConstVals.PPM)
            body.setCenter(center)

            if (currentState == RodentManState.RUN && onWallState?.isRunning() == true) when {
                isFacing(Facing.LEFT) -> {
                    val wallX = leftWall.getMaxX()
                    val bodyX = body.getX() // gets the raw x (before rotation)
                    val adjustedX = body.getBounds().getX() // gets the rotated bound's x
                    val offset = abs(bodyX - adjustedX)
                    body.setX(wallX - offset)
                }

                else -> {
                    val wallX = rightWall.getX()
                    val bodyX = body.getMaxX() // gets the raw max x (before rotation)
                    val adjustedX = body.getBounds().getMaxX() // gets the rotated bound's max x
                    val offset = abs(bodyX - adjustedX)
                    body.setMaxX(wallX + offset)
                }
            }

            body.getFixtures(reusableFixturesArray, FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
                .forEach { fixture -> ((fixture as Fixture).rawShape as GameRectangle).set(body) }
            reusableFixturesArray.clear()

            feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
            headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
            leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
            rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f

            shieldFixture.setActive(currentState == RodentManState.SHIELDED)
            body.fixtures[FixtureType.DAMAGEABLE].first().setActive(currentState != RodentManState.SHIELDED)

            body.physics.gravity.y = ConstVals.PPM * when {
                currentState == RodentManState.RUN && onWallState != null -> 0f
                body.isSensing(BodySense.FEET_ON_GROUND) -> GROUND_GRAVITY
                else -> GRAVITY
            }

            if (shouldSetVelXToZero()) body.physics.velocity.x = 0f

            body.physics.applyFrictionX = shouldApplyFrictionX()
            body.physics.applyFrictionY = shouldApplyFrictionY()

            body.physics.receiveFrictionX = shouldReceiveFrictionX()
            body.physics.receiveFrictionY = shouldReceiveFrictionY()

            body.physics.defaultFrictionOnSelf.x = getDefaultFrictionX()
            body.physics.defaultFrictionOnSelf.y = DEFAULT_FRICTION_Y
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER
            )
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite().also { sprite ->
                sprite.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM)
            }
        ).updatable { _, sprite ->
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            sprite.setPosition(body.getPositionPoint(position), position)
            when (direction) {
                Direction.UP -> sprite.translateY(SPRITE_OFFSET_DIR_UP * ConstVals.PPM)
                Direction.LEFT -> sprite.translateX(SPRITE_OFFSET_DIR_LEFT * ConstVals.PPM)
                Direction.RIGHT -> sprite.translateX(SPRITE_OFFSET_DIR_RIGHT * ConstVals.PPM)
                Direction.DOWN -> {}
            }

            sprite.setFlip(isFacing(Facing.RIGHT), false)

            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    if (defeated) "defeated" else when (currentState) {
                        RodentManState.INIT, RodentManState.STAND -> when {
                            body.isSensing(BodySense.FEET_ON_GROUND) -> if (slashing) "stand_slash" else "stand"
                            else -> if (slashing) "jump_slash" else "jump_down_look_straight"
                        }

                        RodentManState.JUMP -> when {
                            slashing -> "jump_slash"

                            body.physics.velocity.y > 0f -> when {
                                megaman.body.getCenter().y > body.getCenter().y -> "jump_up_look_up"
                                else -> "jump_up_look_down"
                            }

                            else -> when {
                                megaman.body.getCenter().y > body.getCenter().y -> "jump_down_look_straight"
                                else -> "jump_down_look_down"
                            }
                        }

                        else -> currentState.name.lowercase()
                    }
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        try {
                            animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                        } catch (e: Exception) {
                            throw IllegalStateException("Failed to create animation for key=$key", e)
                        }
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<RodentManState>()
        .setOnChangeState(this::onChangeState)
        .setTriggerChangeWhenSameElement(true)
        .initialState(RodentManState.INIT)
        // init
        .transition(RodentManState.INIT, RodentManState.STAND) { true }
        // stand
        .transition(RodentManState.STAND, RodentManState.SHIELDED) { shouldShieldFromStand() }
        .transition(RodentManState.STAND, RodentManState.JUMP) { shouldJumpFromStand() }
        .transition(RodentManState.STAND, RodentManState.RUN) { shouldRunFromStand() }
        .transition(RodentManState.STAND, RodentManState.STAND) { true }
        // run
        .transition(RodentManState.RUN, RodentManState.JUMP) { true }
        // jump
        .transition(RodentManState.JUMP, RodentManState.STAND) { true }
        // shielded
        .transition(RodentManState.SHIELDED, RodentManState.STAND) { true }
        // build
        .build()

    private fun onChangeState(current: RodentManState, previous: RodentManState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        when (current) {
            RodentManState.JUMP -> {
                jump()

                slashDelay.reset()
            }

            RodentManState.STAND -> {
                slashTimer.setToEnd()

                FacingUtils.setFacingOf(this)

                timesAtStandState++
                if (timesAtStandState % STAND_SLASH == 0) slashDelay.reset()
            }

            RodentManState.RUN -> FacingUtils.setFacingOf(this)

            else -> {}
        }

        if (previous != RodentManState.INIT) stateTimers[previous]?.reset()
        if (previous == RodentManState.RUN) {
            onEndRunning()

            FacingUtils.setFacingOf(this)
        }
    }

    private fun shouldUpdateStateTimer(state: RodentManState) = when (state) {
        RodentManState.RUN -> onWallState == null
        RodentManState.INIT -> body.isSensing(BodySense.FEET_ON_GROUND)
        RodentManState.STAND, RodentManState.JUMP -> slashDelay.isFinished() && slashTimer.isFinished()
        else -> true
    }

    private fun spawnSlashWaves() {
        GameLogger.debug(TAG, "spawnSlashWaves()")

        val slashAngles = reusableAnglesArray

        when (currentState) {
            RodentManState.STAND -> when (facing) {
                Facing.LEFT -> slashAngles.addAll(STAND_LEFT_SLASH_WAVE_ANGLES)
                Facing.RIGHT -> slashAngles.addAll(STAND_RIGHT_SLASH_WAVE_ANGLES)
            }

            else -> {
                when {
                    megaman.body.getCenter().y >= body.getCenter().y -> slashAngles.addAll(IN_AIR_UP_SLASH_WAVE_ANGLES)
                    else -> slashAngles.addAll(IN_AIR_DOWN_SLASH_WAVE_ANGLES)
                }

                if (isFacing(Facing.RIGHT)) for (i in 0 until slashAngles.size) slashAngles[i] += 180f
            }
        }

        slashAngles.forEach { slashAngle ->
            val trajectory = GameObjectPools.fetch(Vector2::class)
                .set(0f, SLASH_WAVE_SPEED * ConstVals.PPM)
                .setAngleDeg(slashAngle)

            val center = GameObjectPools.fetch(Vector2::class)
                .set(body.getCenter())
                .add(ConstVals.PPM.toFloat() * facing.value, 0f)

            val slashWave = MegaEntityFactory.fetch(SlashWave::class)!!
            slashWave.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo center,
                    ConstKeys.TRAJECTORY pairTo trajectory
                )
            )
        }

        slashAngles.clear()

        spawnSlashDissipation()

        requestToPlaySound(SoundAsset.WHIP_V2_SOUND, false)
    }

    private fun spawnSlashDissipation() {
        GameLogger.debug(TAG, "spawnSlashDissipation()")

        val center = GameObjectPools.fetch(Vector2::class)
            .set(body.getCenter())
            .add(ConstVals.PPM.toFloat() * facing.value, 0f)

        val slashDissipation = MegaEntityFactory.fetch(SlashDissipation::class)!!
        slashDissipation.spawn(props(ConstKeys.POSITION pairTo center, ConstKeys.FACING pairTo facing))
    }

    private fun spawnRats() {
        GameLogger.debug(TAG, "spawnRats()")

        for (ratSpawn in ratSpawns) {
            if (children.size >= MAX_RATS) break

            val rat = MegaEntityFactory.fetch(RatRobot::class)!!
            rat.spawn(props(ConstKeys.POSITION pairTo ratSpawn, ConstKeys.DROP_ITEM_ON_DEATH pairTo false))

            children.add(rat)
        }
    }

    private fun shouldJumpFromStand() = timesAtStandState % STAND_TO_STATE_MODULO == STAND_TO_JUMP

    private fun shouldRunFromStand() = timesAtStandState % STAND_TO_STATE_MODULO == STAND_TO_RUN

    private fun shouldShieldFromStand() = timesAtStandState % STAND_SHIELDED == 0

    private fun shouldJumpFromWallRunning(): Boolean {
        if (currentState != RodentManState.RUN || onWallState == null) {
            GameLogger.error(TAG, "shouldJumpFromWallRunning(): currentState=$currentState, onWallState=$onWallState")
            return false
        }

        val bodyBounds = body.getBounds()
        val megamanBounds = megaman.body.getBounds()

        // Rodent Man should never jump if he's below the minimum y value
        if (bodyBounds.getMaxY() <= minWallRunJumpY) return false
        // Rodent Man must jump if he's reached the maximum y value
        if (bodyBounds.getY() >= maxWallRunJumpY) return true

        // If Megaman is above Rodent Man and within a certain X distance of the same wall that Rodent Man is running
        // up, then Rodent Man is more likely to hit Megaman if he keeps running up the wall in order to get above him
        if (megamanBounds.getY() >= bodyBounds.getY()) {
            // Get the x-position of the wall that Rodent Man is running up to compare to Megaman's x-position
            val wallX: Float
            val megamanX: Float
            if (isFacing(Facing.LEFT)) {
                wallX = leftWall.getMaxX()
                megamanX = megamanBounds.getX()
            } else {
                wallX = rightWall.getX()
                megamanX = megamanBounds.getMaxX()
            }

            // If below the threshold, then return no chance of jumping
            if (abs(megamanX - wallX) <= WALL_RUN_MEGAMAN_X_DIST_THRESHOLD * ConstVals.PPM) return false
        }

        val denominator = maxWallRunJumpY - minWallRunJumpY
        if (denominator <= 1f) throw IllegalStateException("Denominator should never be less than 1")

        var numerator = bodyBounds.getY() - minWallRunJumpY
        if (numerator < 0f) numerator = 0f
        if (numerator > denominator) numerator = denominator

        return WALL_JUMP_CHANCE_SCALAR * UtilMethods.getRandom(0f, 100f) >= (numerator / denominator) * 100f
    }

    // If Rodent Man is jumping from a wall, then `onWallState` needs to remain a non-null value until after the
    // `jump` function is called.
    private fun jump() {
        val impulseX: Float
        val impulseY: Float
        if (onWallState != null) {
            // Use distance calculation to figure out which wall Rodent Man is jumping from (this is easier than
            // trying to store a field or pass in a parameter to determine which wall he's jumping from).
            val leftDist = abs(leftWall.getMaxX() - body.getCenter().x)
            val rightDist = abs(rightWall.getX() - body.getCenter().x)
            impulseX = ConstVals.PPM * when {
                leftDist < rightDist -> JUMP_FROM_WALL_IMPULSE_X
                else -> -JUMP_FROM_WALL_IMPULSE_X
            }
            impulseY = ConstVals.PPM * JUMP_FROM_WALL_IMPULSE_Y
        } else {
            val rawImpulse = MegaUtilMethods.calculateJumpImpulse(
                body.getPositionPoint(Position.BOTTOM_CENTER),
                megaman.body.getPositionPoint(Position.TOP_CENTER),
                JUMP_FROM_GROUND_IMPULSE_Y * ConstVals.PPM
            )
            impulseX = rawImpulse.x.coerceIn(JUMP_MAX_IMPULSE_X * ConstVals.PPM)
            impulseY = rawImpulse.y
        }
        GameLogger.debug(TAG, "jump(): impulseX=$impulseX, impulseY=$impulseY")
        body.physics.velocity.set(impulseX, impulseY)
    }

    private fun shouldEndJump() = isWallSliding() ||
        (body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f)

    private fun onEndRunning() {
        GameLogger.debug(TAG, "onEndRunning()")
        onWallState = null
        direction = Direction.UP
    }

    private fun isHittingAgainstLeftWall() =
        body.physics.velocity.x <= 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)

    private fun isHittingAgainstRightWall() =
        body.physics.velocity.x >= 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)

    private fun isWallSliding() = body.physics.velocity.y < 0f &&
        !body.isSensing(BodySense.FEET_ON_GROUND) &&
        (isHittingAgainstLeftWall() || isHittingAgainstRightWall())

    private fun shouldSetVelXToZero() = when (currentState) {
        RodentManState.RUN -> onWallState != null
        else -> false
    }

    private fun shouldApplyFrictionX() = currentState != RodentManState.RUN

    private fun shouldApplyFrictionY() = currentState != RodentManState.RUN

    private fun shouldReceiveFrictionX() = currentState != RodentManState.JUMP

    private fun shouldReceiveFrictionY() = currentState != RodentManState.JUMP

    private fun getDefaultFrictionX() = when (currentState) {
        RodentManState.STAND -> if (body.isSensing(BodySense.FEET_ON_GROUND)) STAND_FRICTION_X else DEFAULT_FRICTION_X
        else -> DEFAULT_FRICTION_X
    }

    override fun getTag() = TAG
}
