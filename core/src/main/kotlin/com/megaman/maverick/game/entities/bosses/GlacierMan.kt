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
import com.mega.game.engine.common.UtilMethods.getRandomBool
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
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class GlacierMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "GlacierMan"

        private const val INIT_DUR = 1f
        private const val STAND_DUR = 1f
        private const val DUCK_DUR = 0.75f
        private const val SLED_DUR = 1.5f
        private const val STOP_DUR = 0.8f

        private const val SHOOT_ANIM_DUR = 0.25f
        private const val SHOOT_UP_CHANCE = 0.4f

        private const val ICE_BLAST_ATTACK_DUR = 4f
        private const val ICE_BLAST_ATTACK_COUNT = 6
        private const val ICE_BLAST_VEL = 10f
        private const val CHUNK_ICE_BLAST_VEL_Y = 10f

        private const val MEGAMAN_OFFSET_X = 2.5f
        private const val MEGAMAN_ABOVE_OFFSET_Y = 1.5f

        private const val JUMP_IMPULSE_X = 25f
        private const val JUMP_IMPULSE_Y = 15f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val SLED_SPEED = 8f
        private const val BRAKE_FRICTION_X = 1.25f
        private const val MAX_BRAKE_DUR = 0.75f

        private const val SNOWBALL_SIZE = 0.5f
        private const val SNOWBALL_VEL_UP_X = 9f
        private const val SNOWBALL_VEL_UP_Y = 15f
        private const val SNOWBALL_VEL_STRAIGHT_X = 15f
        private const val SNOWBALL_VEL_STRAIGHT_Y = 6f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class GlacierManState {
        INIT, STAND, STOP, JUMP, SLED, BRAKE, DUCK, ICE_BLAST_ATTACK
    }

    override lateinit var facing: Facing

    private val timers = ObjectMap<String, Timer>()
    private lateinit var animator: Animator
    private lateinit var stateMachine: StateMachine<GlacierManState>
    private val walls = Array<GameRectangle>()
    private var shootUp = false
    private var firstUpdate = true
    private var iceBlastLeftHand = false
    private var previousState: GlacierManState? = null

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            regions.put("defeated", atlas.findRegion("$TAG/defeated"))
            regions.put("stand", atlas.findRegion("$TAG/stand"))
            regions.put("stand_shoot", atlas.findRegion("$TAG/stand_shoot"))
            regions.put("stand_shoot_up", atlas.findRegion("$TAG/stand_shoot_up"))
            regions.put("stop", atlas.findRegion("$TAG/stop"))
            regions.put("jump", atlas.findRegion("$TAG/jump"))
            regions.put("fall", atlas.findRegion("$TAG/fall"))
            regions.put("fall_shoot_up", atlas.findRegion("$TAG/fall_shoot_up"))
            regions.put("sled", atlas.findRegion("$TAG/sled"))
            regions.put("sled_shoot", atlas.findRegion("$TAG/sled_shoot"))
            regions.put("sled_shoot_up", atlas.findRegion("$TAG/sled_shoot_up"))
            regions.put("brake", atlas.findRegion("$TAG/brake"))
            regions.put("brake_shoot", atlas.findRegion("$TAG/brake_shoot"))
            regions.put("brake_shoot_up", atlas.findRegion("$TAG/brake_shoot_up"))
            regions.put("duck", atlas.findRegion("$TAG/duck"))
            regions.put("duck_shoot", atlas.findRegion("$TAG/duck_shoot"))
            regions.put("duck_shoot_up", atlas.findRegion("$TAG/duck_shoot_up"))
            regions.put("ice_blast_attack", atlas.findRegion("$TAG/ice_blast_attack"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
        buildTimers()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        body.physics.defaultFrictionOnSelf.x = 1f
        body.physics.applyFrictionX = false
        body.physics.velocity.setZero()

        stateMachine.reset()
        timers.forEach { if (it.key == "shoot_anim") it.value.setToEnd() else it.value.reset() }

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        shootUp = false
        firstUpdate = true
        iceBlastLeftHand = false

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.WALL)) {
                val wall = (value as RectangleMapObject).rectangle.toGameRectangle(false)
                walls.add(wall)
            }
        }

        previousState = null
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun isReady(delta: Float) = timers["init"].isFinished()

    override fun triggerDefeat() {
        super.triggerDefeat()
        body.physics.velocity.setZero()
        body.physics.gravityOn = false
    }

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

            timers["shoot_anim"].update(delta)

            when (val state = stateMachine.getCurrent()) {
                GlacierManState.INIT -> {
                    // init state only occurs as the first state and never again
                    val timer = timers["init"]
                    timer.update(delta)
                    if (timer.isFinished()) stateMachine.next()
                }

                GlacierManState.STAND,
                GlacierManState.DUCK,
                GlacierManState.SLED,
                GlacierManState.STOP,
                GlacierManState.ICE_BLAST_ATTACK -> {
                    if (state.equalsAny(GlacierManState.STAND, GlacierManState.DUCK))
                        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

                    body.physics.velocity.x =
                        if (state == GlacierManState.SLED) SLED_SPEED * ConstVals.PPM * facing.value else 0f

                    val key = state.name.lowercase()
                    val timer = timers[key]
                    timer.update(delta)
                    if (timer.isFinished() || (state == GlacierManState.SLED && shouldStopSledding())) {
                        timer.reset()
                        val next = stateMachine.next()
                        GameLogger.debug(TAG, "update(): current=$state, next=$next")
                    }
                }

                GlacierManState.JUMP -> {
                    facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                    if (body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND)) {
                        GameLogger.debug(TAG, "update(): end jump")
                        stateMachine.next()
                    } else body.physics.velocity.x += JUMP_IMPULSE_X * ConstVals.PPM * facing.value * delta
                }

                GlacierManState.BRAKE -> {
                    val maxBrakeTimer = timers["max_brake"]
                    maxBrakeTimer.update(delta)
                    if ((abs(body.physics.velocity.x).epsilonEquals(0f, 0.25f * ConstVals.PPM) &&
                            body.isSensing(BodySense.FEET_ON_GROUND)) || maxBrakeTimer.isFinished()
                    ) {
                        GameLogger.debug(TAG, "update(): end brake")
                        body.physics.velocity.x = 0f
                        maxBrakeTimer.reset()
                        stateMachine.next()
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM, 1.75f * ConstVals.PPM)
        body.physics.velocityClamp.set(10f * ConstVals.PPM, 25f * ConstVals.PPM)
        body.physics.applyFrictionX = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(
                body,
                FixtureType.FEET,
                GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM)
            )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(
                body,
                FixtureType.HEAD,
                GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM)
            )
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val leftFixture =
            Fixture(
                body,
                FixtureType.SIDE,
                GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat())
            )
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(
                body,
                FixtureType.SIDE,
                GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat())
            )
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                (if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY) * ConstVals.PPM
            if ((isFacing(Facing.LEFT) && body.physics.velocity.x < 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                (isFacing(Facing.RIGHT) && body.physics.velocity.x > 0f && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
            ) body.physics.velocity.x = 0f
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER
            )
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(3.5f * ConstVals.PPM, 2.625f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animations = objectMapOf<String, IAnimation>(
            "defeated" pairTo Animation(regions["defeated"], 3, 1, 0.1f, true),
            "stand" pairTo Animation(regions["stand"]),
            "stand_shoot" pairTo Animation(regions["stand_shoot"]),
            "stand_shoot_up" pairTo Animation(regions["stand_shoot_up"]),
            "brake" pairTo Animation(regions["brake"], 3, 1, 0.1f, true),
            "brake_shoot" pairTo Animation(regions["brake_shoot"], 3, 1, 0.1f, true),
            "brake_shoot_up" pairTo Animation(regions["brake_shoot_up"], 3, 1, 0.1f, true),
            "duck" pairTo Animation(regions["duck"]),
            "duck_shoot" pairTo Animation(regions["duck_shoot"]),
            "duck_shoot_up" pairTo Animation(regions["duck_shoot_up"]),
            "fall" pairTo Animation(regions["fall"], 4, 2, 0.1f, true),
            "fall_shoot_up" pairTo Animation(regions["fall_shoot_up"], 4, 2, 0.1f, true),
            "jump" pairTo Animation(regions["jump"], 4, 2, 0.1f, true),
            "sled" pairTo Animation(regions["sled"], 2, 2, 0.1f, true),
            "sled_shoot" pairTo Animation(regions["sled_shoot"], 2, 2, 0.1f, true),
            "sled_shoot_up" pairTo Animation(regions["sled_shoot_up"], 2, 2, 0.1f, true),
            "stop" pairTo Animation(regions["stop"], 3, 3, 0.075f, false),
            "ice_blast_attack" pairTo Animation(regions["ice_blast_attack"], 2, 1, 0.1f, true)
        )
        val keySupplier: () -> String? = {
            if (defeated) "defeated"
            else {
                val state = stateMachine.getCurrent()

                val key = when (state) {
                    GlacierManState.INIT -> if (body.isSensing(BodySense.FEET_ON_GROUND)) "stop" else "fall"
                    GlacierManState.JUMP -> if (body.physics.velocity.y > 0f) "jump" else "fall"
                    else -> state.name.lowercase()
                }

                if (!timers["shoot_anim"].isFinished()) {
                    val shootKey = if (shootUp) "${key}_shoot_up" else "${key}_shoot"
                    if (animations.containsKey(shootKey)) shootKey else key
                } else key
            }
        }
        val onChangeKey: (String?, String?) -> Unit = { oldKey, newKey ->
            if (newKey != null && oldKey != null && newKey.contains("_shoot") && !oldKey.contains("_shoot")) {
                val newKeyRaw = newKey.removeSuffix("_up").removeSuffix("_shoot")
                if (newKeyRaw == oldKey) {
                    val oldAnimation = animations[oldKey]
                    val currentAnimation = animations[newKey]
                    if (oldAnimation != null && currentAnimation != null) {
                        val oldAnimIndex = oldAnimation.getIndex()
                        currentAnimation.setIndex(oldAnimIndex)
                    }
                }
            }
        }
        animator = Animator(keySupplier, animations, onChangeKey = onChangeKey)
        return AnimationsComponent(this, animator)
    }

    private fun buildStateMachine(): StateMachine<GlacierManState> {
        val builder = StateMachineBuilder<GlacierManState>()
        GlacierManState.entries.forEach { state -> builder.state(state.name, state) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(GlacierManState.INIT.name)
            // init state only occurs as the first state and always goes to the stand state
            .transition(GlacierManState.INIT.name, GlacierManState.STAND.name) { ready }
            /*
            Transitions from the STAND state:
            - If this is not the first update and random is less than threshold, then perform "ice blast attack" behavior.
            - If Megaman is above the offset value from the boss's body, then the "jump" behavior will always occur.
            The "jump" behavior includes the boss shooting numerous projectiles, which makes the player likely to
            prefer staying toward the ground (which is what the boss wants).
            - If Megaman is NOT above the offset value from the boss's body, then the boss will prefer to either
            the "duck" or "sled" behavior, though it is possible for the boss to jump.
            */
            .transition(
                GlacierManState.STAND.name,
                GlacierManState.ICE_BLAST_ATTACK.name
            ) {
                !firstUpdate &&
                    previousState != GlacierManState.ICE_BLAST_ATTACK &&
                    canIceBlast() &&
                    getRandom(0, 10) <= 4
            }
            .transition(
                GlacierManState.STAND.name,
                GlacierManState.DUCK.name
            ) {
                !isMegamanAboveOffsetY() &&
                    previousState != GlacierManState.DUCK &&
                    getRandom(0, 10) <= 5
            }
            .transition(
                GlacierManState.STAND.name,
                GlacierManState.SLED.name
            ) {
                isMegamanOutsideOffsetX() && previousState != GlacierManState.SLED
            }
            .transition(GlacierManState.STAND.name, GlacierManState.JUMP.name) { true }
            /*
            Transitions from the DUCK state:
            - If Megaman is outside offset x, then start "sled" behavior.
            - If Megaman is above the offset value from the boss's body, then jump.
            */
            .transition(
                GlacierManState.DUCK.name,
                GlacierManState.SLED.name
            ) { isMegamanOutsideOffsetX() }
            .transition(
                GlacierManState.DUCK.name,
                GlacierManState.JUMP.name
            ) { isMegamanAboveOffsetY() }
            /*
            Transitions from the SLED state:
            - If Megaman is above the offset value from the boss's body, then jump.
            - Otherwise, go into the "brake" state.
            */
            .transition(
                GlacierManState.SLED.name,
                GlacierManState.JUMP.name
            ) { isMegamanAboveOffsetY() }
            .transition(GlacierManState.SLED.name, GlacierManState.BRAKE.name) { true }
            /*
            Transition from the JUMP state:
            - Jump always goes to the "brake" state when "next" is called.
            */
            .transition(GlacierManState.JUMP.name, GlacierManState.BRAKE.name) { true }
            /*
            Transition from the BRAKE state:
            - Brake always goes to the "stop" state when "next" is called.
            */
            .transition(GlacierManState.BRAKE.name, GlacierManState.STOP.name) { true }
            /*
            Transition from the STOP state:
            - Stop always goes to "stand" state when "next" is called.
            */
            .transition(GlacierManState.STOP.name, GlacierManState.STAND.name) { true }
            /*
            Transition from the ICE_BLAST_ATTACK state:
            - Always goes to "stand" state when "next" is called.
            */
            .transition(GlacierManState.ICE_BLAST_ATTACK.name, GlacierManState.STAND.name) { true }

        return builder.build()
    }

    private fun buildTimers() {
        val shootAnimTimer = Timer(SHOOT_ANIM_DUR)
        timers.put("shoot_anim", shootAnimTimer)

        val initTimer = Timer(INIT_DUR)
        timers.put("init", initTimer)

        val standTimer = Timer(STAND_DUR)
        standTimer.setRunnables(gdxArrayOf(TimeMarkedRunnable(0.5f) { shoot() }))
        timers.put("stand", standTimer)

        val duckTimer = Timer(DUCK_DUR)
        val duckRunnable = TimeMarkedRunnable(0.25f) { shoot() }
        duckTimer.setRunnables(gdxArrayOf(duckRunnable))
        timers.put("duck", duckTimer)

        val sledTimer = Timer(SLED_DUR)
        val sledRunnables = Array<TimeMarkedRunnable>()
        for (i in 0 until 2) {
            val time = 0.5f + 0.5f * i
            val sledRunnable = TimeMarkedRunnable(time) { shoot() }
            sledRunnables.add(sledRunnable)
        }
        sledTimer.setRunnables(sledRunnables)
        timers.put("sled", sledTimer)

        val maxBrakeTimer = Timer(MAX_BRAKE_DUR)
        timers.put("max_brake", maxBrakeTimer)

        val iceBlastAttackTimer = Timer(ICE_BLAST_ATTACK_DUR)
        val iceBlastAttackTimerRunnables = Array<TimeMarkedRunnable>()
        for (i in 0 until ICE_BLAST_ATTACK_COUNT) {
            val increment = ICE_BLAST_ATTACK_DUR / ICE_BLAST_ATTACK_COUNT
            val time = increment + i * increment
            val iceBlastAttackRunnable = TimeMarkedRunnable(time) { iceBlast() }
            iceBlastAttackTimerRunnables.add(iceBlastAttackRunnable)
        }
        iceBlastAttackTimer.setRunnables(iceBlastAttackTimerRunnables)
        timers.put("ice_blast_attack", iceBlastAttackTimer)

        val stopTimer = Timer(STOP_DUR)
        timers.put("stop", stopTimer)
    }

    private fun getIceBlastLeftPos() = body.getCenter().add(-0.75f * ConstVals.PPM, 0.2f * ConstVals.PPM)

    private fun getIceBlastRightPos() = body.getCenter().add(0.75f * ConstVals.PPM, 0.2f * ConstVals.PPM)

    private fun getCurrentIceBlastPos(): Vector2 {
        var preferredPos = if (iceBlastLeftHand) getIceBlastLeftPos() else getIceBlastRightPos()

        val mockIceCubeBounds = GameObjectPools.fetch(GameRectangle::class)
            .setSize(SmallIceCube.BODY_SIZE * ConstVals.PPM)
            .setCenter(preferredPos)

        val contactWithWall = walls.any { it.overlaps(mockIceCubeBounds) }
        if (contactWithWall)
            preferredPos = if (iceBlastLeftHand) getIceBlastRightPos() else getIceBlastLeftPos()

        return preferredPos
    }

    private fun canIceBlast(): Boolean {
        if (!body.isSensing(BodySense.FEET_ON_GROUND)) return false
        val iceBlastPositions = gdxArrayOf(getIceBlastLeftPos(), getIceBlastRightPos())
        return iceBlastPositions.any { pos -> !walls.any { it.contains(pos) } }
    }

    private fun iceBlast() {
        GameLogger.debug(TAG, "iceBlast()")

        val spawn = getCurrentIceBlastPos()
        iceBlastLeftHand = !iceBlastLeftHand

        val trajectory: Vector2
        val gravityOn: Boolean
        val chunkIceBlast = getRandomBool()
        if (chunkIceBlast) {
            trajectory = MegaUtilMethods.calculateJumpImpulse(
                spawn, megaman.body.getCenter(), CHUNK_ICE_BLAST_VEL_Y * ConstVals.PPM
            )
            gravityOn = true
        } else {
            trajectory =
                megaman.body.getCenter().sub(body.getCenter()).nor().scl(ICE_BLAST_VEL * ConstVals.PPM)
            gravityOn = false
        }

        val iceCube = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.SMALL_ICE_CUBE)!!
        iceCube.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.GRAVITY_ON pairTo gravityOn,
                ConstKeys.CLAMP pairTo !chunkIceBlast,
                ConstKeys.FRICTION_X pairTo false,
                ConstKeys.FRICTION_Y pairTo false,
                ConstKeys.SECTION pairTo DrawingSection.FOREGROUND,
                ConstKeys.PRIORITY pairTo 1,
                ConstKeys.MAX pairTo 1,
                ConstKeys.HIT_BY_BLOCK pairTo true
            )
        )
    }

    private fun canShootUp() = !stateMachine.getCurrent().equalsAny(GlacierManState.SLED, GlacierManState.JUMP)

    private fun shoot() {
        GameLogger.debug(TAG, "shoot()")

        shootUp = canShootUp() && (isFalling() || isMegamanAboveOffsetY() || getRandom(0f, 1f) <= SHOOT_UP_CHANCE)

        val spawn = body.getCenter()
        val trajectory = Vector2()
        if (body.isSensing(BodySense.FEET_ON_GROUND)) {
            when {
                stateMachine.getCurrent() == GlacierManState.SLED ->
                    spawn.add(0.75f * facing.value * ConstVals.PPM, 0.1f * ConstVals.PPM)
                shootUp -> spawn.add(0.4f * facing.value * ConstVals.PPM, 0.1f * ConstVals.PPM)
                else -> spawn.add(0.5f * facing.value * ConstVals.PPM, 0f)
            }

            trajectory.set(
                (if (shootUp) SNOWBALL_VEL_UP_X else SNOWBALL_VEL_STRAIGHT_X) * facing.value,
                if (shootUp) SNOWBALL_VEL_UP_Y else SNOWBALL_VEL_STRAIGHT_Y
            ).scl(ConstVals.PPM.toFloat())
        }

        val snowhead = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SNOW_HEAD)!!
        snowhead.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                "${ConstKeys.NO}_${ConstKeys.FACE}" pairTo true,
                ConstKeys.SIZE pairTo Vector2().set(SNOWBALL_SIZE * ConstVals.PPM),
                ConstKeys.SECTION pairTo DrawingSection.FOREGROUND,
                ConstKeys.PRIORITY pairTo 1
            )
        )

        timers["shoot_anim"].reset()
    }

    private fun jump() {
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getPositionPoint(Position.BOTTOM_CENTER), megaman.body.getCenter(), JUMP_IMPULSE_Y * ConstVals.PPM
        )
        body.physics.velocity.y = impulse.y
    }

    private fun isFalling() = stateMachine.getCurrent() == GlacierManState.JUMP &&
        body.physics.velocity.y < 0f && !body.isSensing(BodySense.FEET_ON_GROUND)

    private fun onChangeState(current: GlacierManState, previous: GlacierManState) {
        GameLogger.debug(TAG, "onChangeState(): new=$current, old=$previous")

        firstUpdate = false
        when (current) {
            GlacierManState.STAND -> previousState = previous
            GlacierManState.JUMP -> {
                GameLogger.debug(TAG, "onChangeState(): jump")
                jump()
            }

            GlacierManState.BRAKE -> {
                GameLogger.debug(TAG, "onChangeState(): start brake, apply brake friction")
                body.physics.defaultFrictionOnSelf.x = BRAKE_FRICTION_X
                body.putProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_X}", false)
                body.physics.applyFrictionX = true
            }

            else -> GameLogger.debug(TAG, "onChangeState(): no change when current=$current")
        }

        if (previous == GlacierManState.BRAKE) {
            GameLogger.debug(TAG, "onChangeState(): stop brake")
            body.physics.defaultFrictionOnSelf.x = ConstVals.STANDARD_RESISTANCE_X
            body.putProperty("${ConstKeys.ICE}_${ConstKeys.FRICTION_X}", true)
            body.physics.applyFrictionX = true
        }

        if (previous != GlacierManState.BRAKE && current != GlacierManState.BRAKE) {
            body.physics.defaultFrictionOnSelf.x = 1f
            body.physics.applyFrictionX = false
        }
    }

    private fun isMegamanAboveOffsetY() =
        megaman.body.getMaxY() >= body.getY() + MEGAMAN_ABOVE_OFFSET_Y * ConstVals.PPM

    private fun isMegamanOutsideOffsetX() =
        abs(megaman.body.getX() - body.getX()) > MEGAMAN_OFFSET_X * ConstVals.PPM

    private fun shouldStopSledding() =
        (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
}
