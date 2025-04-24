package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
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
import com.mega.game.engine.state.EnumStateMachineBuilder
import com.mega.game.engine.state.StateMachine
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
import com.megaman.maverick.game.entities.hazards.TubeBeamerV2
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.utils.misc.HeadUtils
import com.megaman.maverick.game.world.body.*
import kotlin.math.min

class ReactorManV2(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "ReactorMan"

        private const val INIT_DUR = 1f

        private const val STAND_DUR = 0.25f

        private const val STAND_THROW_DELAY_KEY = "stand_throw_delay"
        private const val STAND_THROW_DELAY = 0.5f
        private const val STAND_THROW_DUR = 1f
        private const val STAND_THROW_TIME = 0.3f

        private const val STAND_THROW_TWO_DUR = 0.5f
        private const val STAND_THROW_TWO_TIME = 0.3f

        private const val RUN_DUR = 0.5f
        private const val RUN_SPEED = 10f

        private const val JUMP_IMPULSE = 16f
        private const val JUMP_THROW_DUR = 0.75f
        private const val JUMP_THROW_TIME = 0.2f
        private const val JUMP_THROW_TWO_DUR = 0.5f
        private const val JUMP_THROW_TWO_TIME = 0.2f

        private const val GIGA_STAND_DUR = 0.5f
        private const val GIGA_RISE_MAX_VEL = 5f
        private const val GIGA_RISE_IMPULSE = 10f
        private const val GIGA_RISE_DUR = 1f
        private const val GIGA_CHANCE_DELTA = 25f
        private const val MIN_CYCLES_BEFORE_GIGA = 3
        private const val GIGA_LASER_BEAM_SETS = 3
        private const val GIGA_LASER_BEAMS_PER_SET = 4
        private const val GIGA_DELAY_BETWEEN_BEAMS = 0.5f
        private const val GIGA_DELAY_BETWEEN_BEAMS_KEY = "giga_delay_between_beams"

        private const val CHANGE_FACING_DELAY_KEY = "change_facing_delay"
        private const val CHANGE_FACING_DELAY = 0.1f

        private const val PROJ_SPEED = 14f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.001f

        private val animDefs = orderedMapOf(
            ReactorManState.INIT pairTo AnimationDef(3, 1, 0.1f, false),
            ReactorManState.STAND pairTo AnimationDef(1, 3, gdxArrayOf(0.5f, 0.15f, 0.15f), true),
            ReactorManState.STAND_THROW pairTo AnimationDef(3, 3, 0.1f, false),
            ReactorManState.STAND_THROW_TWO pairTo AnimationDef(3, 1, 0.1f, false),
            ReactorManState.RUN pairTo AnimationDef(2, 2, 0.1f, true),
            ReactorManState.JUMP pairTo AnimationDef(),
            ReactorManState.JUMP_THROW pairTo AnimationDef(3, 2, 0.1f, false),
            ReactorManState.JUMP_THROW_TWO pairTo AnimationDef(3, 1, 0.1f, false),
            ReactorManState.GIGA_STAND pairTo AnimationDef(2, 1, 0.1f, true),
            ReactorManState.GIGA_RISE pairTo AnimationDef(2, 1, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class ReactorManState {
        INIT, STAND, STAND_THROW, STAND_THROW_TWO, RUN, JUMP, JUMP_THROW, JUMP_THROW_TWO, GIGA_STAND, GIGA_RISE
    }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<ReactorManState>
    private val currentState: ReactorManState
        get() = stateMachine.getCurrent()

    private val stateTimers = orderedMapOf(
        ReactorManState.STAND pairTo Timer(STAND_DUR),
        ReactorManState.INIT pairTo Timer(INIT_DUR),
        ReactorManState.RUN pairTo Timer(RUN_DUR),
        ReactorManState.STAND_THROW pairTo Timer(STAND_THROW_DUR)
            .addRunnable(TimeMarkedRunnable(STAND_THROW_TIME) { throwOneProjectile() }),
        ReactorManState.STAND_THROW_TWO pairTo Timer(STAND_THROW_TWO_DUR)
            .addRunnable(TimeMarkedRunnable(STAND_THROW_TWO_TIME) { throwTwoProjectiles() }),
        ReactorManState.JUMP_THROW pairTo Timer(JUMP_THROW_DUR)
            .setRunOnFirstupdate { spawnProjectile() }
            .addRunnable(TimeMarkedRunnable(JUMP_THROW_TIME) { throwOneProjectile() }),
        ReactorManState.JUMP_THROW_TWO pairTo Timer(JUMP_THROW_TWO_DUR)
            .setRunOnFirstupdate { (0 until 2).forEach { spawnProjectile() } }
            .addRunnable(TimeMarkedRunnable(JUMP_THROW_TWO_TIME) { throwTwoProjectiles() }),
        ReactorManState.GIGA_STAND pairTo Timer(GIGA_STAND_DUR),
        ReactorManState.GIGA_RISE pairTo Timer(GIGA_RISE_DUR)
    )
    private val currentStateTimer: Timer?
        get() = stateTimers[currentState]

    private val otherTimers = orderedMapOf(
        STAND_THROW_DELAY_KEY pairTo Timer(STAND_THROW_DELAY),
        CHANGE_FACING_DELAY_KEY pairTo Timer(CHANGE_FACING_DELAY),
        GIGA_DELAY_BETWEEN_BEAMS_KEY pairTo Timer(GIGA_DELAY_BETWEEN_BEAMS)
    )

    private var standCycles = -1
    private var jumpCycles = -1

    private val projectiles = Array<ReactorManProjectile>()

    private val beamers = Array<TubeBeamerV2>()
    private val queuedBeams = Queue<TubeBeamerV2>()
    private val reusableIntArr = Array<Int>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)

            val failedKeys = Array<String>()

            animDefs.keys().map { it.name.lowercase() }.forEach { key ->
                val region = atlas.findRegion("$TAG/$key")

                if (region == null) {
                    failedKeys.add(key)
                    return@forEach
                }

                regions.put(key, region)
            }

            if (!failedKeys.isEmpty) throw IllegalStateException("Failed to fetch regions for keys: $failedKeys")
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

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }
        otherTimers.values().forEach { it.reset() }

        FacingUtils.setFacingOf(this, this::onChangeFacing)

        standCycles = -1
        jumpCycles = -1

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.BEAMER)) {
                val props = (value as RectangleMapObject).toProps()
                props.put(ConstKeys.OWNED, true)

                val beamer = MegaEntityFactory.fetch(TubeBeamerV2::class)!!
                beamer.spawn(props)

                beamers.add(beamer)
            }
        }
    }

    override fun isReady(delta: Float) = stateTimers[ReactorManState.INIT].isFinished()

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        projectiles.forEach { it.destroy() }
        projectiles.clear()
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

            currentStateTimer?.let {
                if (shouldUpdateTimer()) {
                    it.update(delta)
                    if (it.isFinished() && shouldTriggerNextStateOnTimerEnd()) stateMachine.next()
                }
            }

            if (canChangeFacing()) {
                val changeFacingDelay = otherTimers[CHANGE_FACING_DELAY_KEY]
                changeFacingDelay.update(delta)
                if (changeFacingDelay.isFinished()) FacingUtils.setFacingOf(this, this::onChangeFacing)
            }

            when (currentState) {
                ReactorManState.STAND ->
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.x = 0f

                ReactorManState.STAND_THROW, ReactorManState.STAND_THROW_TWO -> {
                    if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.x = 0f
                    otherTimers[STAND_THROW_DELAY_KEY].update(delta)
                }

                ReactorManState.JUMP -> if (shouldEndJump() || body.physics.velocity.y < 0f) stateMachine.next()

                ReactorManState.JUMP_THROW, ReactorManState.JUMP_THROW_TWO -> when {
                    !stateTimers[currentState].isFinished() -> body.physics.velocity.setZero()
                    shouldEndJump() -> stateMachine.next()
                }

                ReactorManState.RUN -> {
                    body.physics.velocity.set(RUN_SPEED * ConstVals.PPM * facing.value, 0f)
                    if (shouldStopRunning()) stateMachine.next()
                }

                ReactorManState.GIGA_STAND -> body.physics.velocity.setZero()

                ReactorManState.GIGA_RISE -> {
                    when {
                        !stateTimers[currentState].isFinished() -> {
                            val velY = min(
                                GIGA_RISE_MAX_VEL * ConstVals.PPM,
                                body.physics.velocity.y + (GIGA_RISE_IMPULSE * ConstVals.PPM * delta)
                            )
                            body.physics.velocity.set(0f, velY)
                        }
                        else -> {
                            body.physics.velocity.y -= GIGA_RISE_IMPULSE * ConstVals.PPM

                            if (body.physics.velocity.y > 0.1f * ConstVals.PPM) return@update

                            body.physics.velocity.setZero()

                            val delay = otherTimers[GIGA_DELAY_BETWEEN_BEAMS_KEY]
                            delay.update(delta)

                            if (delay.isFinished()) {
                                val beam = queuedBeams.removeFirst()
                                beam.startBeaming()

                                delay.reset()
                            }

                            if (queuedBeams.isEmpty) stateMachine.next()
                        }
                    }
                }

                else -> {}
            }

            if (projectiles.size >= 1 && projectiles[0].spawned)
                projectiles[0].body.setCenter(getProjectilePosition1())
            if (projectiles.size >= 2 && projectiles[1].spawned)
                projectiles[1].body.setCenter(getProjectilePosition2())
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM, 2f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM)
        )
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.25f * ConstVals.PPM)
        )
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            HeadUtils.stopJumpingIfHitHead(body)

            body.physics.gravityOn = shouldApplyGravity()

            if (body.physics.gravityOn) {
                val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
                body.physics.gravity.y = gravity * ConstVals.PPM
            }

            body.physics.applyFrictionX = shouldApplyFrictionX()
            body.physics.frictionOnSelf.x = getFrictionX()

            body.physics.applyFrictionY = shouldApplyFrictionY()
            body.physics.frictionOnSelf.y = getFrictionY()

            val shielded = currentState.equalsAny(ReactorManState.GIGA_STAND, ReactorManState.GIGA_RISE)
            body.fixtures[FixtureType.SHIELD].first().setActive(shielded)
            body.fixtures[FixtureType.DAMAGEABLE].first().setActive(!shielded)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink || game.isProperty(ConstKeys.ROOM_TRANSITION, true)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier key@{
                    if (defeated) return@key "defeated"

                    if (!body.isSensing(BodySense.FEET_ON_GROUND) &&
                        currentState.equalsAny(
                            ReactorManState.INIT,
                            ReactorManState.STAND,
                            ReactorManState.STAND_THROW,
                            ReactorManState.STAND_THROW_TWO
                        )
                    ) return@key "jump"

                    if (currentState.equalsAny(ReactorManState.STAND_THROW, ReactorManState.STAND_THROW_TWO) &&
                        !otherTimers[STAND_THROW_DELAY_KEY].isFinished()
                    ) return@key "stand"

                    if (currentState.equalsAny(ReactorManState.JUMP_THROW, ReactorManState.JUMP_THROW_TWO) &&
                        stateTimers[currentState].isFinished()
                    ) return@key "jump"

                    return@key currentState.name.lowercase()
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key.name.lowercase()
                        val (rows, columns, durations, loop) = entry.value
                        try {
                            val animation = Animation(regions[key], rows, columns, durations, loop)
                            animations.put(key, animation)
                        } catch (e: Exception) {
                            throw IllegalStateException("Failed to build animation for key: $key", e)
                        }
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<ReactorManState>()
        .initialState(ReactorManState.INIT)
        .onChangeState(this::onChangeState)
        // init
        .transition(ReactorManState.INIT, ReactorManState.STAND) { true }
        // stand
        .transition(ReactorManState.STAND, ReactorManState.GIGA_STAND) { shouldPerformGigaAttack() }
        .transition(ReactorManState.STAND, ReactorManState.STAND_THROW_TWO) { shouldPerformStandTwoThrow() }
        .transition(ReactorManState.STAND, ReactorManState.STAND_THROW) { true }
        // stand throw
        .transition(ReactorManState.STAND_THROW, ReactorManState.JUMP) { true }
        // stand throw two
        .transition(ReactorManState.STAND_THROW_TWO, ReactorManState.JUMP) { true }
        // jump
        .transition(ReactorManState.JUMP, ReactorManState.STAND) { shouldEndJump() }
        .transition(ReactorManState.JUMP, ReactorManState.JUMP_THROW) { jumpCycles % 2 == 0 }
        .transition(ReactorManState.JUMP, ReactorManState.JUMP_THROW_TWO) { true }
        // jump throw
        .transition(ReactorManState.JUMP_THROW, ReactorManState.STAND) { true }
        // jump throw two
        .transition(ReactorManState.JUMP_THROW_TWO, ReactorManState.STAND) { true }
        // giga stand
        .transition(ReactorManState.GIGA_STAND, ReactorManState.GIGA_RISE) { true }
        // giga rise
        .transition(ReactorManState.GIGA_RISE, ReactorManState.STAND) { true }
        // build
        .build()

    private fun onChangeState(current: ReactorManState, previous: ReactorManState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        if (previous.equalsAny(ReactorManState.GIGA_STAND, ReactorManState.GIGA_RISE)) standCycles = -1

        if (current != ReactorManState.INIT) stateTimers[current]?.reset()

        when (current) {
            ReactorManState.JUMP -> {
                jump()
                jumpCycles++
            }
            ReactorManState.STAND -> standCycles++
            ReactorManState.STAND_THROW -> {
                spawnProjectile()
                otherTimers[STAND_THROW_DELAY_KEY].reset()
            }
            ReactorManState.STAND_THROW_TWO -> {
                (0 until 2).forEach { spawnProjectile() }
                otherTimers[STAND_THROW_DELAY_KEY].reset()
            }
            ReactorManState.GIGA_RISE -> {
                queueBeams()
                otherTimers[GIGA_DELAY_BETWEEN_BEAMS_KEY].reset()
            }
            else -> {}
        }
    }

    private fun queueBeams() = (0 until GIGA_LASER_BEAM_SETS).forEach {
        (0 until beamers.size).forEach { reusableIntArr.add(it) }
        reusableIntArr.shuffle()

        (0 until GIGA_LASER_BEAMS_PER_SET).forEach {
            val index = reusableIntArr.pop()

            val beamer = beamers[index]
            queuedBeams.addLast(beamer)
        }

        reusableIntArr.clear()
    }

    private fun jump() {
        GameLogger.debug(TAG, "jump()")
        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getPosition(), megaman.body.getPosition(), JUMP_IMPULSE * ConstVals.PPM
        )
        body.physics.velocity.set(impulse)
    }

    private fun shouldApplyGravity(): Boolean {
        if (currentState == ReactorManState.GIGA_RISE) return false

        if (currentState.equalsAny(ReactorManState.JUMP_THROW, ReactorManState.JUMP_THROW_TWO))
            return stateTimers[currentState].isFinished()

        return true
    }

    private fun shouldApplyFrictionX() = true

    private fun getFrictionX() = 1f

    private fun shouldApplyFrictionY() = currentState != ReactorManState.INIT

    private fun getFrictionY() = 1f

    private fun shouldUpdateTimer() = when (currentState) {
        ReactorManState.INIT, ReactorManState.STAND, ReactorManState.RUN ->
            body.isSensing(BodySense.FEET_ON_GROUND)

        ReactorManState.STAND_THROW, ReactorManState.STAND_THROW_TWO ->
            body.isSensing(BodySense.FEET_ON_GROUND) && otherTimers[STAND_THROW_DELAY_KEY].isFinished()

        else -> true
    }

    private fun shouldTriggerNextStateOnTimerEnd() = when (currentState) {
        ReactorManState.JUMP_THROW, ReactorManState.JUMP_THROW_TWO -> shouldEndJump()
        else -> currentState != ReactorManState.GIGA_RISE
    }

    private fun shouldPerformStandTwoThrow() = standCycles > 0 && standCycles % 2 == 0

    private fun shouldPerformGigaAttack(): Boolean {
        if (standCycles < MIN_CYCLES_BEFORE_GIGA) return false

        val chance = UtilMethods.getRandom(0f, 100f)
        return chance < standCycles * GIGA_CHANCE_DELTA
    }

    private fun getProjectilePosition1() = when (currentState) {
        ReactorManState.STAND_THROW, ReactorManState.JUMP_THROW -> body.getPositionPoint(Position.TOP_CENTER).add(
            0.25f * ConstVals.PPM * -facing.value,
            (if (body.isSensing(BodySense.FEET_ON_GROUND)) 0.1f else 0.25f) * ConstVals.PPM
        )
        else -> body.getCenter().add(ConstVals.PPM.toFloat() * -facing.value, 0f)
    }

    private fun getProjectilePosition2() = when (currentState) {
        ReactorManState.STAND_THROW, ReactorManState.JUMP_THROW-> body.getPositionPoint(Position.TOP_CENTER).add(
            0.25f * ConstVals.PPM * facing.value,
            (if (body.isSensing(BodySense.FEET_ON_GROUND)) 0.1f else 0.25f) * ConstVals.PPM
        )
        else -> body.getCenter().add(ConstVals.PPM.toFloat() * facing.value, 0f)
    }

    private fun spawnProjectile() {
        val sizeBefore = projectiles.size

        val spawn = if (projectiles.isEmpty) getProjectilePosition1() else getProjectilePosition2()

        val projectile = MegaEntityFactory.fetch(ReactorManProjectile::class)!!
        projectile.spawn(
            props(
                ConstKeys.BIG pairTo true,
                ConstKeys.GROW pairTo true,
                ConstKeys.OWNER pairTo this,
                ConstKeys.ACTIVE pairTo false,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.GRAVITY_ON pairTo false
            )
        )

        projectiles.add(projectile)

        GameLogger.debug(
            TAG,
            "spawnProjectile(): projectiles.sizeBeforeSpawn=$sizeBefore, spawn=$spawn, projectiles=$projectiles"
        )
    }

    private fun throwOneProjectile() {
        if (projectiles.size != 1) throw IllegalStateException(
            "Should not call throwOneProjectile() if projectiles size = ${projectiles.size}"
        )

        val trajectory = megaman.body.getPositionPoint(Position.TOP_CENTER)
            .add(0f, 0.2f * ConstVals.PPM)
            .sub(body.getCenter())
            .nor()
            .scl(PROJ_SPEED * ConstVals.PPM)

        val projectile = projectiles.pop()
        projectile.body.physics.velocity.set(trajectory)
        projectile.body.physics.gravityOn = true
        projectile.active = true

        GameLogger.debug(TAG, "throwOneProjectile(): trajectory=$trajectory, projectile=$projectile")
    }

    private fun throwTwoProjectiles() {
        if (projectiles.size != 2) throw IllegalStateException(
            "Should not call throwTwoProjectiles() if projectiles size = ${projectiles.size}"
        )

        for (i in 0 until 2) {
            val trajectory = GameObjectPools.fetch(Vector2::class)
                .set((if (i == 0) -facing.value else facing.value).toFloat(), 0.25f)
                .nor().scl(PROJ_SPEED * ConstVals.PPM)

            val projectile = projectiles[i]
            projectile.body.physics.velocity.set(trajectory)
            projectile.body.physics.gravityOn = true
            projectile.active = true
        }

        projectiles.clear()
    }

    private fun shouldStopRunning() = FacingUtils.isFacingBlock(this)

    private fun shouldEndJump() = body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND)

    private fun canChangeFacing() = !currentState.equalsAny(ReactorManState.GIGA_STAND, ReactorManState.GIGA_RISE)

    private fun onChangeFacing(current: Facing, previous: Facing?) {
        GameLogger.debug(TAG, "onChangeFacing(): current=$current, previous=$previous, state=$currentState")
        otherTimers[CHANGE_FACING_DELAY_KEY].reset()
    }

    override fun getTag() = TAG
}
