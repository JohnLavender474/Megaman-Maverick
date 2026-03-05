package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.FLY_BY_SPEED
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.FLY_IN_SLOW_DOWN_DISTANCE
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.FLY_IN_SPEED
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.MAX_FLY_BYS
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.OFF_SCREEN_BUFFER
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.STATE_QUEUE_MAX_SIZE
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.WarningSign
import com.megaman.maverick.game.entities.hazards.WilyDeathPlaneLazor
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.min

class WilyFinalBoss(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity {

    companion object {
        const val TAG = "WilyFinalBoss"

        private const val WILY_DEATH_PLANE_SPRITE_WIDTH = 16f
        private const val WILY_DEATH_PLANE_SPRITE_HEIGHT = 16f

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = orderedMapOf(
            "phase_1/fly_by" pairTo AnimationDef(),
            "phase_1/hover" pairTo AnimationDef(),
            "phase_1/lazors" pairTo AnimationDef(3, 1, 0.1f, true),
            "phase_1/open_hatch" pairTo AnimationDef(),
            "phase_1/shoot_missiles" pairTo AnimationDef(
                rows = 3,
                cols = 3,
                durations = gdxFilledArrayOf(9, 0.1f).also { it[4] = 1f },
                loop = false
            ),
            "phase_1/swoop" pairTo AnimationDef(
                rows = 5,
                cols = 2,
                durations = gdxFilledArrayOf(10, 0.05f).also { it[0] = 0.25f },
                loop = false
            ),
            "phase_1/tilt" pairTo AnimationDef(),
            "phase_1/tilt_lazors" pairTo AnimationDef(2, 1, 0.1f, true),
            "phase_1/fly_out" pairTo AnimationDef(2, 1, 0.05f, false)
        )
    }

    private enum class WilyFinalBossPhase {
        PHASE_1, PHASE_2, PHASE_3
    }

    private enum class WilyPhase1State {
        SWOOP, HOVER, FLY_IN, FLY_BY, FLY_OUT, FIRE_LAZORS, SHOOT_MISSILES
    }

    private lateinit var currentPhase: WilyFinalBossPhase
    private val stateMachines = OrderedMap<WilyFinalBossPhase, StateMachine<*>>()

    private val phase1Handler = Phase1Handler()

    private var initSequence = false

    val delayBetweenStates = Timer()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.WILY_FINAL_BOSS.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion(it)) }
        }
        super.init()
        buildStateMachines()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.MUSIC, MusicAsset.MMX7_OUR_BLOOD_BOILS_MUSIC.name)
        spawnProps.put(ConstKeys.ORB, false)
        spawnProps.put(ConstKeys.END, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        phase1Handler.init(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val colRowOrder = gdxArrayOf(0 to 1, 1 to 1, 1 to 0, 0 to 0)
        for (i in 1..4) {
            val (col, row) = colRowOrder[i - 1]

            val flyInStart = spawnProps
                .get("fly_in_start_$i", RectangleMapObject::class)!!
                .rectangle.getCenter(false)
            phase1Handler.flyInStartPositions[col, row] = flyInStart

            val flyInTarget = spawnProps
                .get("fly_in_target_$i", RectangleMapObject::class)!!
                .rectangle.getCenter(false)
            phase1Handler.flyInTargetPositions[col, row] = flyInTarget
        }

        phase1Handler.lazorLeftBound.set(
            spawnProps.get("lazor_left_bound", RectangleMapObject::class)!!.rectangle.getCenter()
        )
        phase1Handler.lazorRightBound.set(
            spawnProps.get("lazor_right_bound", RectangleMapObject::class)!!.rectangle.getCenter()
        )

        currentPhase = WilyFinalBossPhase.PHASE_1
        stateMachines.values().forEach { it.reset() }

        initSequence = true

        delayBetweenStates.setToEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        phase1Handler.reset()
    }

    override fun onHealthDepleted() {
        if (currentPhase == WilyFinalBossPhase.PHASE_3) {
            GameLogger.debug(TAG, "onHealthDepleted(): reached final phase, dying")
            super.onHealthDepleted()
        } else {
            GameLogger.debug(TAG, "onHealthDepleted(): going to next phase")
            goToNextPhase()
        }
    }

    override fun onDefeated(delta: Float) {
        GameLogger.debug(TAG, "onDefeated()")
        super.onDefeated(delta)
    }

    override fun isReady(delta: Float) = !initSequence

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (defeated) {
                body.physics.velocity.setZero()
                explodeOnDefeat(delta)
                return@add
            }

            delayBetweenStates.update(delta)

            if (currentPhase == WilyFinalBossPhase.PHASE_1) {
                val state = (stateMachines.get(currentPhase) as StateMachine<WilyPhase1State>).getCurrentElement()
                phase1Handler.updateWarningSigns(state)
            }

            if (!delayBetweenStates.isFinished()) {
                body.physics.velocity.setZero()
                return@add
            }

            val currentStateMachine = stateMachines.get(currentPhase)
            when (currentPhase) {
                WilyFinalBossPhase.PHASE_1 -> {
                    val phase1StateMachine =
                        currentStateMachine as StateMachine<WilyPhase1State>

                    val state = phase1StateMachine.getCurrentElement()

                    val stateTime = phase1Handler.stateTimers[state]?.time
                    game.setDebugText("$state: $stateTime")

                    when (state) {
                        WilyPhase1State.SWOOP -> {
                            if (initSequence && !megaman.body.isSensing(BodySense.FEET_ON_GROUND)) return@add

                            if (phase1Handler.swoopIn(delta)) {
                                GameLogger.debug(TAG, "update(): phase 1 - swoop: go to next state")
                                phase1StateMachine.next()
                            }
                        }
                        WilyPhase1State.FLY_IN -> {
                            if (delayBetweenStates.isJustFinished()) requestToPlaySound(SoundAsset.JET_SOUND, false)
                            if (phase1Handler.flyInToTarget(delta)) phase1StateMachine.next()
                        }
                        WilyPhase1State.FLY_BY -> {
                            if (delayBetweenStates.isJustFinished()) requestToPlaySound(SoundAsset.JET_SOUND, false)
                            if (phase1Handler.updateFlyBy(delta)) phase1StateMachine.next()
                        }
                        WilyPhase1State.HOVER -> if (phase1Handler.hover(delta)) phase1StateMachine.next()
                        WilyPhase1State.FIRE_LAZORS -> {
                            if (phase1Handler.updateLazors(delta)) {
                                GameLogger.debug(TAG, "update(): phase 1 - fire lazors: go to next state")
                                phase1StateMachine.next()
                            }
                        }
                        WilyPhase1State.SHOOT_MISSILES -> {
                            body.physics.velocity.setZero()

                            val timer = phase1Handler.stateTimers[WilyPhase1State.SHOOT_MISSILES]
                            timer.update(delta)

                            if (timer.isFinished()) {
                                GameLogger.debug(TAG, "update(): phase 1 - shoot missiles: go to next state")
                                phase1StateMachine.next()
                            }
                        }
                        WilyPhase1State.FLY_OUT -> {
                            val timer = phase1Handler.stateTimers[WilyPhase1State.FLY_OUT]
                            timer.update(delta)

                            if (timer.isFinished()) {
                                GameLogger.debug(TAG, "update(): phase 1 - fly out: go to next state")
                                phase1StateMachine.next()
                            }
                        }
                    }
                }
                WilyFinalBossPhase.PHASE_2 -> TODO()
                WilyFinalBossPhase.PHASE_3 -> TODO()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.collisionOn = false
        body.physics.receiveFrictionX = false
        body.physics.receiveFrictionY = false
        body.setSize(6f * ConstVals.PPM, 4f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        val bodyDamager = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(bodyDamager)
        debugShapes.add add@{
            bodyDamager.drawingColor = if (bodyDamager.isActive()) Color.RED else Color.GRAY
            return@add bodyDamager
        }

        val flyByTailDamager = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(2f * ConstVals.PPM, 4f * ConstVals.PPM)
        )
        flyByTailDamager.offsetFromBodyAttachment.y = 2f * ConstVals.PPM
        body.addFixture(flyByTailDamager)
        debugShapes.add add@{
            flyByTailDamager.drawingColor = if (flyByTailDamager.isActive()) Color.RED else Color.GRAY
            return@add flyByTailDamager
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            when (currentPhase) {
                WilyFinalBossPhase.PHASE_1 -> {
                    val state =
                        stateMachines.get(currentPhase).getCurrentElement() as WilyPhase1State

                    if (state.equalsAny(WilyPhase1State.FLY_OUT, WilyPhase1State.SWOOP))
                        body.forEachFixture { it.setActive(false) }
                    else body.forEachFixture { it.setActive(true) }

                    if ((state == WilyPhase1State.FLY_BY && delayBetweenStates.isFinished()) ||
                        (state == WilyPhase1State.FLY_IN && !phase1Handler.flyInDecelerating)
                    ) {
                        bodyDamager.offsetFromBodyAttachment.setZero()

                        flyByTailDamager.setActive(true)
                        flyByTailDamager.offsetFromBodyAttachment.x = 4f * ConstVals.PPM
                        if ((state == WilyPhase1State.FLY_BY && phase1Handler.flyByMovingRight) ||
                            body.physics.velocity.x >= 0f
                        ) flyByTailDamager.offsetFromBodyAttachment.x *= -1f
                    } else {
                        bodyDamager.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM

                        flyByTailDamager.setActive(false)
                    }
                }
                else -> body.forEachFixture { it.setActive(true) }
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 0))
                .also { sprite ->
                    sprite.setSize(
                        WILY_DEATH_PLANE_SPRITE_WIDTH * ConstVals.PPM,
                        WILY_DEATH_PLANE_SPRITE_HEIGHT * ConstVals.PPM
                    )
                }
        )
        .preProcess { _, sprite ->
            val center = body.getCenter().add(0f, 0.5f * ConstVals.PPM)
            sprite.setCenter(center)

            when (currentPhase) {
                WilyFinalBossPhase.PHASE_1 -> {
                    val phase1StateMachine =
                        stateMachines.get(currentPhase) as StateMachine<WilyPhase1State>
                    val state = phase1StateMachine.getCurrentElement()

                    sprite.hidden = damageBlink || phase1Handler.stateTimers[state]?.isFinished() == true

                    if (!sprite.hidden && state == WilyPhase1State.SWOOP)
                        sprite.hidden = !delayBetweenStates.isFinished() ||
                            !phase1Handler.swoopEntryDelayTimer.isFinished()

                    if (!sprite.hidden && state == WilyPhase1State.FLY_IN)
                        sprite.hidden = !delayBetweenStates.isFinished()

                    sprite.priority.section = when (state) {
                        WilyPhase1State.SWOOP -> when {
                            phase1Handler.stateTimers[state].getRatio() < 0.8f -> DrawingSection.PLAYGROUND
                            else -> DrawingSection.FOREGROUND
                        }
                        WilyPhase1State.FLY_OUT -> DrawingSection.FOREGROUND
                        else -> DrawingSection.PLAYGROUND
                    }

                    sprite.priority.value = when (state) {
                        WilyPhase1State.SWOOP -> when {
                            phase1Handler.stateTimers[state].getRatio() < 0.8f -> -1
                            else -> 3
                        }
                        else -> 3
                    }

                    val flipX = when (state) {
                        WilyPhase1State.FLY_BY ->
                            if (!delayBetweenStates.isFinished()) phase1Handler.flyByMovingRight
                            else body.physics.velocity.x > 0f
                        WilyPhase1State.FLY_IN -> body.physics.velocity.x > 0f
                        else -> false
                    }
                    sprite.setFlip(flipX, false)
                }
                else -> {
                    sprite.priority.section = DrawingSection.PLAYGROUND
                    sprite.setFlip(false, false)
                    sprite.hidden = false
                }
            }
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    val prefix = currentPhase.name.lowercase()

                    val suffix = when (val state = stateMachines.get(currentPhase).getCurrentElement()) {
                        WilyPhase1State.FLY_BY ->
                            if (!delayBetweenStates.isFinished()) "tilt" else "fly_by"
                        WilyPhase1State.FLY_IN ->
                            if (phase1Handler.flyInDecelerating) "tilt" else "fly_by"
                        WilyPhase1State.FIRE_LAZORS ->
                            if (phase1Handler.lazorCompletionStarted) "hover" else "lazors"
                        else -> (state as Enum<*>).name.lowercase()
                    }

                    return@keySupplier "${prefix}/${suffix}"
                }
                .shouldAnimate shouldAnimate@{
                    return@shouldAnimate when (currentPhase) {
                        WilyFinalBossPhase.PHASE_1 -> {
                            val state =
                                stateMachines.get(currentPhase).getCurrentElement() as WilyPhase1State

                            if (state == WilyPhase1State.SWOOP)
                                return@shouldAnimate delayBetweenStates.isFinished() &&
                                    phase1Handler.swoopEntryDelayTimer.isFinished()

                            return@shouldAnimate true
                        }
                        else -> true
                    }
                }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun goToNextPhase() {
        val oldPhase = currentPhase
        currentPhase = WilyFinalBossPhase.entries[currentPhase.ordinal + 1]
        GameLogger.debug(TAG, "goToNextPhase(): oldPhase=$oldPhase, currentPhase=$currentPhase")
    }

    object Phase1ConstVals {
        const val INIT_SWOOP_CHANCE = 0.25f
        const val INIT_MISSILES_CHANCE = 0.25f
        const val INIT_LAZOR_CHANCE = 0.25f

        const val SWOOP_CHANCE_INCR = 0.25f
        const val MISSILES_CHANCE_INCR = 0.25f
        const val LAZOR_CHANCE_INCR = 0.25f

        const val FLY_IN_SPEED = 16f
        const val FLY_IN_SLOW_DOWN_DISTANCE = 1.5f

        const val MAX_FLY_BYS = 3
        const val FLY_BY_SPEED = 14f
        const val OFF_SCREEN_BUFFER = 4f

        const val STATE_QUEUE_MAX_SIZE = 4

        const val SWOOP_ENTRY_DELAY = 1.25f
        const val SWOOP_STATE_DUR = 0.75f
        const val HOVER_STATE_DUR = 0.75f
        const val FLY_BY_STATE_DUR = 0.5f
        const val FLY_OUT_STATE_DUR = 0.1f
        const val SHOOT_MISSILES_STATE_DUR = 2f

        const val LAZOR_MAX_PASSES = 2
        const val LAZOR_CENTER_EPSILON = 0.15f
        const val LAZOR_VERTICAL_BOB = 0.6f
        const val LAZOR_ANGULAR_SPEED = MathUtils.PI2 / 10f
        const val LAZOR_BOB_ANGULAR_SPEED_MULT = 4f
        const val LAZOR_START_PAUSE_TIME = 1f
        const val LAZOR_END_PAUSE_TIME = 0.5f
        const val LAZOR_NEAR_CENTER_SIN_THRESHOLD = 0.3f
        const val LAZOR_WARM_UP_START = 0.25f
        const val LAZOR_WARM_UP_RATE = 0.5f

        val WARNING_SIGN_KEYS = Array<String>()
            .also { keys ->
                Position.getDiagonalPositions().forEach {
                    keys.add("${it.name.lowercase()}_warning_sign")
                }
            }
            .also { keys ->
                for (i in 1..5) keys.add("ground_warning_sign_$i")
            }
    }

    private inner class Phase1Handler : Initializable, Resettable {

        // col 0 = left, col 1 = right; row 0 = bottom, row 1 = top
        val flyInStartPositions = Matrix<Vector2>(2, 2)
        val flyInTargetPositions = Matrix<Vector2>(2, 2)

        var currentFlyInCol = 0
        var currentFlyInRow = 1

        var swoopChance = Phase1ConstVals.INIT_SWOOP_CHANCE
        val swoopEntryDelayTimer = Timer(Phase1ConstVals.SWOOP_ENTRY_DELAY)
            .setRunOnJustFinished {
                MegaUtilMethods.delayRun(game, 0.1f) {
                    requestToPlaySound(SoundAsset.JET_SOUND, false)
                }
            }

        var flyInDecelerating = false

        var flyByCount = 0
        var flyByDone = false
        var flyByMovingRight = false

        var currentFlyByStartCol = 0
        var currentFlyByStartRow = 0

        var missilesChance = Phase1ConstVals.INIT_MISSILES_CHANCE

        val lazorLeftBound = Vector2()
        val lazorRightBound = Vector2()

        var lazorPasses = 0
        var lazorTheta = 0f

        val lazorAnchor = Vector2()

        var lazorDirectionSign = 1f

        var lazorNearCenter = false
        var lazorMovingRight = false
        var lazorCompletionStarted = false

        var lazorChance = Phase1ConstVals.INIT_LAZOR_CHANCE
        var lazorWarmUpScalar = Phase1ConstVals.LAZOR_WARM_UP_START

        private val lazorStartPauseTimer = Timer(Phase1ConstVals.LAZOR_START_PAUSE_TIME)
        private val lazorEndPauseTimer = Timer(Phase1ConstVals.LAZOR_END_PAUSE_TIME)

        var leftLazor: WilyDeathPlaneLazor? = null
        var rightLazor: WilyDeathPlaneLazor? = null

        val warningSigns = OrderedMap<String, WarningSign>()

        val stateQueue = Queue<WilyPhase1State>()

        val stateTimers = orderedMapOf(
            WilyPhase1State.SWOOP pairTo Timer(Phase1ConstVals.SWOOP_STATE_DUR),
            WilyPhase1State.HOVER pairTo Timer(Phase1ConstVals.HOVER_STATE_DUR),
            WilyPhase1State.FLY_BY pairTo Timer(Phase1ConstVals.FLY_BY_STATE_DUR),
            WilyPhase1State.FLY_OUT pairTo Timer(Phase1ConstVals.FLY_OUT_STATE_DUR),
            WilyPhase1State.SHOOT_MISSILES pairTo Timer(Phase1ConstVals.SHOOT_MISSILES_STATE_DUR)
        )

        override fun init(vararg params: Any) {
            GameLogger.debug(TAG, "Phase1Handler: init()")

            leftLazor = MegaEntityFactory.fetch(WilyDeathPlaneLazor::class)!!
            rightLazor = MegaEntityFactory.fetch(WilyDeathPlaneLazor::class)!!

            leftLazor!!.spawn(props(ConstKeys.OWNER pairTo this@WilyFinalBoss))
            rightLazor!!.spawn(props(ConstKeys.OWNER pairTo this@WilyFinalBoss))

            val spawnProps = params[0] as Properties

            Phase1ConstVals.WARNING_SIGN_KEYS.forEach { key ->
                val center = spawnProps.get(key, RectangleMapObject::class)!!.rectangle.getCenter()
                val warningSign = MegaEntityFactory.fetch(WarningSign::class)!!
                warningSign.spawn(props(ConstKeys.CENTER pairTo center))
                warningSigns.put(key, warningSign)
            }
        }

        fun buildStateMachine() = EnumStateMachineBuilder
            .create<WilyPhase1State>()
            .initialState(WilyPhase1State.SWOOP)
            .onChangeState(::onChangeState)
            // swoop
            .transition(WilyPhase1State.SWOOP, WilyPhase1State.FLY_BY) { shouldFlyBy() }
            .transition(WilyPhase1State.SWOOP, WilyPhase1State.FLY_IN) { true }
            // fly in
            .transition(WilyPhase1State.FLY_IN, WilyPhase1State.HOVER) { true }
            // fly by
            .transition(WilyPhase1State.FLY_BY, WilyPhase1State.SWOOP) { shouldSwoop() }
            .transition(WilyPhase1State.FLY_BY, WilyPhase1State.FLY_IN) { true }
            // hover
            .transition(WilyPhase1State.HOVER, WilyPhase1State.SHOOT_MISSILES) { shouldShootMissiles() }
            .transition(WilyPhase1State.HOVER, WilyPhase1State.FIRE_LAZORS) { shouldFireLazors() }
            .transition(WilyPhase1State.HOVER, WilyPhase1State.FLY_OUT) { shouldFlyOut() }
            .transition(WilyPhase1State.HOVER, WilyPhase1State.FLY_BY) { true }
            // shoot missiles
            .transition(WilyPhase1State.SHOOT_MISSILES, WilyPhase1State.FLY_BY) { true }
            // fire lazors
            .transition(WilyPhase1State.FIRE_LAZORS, WilyPhase1State.FLY_OUT) { true }
            // fly out
            .transition(WilyPhase1State.FLY_OUT, WilyPhase1State.SWOOP) { shouldSwoop() }
            .transition(WilyPhase1State.FLY_OUT, WilyPhase1State.FLY_BY) { shouldFlyBy() }
            .transition(WilyPhase1State.FLY_OUT, WilyPhase1State.FLY_IN) { true }
            // build
            .build()

        private fun getStateDelay(current: WilyPhase1State, previous: WilyPhase1State): Float {
            if (previous.equalsAny(WilyPhase1State.FLY_OUT, WilyPhase1State.SWOOP))
                return 0.5f

            if (current == WilyPhase1State.FLY_BY) return when {
                previous.equalsAny(WilyPhase1State.FLY_OUT, WilyPhase1State.SWOOP) -> 0.75f
                else -> 0.1f
            }

            if (current == WilyPhase1State.SWOOP && initSequence) return 0.25f

            if (current.equalsAny(WilyPhase1State.SWOOP, WilyPhase1State.SHOOT_MISSILES))
                return 0.5f

            return 0f
        }

        fun onChangeState(current: WilyPhase1State, previous: WilyPhase1State) {
            GameLogger.debug(TAG, "Phase1Handler: onChangeState(): current=$current, previous=$previous")

            delayBetweenStates.resetDuration(getStateDelay(current, previous))

            if (stateTimers.containsKey(previous)) stateTimers[previous].reset()

            if (stateQueue.isEmpty) stateQueue.addFirst(current)
            stateQueue.addLast(current)
            if (stateQueue.size > STATE_QUEUE_MAX_SIZE) stateQueue.removeFirst()

            when (current) {
                WilyPhase1State.SWOOP -> {
                    swoopChance = Phase1ConstVals.INIT_SWOOP_CHANCE

                    val position = flyInTargetPositions[0, 1]!!
                    body.setCenter(position)

                    swoopEntryDelayTimer.reset()
                }
                WilyPhase1State.FLY_BY -> {
                    flyByCount = 0
                    flyByDone = false

                    stateTimers[WilyPhase1State.FLY_BY].reset()

                    if (previous.equalsAny(WilyPhase1State.HOVER, WilyPhase1State.SHOOT_MISSILES))
                        beginFlyByFromHover()
                    else {
                        currentFlyByStartCol = 1 - currentFlyByStartCol
                        currentFlyByStartRow = UtilMethods.getRandom(0, 1)
                        beginNextFlyBy()
                    }
                }
                WilyPhase1State.FLY_IN -> {
                    if (initSequence) {
                        currentFlyInCol = 0
                        currentFlyInRow = 1
                    } else {
                        currentFlyInCol = UtilMethods.getRandom(0, 1)
                        currentFlyInRow = UtilMethods.getRandom(0, 1)
                    }

                    body.setCenter(flyInStartPositions[currentFlyInCol, currentFlyInRow]!!)
                }
                WilyPhase1State.HOVER -> {
                    if (initSequence) {
                        GameLogger.debug(TAG, "onChangeState(): end init sequence")
                        initSequence = false
                    }
                }
                WilyPhase1State.FIRE_LAZORS -> {
                    lazorChance = Phase1ConstVals.INIT_LAZOR_CHANCE

                    // increase the chance of the missiles so that it is more favorable than the lazors
                    missilesChance = min(1f, missilesChance + 2f * Phase1ConstVals.MISSILES_CHANCE_INCR)

                    startLazors()
                }
                WilyPhase1State.FLY_OUT -> {
                    requestToPlaySound(SoundAsset.JET_SOUND, false)
                    body.physics.velocity.setZero()
                }
                WilyPhase1State.SHOOT_MISSILES -> missilesChance = Phase1ConstVals.INIT_MISSILES_CHANCE
            }

            when (previous) {
                WilyPhase1State.HOVER ->
                    if (currentFlyInRow == 1 && current != WilyPhase1State.FIRE_LAZORS) {
                        lazorChance = min(1f, lazorChance + Phase1ConstVals.LAZOR_CHANCE_INCR)
                        GameLogger.debug(TAG, "Phase1Handler: onChangeState(): lazorChance=$lazorChance")
                    } else if (current != WilyPhase1State.SHOOT_MISSILES) {
                        missilesChance = min(1f, missilesChance + Phase1ConstVals.MISSILES_CHANCE_INCR)
                        GameLogger.debug(TAG, "Phase1Handler: onChangeState(): missilesChance=$missilesChance")
                    }
                WilyPhase1State.FIRE_LAZORS -> {
                    leftLazor?.on = false
                    rightLazor?.on = false
                }
                else -> {}
            }

            updateSwoopChance(current, previous)
        }

        private fun updateSwoopChance(current: WilyPhase1State, previous: WilyPhase1State) {
            // Increment when FLY_OUT skipped swoop in favour of FLY_BY
            if (current == WilyPhase1State.FLY_BY && previous == WilyPhase1State.FLY_OUT) {
                swoopChance = min(1f, swoopChance + Phase1ConstVals.SWOOP_CHANCE_INCR)
                return
            }

            // Increment when FLY_BY or FLY_OUT skipped swoop in favour of FLY_IN
            if (current == WilyPhase1State.FLY_IN &&
                previous.equalsAny(WilyPhase1State.FLY_BY, WilyPhase1State.FLY_OUT)
            ) swoopChance = min(1f, swoopChance + Phase1ConstVals.SWOOP_CHANCE_INCR)
        }

        fun shouldSwoop() =
            !stateQueue.contains(WilyPhase1State.SWOOP) && UtilMethods.getRandom(0f, 1f) <= swoopChance

        fun shouldFlyBy() = !initSequence && UtilMethods.getRandomBool()

        fun shouldFlyOut() = currentFlyInRow == 1 && !stateQueue.contains(WilyPhase1State.FLY_OUT)

        fun shouldShootMissiles() =
            !stateQueue.contains(WilyPhase1State.SHOOT_MISSILES) && UtilMethods.getRandom(0f, 1f) <= missilesChance

        fun shouldFireLazors() =
            stateQueue.size >= STATE_QUEUE_MAX_SIZE && // Do not trigger lazor too soon after boss spawns
                currentFlyInRow == 1 && UtilMethods.getRandom(0f, 1f) <= lazorChance

        private fun getGridSignKey(col: Int, row: Int) = when {
            col == 0 && row == 0 -> "bottom_left_warning_sign"
            col == 1 && row == 0 -> "bottom_right_warning_sign"
            col == 0 && row == 1 -> "top_left_warning_sign"
            else -> "top_right_warning_sign"
        }

        fun updateWarningSigns(state: WilyPhase1State) {
            warningSigns.values().forEach { it.on = false }

            when (state) {
                WilyPhase1State.FLY_IN ->
                    warningSigns[getGridSignKey(currentFlyInCol, currentFlyInRow)]?.on = true
                WilyPhase1State.FLY_BY ->
                    for (col in 0..1) warningSigns[getGridSignKey(col, currentFlyByStartRow)]?.on = true
                WilyPhase1State.SWOOP ->
                    for (i in 1..5) warningSigns["ground_warning_sign_$i"]?.on = true
                else -> {}
            }
        }

        fun hover(delta: Float): Boolean {
            body.physics.velocity.setZero()

            val timer = stateTimers[WilyPhase1State.HOVER]
            timer.update(delta)

            return timer.isFinished()
        }

        fun swoopIn(delta: Float): Boolean {
            body.physics.velocity.setZero()

            swoopEntryDelayTimer.update(delta)
            if (!swoopEntryDelayTimer.isFinished()) return false

            val timer = stateTimers[WilyPhase1State.SWOOP]
            timer.update(delta)

            return timer.isFinished()
        }

        fun flyInToTarget(delta: Float): Boolean {
            val center = body.getCenter()

            val target = flyInTargetPositions[currentFlyInCol, currentFlyInRow]!!

            if (center.epsilonEquals(target, 0.1f * ConstVals.PPM)) {
                body.setCenter(target)
                body.physics.velocity.setZero()
                return true
            }

            val toTarget = target.cpy().sub(center)
            val distance = toTarget.len()

            val slowDownThreshold = FLY_IN_SLOW_DOWN_DISTANCE * ConstVals.PPM

            flyInDecelerating = distance <= slowDownThreshold

            val speed = when {
                flyInDecelerating -> minOf(
                    FLY_IN_SPEED * ConstVals.PPM * (distance / slowDownThreshold),
                    distance / delta
                )
                else -> FLY_IN_SPEED * ConstVals.PPM
            }
            body.physics.velocity.set(toTarget.nor().scl(speed))

            return false
        }

        fun beginFlyByFromHover() {
            currentFlyByStartCol = currentFlyInCol
            currentFlyByStartRow = currentFlyInRow

            flyByMovingRight = currentFlyByStartCol == 0

            body.physics.velocity.x =
                if (flyByMovingRight) FLY_BY_SPEED * ConstVals.PPM else -FLY_BY_SPEED * ConstVals.PPM
            body.physics.velocity.y = 0f

            GameLogger.debug(
                TAG,
                "Phase1Handler: beginFlyByFromHover(): " +
                    "col=$currentFlyByStartCol, row=$currentFlyByStartRow, movingRight=$flyByMovingRight"
            )
        }

        fun beginNextFlyBy() {
            flyByMovingRight = currentFlyByStartCol == 0

            body.setCenter(flyInStartPositions[currentFlyByStartCol, currentFlyByStartRow]!!)
            body.physics.velocity.x =
                if (flyByMovingRight) FLY_BY_SPEED * ConstVals.PPM else -FLY_BY_SPEED * ConstVals.PPM
            body.physics.velocity.y = 0f

            GameLogger.debug(
                TAG, "Phase1Handler: beginNextFlyBy(): flyByCount=$flyByCount, " +
                    "col=$currentFlyByStartCol, row=$currentFlyByStartRow, movingRight=$flyByMovingRight"
            )

            if (flyByCount != 0) requestToPlaySound(SoundAsset.JET_SOUND, false)
        }

        fun updateFlyBy(delta: Float): Boolean {
            if (flyByDone) {
                val timer = stateTimers[WilyPhase1State.FLY_BY]
                timer.update(delta)
                return timer.isFinished()
            }

            body.physics.velocity.x =
                if (flyByMovingRight) FLY_BY_SPEED * ConstVals.PPM else -FLY_BY_SPEED * ConstVals.PPM
            body.physics.velocity.y = 0f

            val camBounds = game.getGameCamera().getRotatedBounds()
            val offScreen = when {
                flyByMovingRight -> body.getX() > camBounds.getMaxX() + OFF_SCREEN_BUFFER * ConstVals.PPM
                else -> body.getMaxX() < camBounds.getX() - OFF_SCREEN_BUFFER * ConstVals.PPM
            }

            if (!offScreen) return false

            flyByCount++

            GameLogger.debug(TAG, "Phase1Handler: updateFlyBy(): fly-by complete, flyByCount=$flyByCount")

            if (flyByCount >= MAX_FLY_BYS) {
                body.physics.velocity.setZero()
                flyByDone = true
                return false
            }

            currentFlyByStartRow = 1 - currentFlyByStartRow
            currentFlyByStartCol = 1 - currentFlyByStartCol

            beginNextFlyBy()

            return false
        }

        private fun startLazors() {
            GameLogger.debug(TAG, "Phase1Handler: startLazors()")

            lazorPasses = 0

            lazorTheta = 0f

            lazorCompletionStarted = false

            lazorNearCenter = false
            lazorMovingRight = false
            lazorWarmUpScalar = Phase1ConstVals.LAZOR_WARM_UP_START

            val centerX = (lazorLeftBound.x + lazorRightBound.x) / 2f
            lazorAnchor.set(centerX, body.getCenter().y)

            lazorDirectionSign = if (UtilMethods.getRandomBool()) 1f else -1f

            lazorStartPauseTimer.reset()
            lazorEndPauseTimer.reset()
        }

        fun updateLazors(delta: Float): Boolean {
            // If we've already completed the passes and are now pausing at end
            if (lazorCompletionStarted) {
                leftLazor?.on = false
                rightLazor?.on = false

                lazorEndPauseTimer.update(delta)
                body.physics.velocity.setZero()
                return lazorEndPauseTimer.isFinished()
            }

            // Short pause before the sine wave begins
            if (!lazorStartPauseTimer.isFinished()) {
                lazorStartPauseTimer.update(delta)
                body.setCenter(lazorAnchor)
                body.physics.velocity.setZero()
                return false
            }

            // Ramp up horizontal amplitude from Phase1ConstVals.LAZOR_WARM_UP_START to 1
            lazorWarmUpScalar = minOf(1f, lazorWarmUpScalar + Phase1ConstVals.LAZOR_WARM_UP_RATE * delta)

            // Continue sine movement
            lazorTheta += Phase1ConstVals.LAZOR_ANGULAR_SPEED * delta

            val halfSpan = (lazorRightBound.x - lazorLeftBound.x) / 2f
            val centerX = (lazorLeftBound.x + lazorRightBound.x) / 2f

            // Both x and y use sin so at theta=0 the position is exactly (centerX, lazorAnchor.y)
            val x = centerX + lazorDirectionSign * halfSpan * lazorWarmUpScalar * MathUtils.sin(lazorTheta)
            val y = lazorAnchor.y + Phase1ConstVals.LAZOR_VERTICAL_BOB * ConstVals.PPM *
                MathUtils.sin(lazorTheta * Phase1ConstVals.LAZOR_BOB_ANGULAR_SPEED_MULT)

            body.setCenter(x, y)
            body.physics.velocity.setZero()

            // Track near-center and direction of travel for the tilt animation
            val sinTheta = MathUtils.sin(lazorTheta)
            lazorNearCenter = abs(sinTheta) < Phase1ConstVals.LAZOR_NEAR_CENTER_SIN_THRESHOLD
            val cosTheta = MathUtils.cos(lazorTheta)
            lazorMovingRight = lazorDirectionSign * cosTheta > 0f

            lazorPasses = (lazorTheta / MathUtils.PI2).toInt()

            val nearCenter = MathUtils.isEqual(x, centerX, Phase1ConstVals.LAZOR_CENTER_EPSILON * ConstVals.PPM)
            if (lazorPasses >= Phase1ConstVals.LAZOR_MAX_PASSES && nearCenter) {
                body.physics.velocity.setZero()
                lazorCompletionStarted = true
                lazorEndPauseTimer.reset()
            }

            leftLazor?.on = true
            rightLazor?.on = true

            leftLazor?.body?.setTopCenterToPoint(x - 4f * ConstVals.PPM, y - 1f * ConstVals.PPM)
            rightLazor?.body?.setTopCenterToPoint(x + 4f * ConstVals.PPM, y - 1f * ConstVals.PPM)

            return false
        }

        override fun reset() {
            GameLogger.debug(TAG, "Phase1Handler: reset()")

            flyInStartPositions.forEach { _, _, v -> v?.let { GameObjectPools.free(it) } }
            flyInStartPositions.clear()

            flyInTargetPositions.forEach { _, _, v -> v?.let { GameObjectPools.free(it) } }
            flyInTargetPositions.clear()

            stateTimers.values().forEach { it.reset() }

            stateQueue.clear()

            currentFlyInCol = 0
            currentFlyInRow = 1

            swoopEntryDelayTimer.reset()

            flyByCount = 0
            flyByDone = false
            flyByMovingRight = false

            currentFlyByStartCol = 0
            currentFlyByStartRow = 0

            lazorChance = Phase1ConstVals.INIT_LAZOR_CHANCE
            missilesChance = Phase1ConstVals.INIT_MISSILES_CHANCE

            leftLazor?.destroy()
            leftLazor = null
            rightLazor?.destroy()
            rightLazor = null

            warningSigns.values().forEach { it.destroy() }
            warningSigns.clear()
        }
    }

    private fun buildStateMachines() {
        stateMachines.put(WilyFinalBossPhase.PHASE_1, phase1Handler.buildStateMachine())
    }

    override fun getTag() = TAG
}
