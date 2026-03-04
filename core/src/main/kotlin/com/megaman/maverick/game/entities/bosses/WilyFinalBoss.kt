package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
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
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.world.body.getCenter

class WilyFinalBoss(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity {

    companion object {
        const val TAG = "WilyFinalBoss"

        private const val WILY_DEATH_PLANE_SPRITE_WIDTH = 16f
        private const val WILY_DEATH_PLANE_SPRITE_HEIGHT = 16f

        private const val FLY_IN_SPEED = 12f
        private const val FLY_IN_SLOW_DOWN_DISTANCE = 1.5f

        private const val MAX_FLY_BYS = 3
        private const val FLY_BY_SPEED = 12f
        private const val OFF_SCREEN_BUFFER = 4f

        private const val STATE_QUEUE_MAX_SIZE = 3

        private const val SWOOP_ENTRY_DELAY = 1.25f

        private const val DELAY_BETWEEN_STATES = 0.75f

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = orderedMapOf(
            "phase_1/fly_by" pairTo AnimationDef(),
            "phase_1/hover" pairTo AnimationDef(),
            "phase_1/lazors" pairTo AnimationDef(2, 1, 0.1f, true),
            "phase_1/open_hatch" pairTo AnimationDef(),
            "phase_1/shoot_missiles" pairTo AnimationDef(
                rows = 7,
                cols = 1,
                durations = gdxFilledArrayOf(7, 0.1f).also { it[3] = 1f },
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

    val delayBetweenStates = Timer(DELAY_BETWEEN_STATES)

    override fun init() {
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
        spawnProps.put(ConstKeys.MUSIC, MusicAsset.OUR_BLOOD_BOILS_MUSIC.name)
        spawnProps.put(ConstKeys.ORB, false)
        spawnProps.put(ConstKeys.END, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

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
                        WilyPhase1State.FLY_IN -> if (phase1Handler.flyInToTarget(delta)) phase1StateMachine.next()
                        WilyPhase1State.FLY_BY -> if (phase1Handler.updateFlyBy(delta)) phase1StateMachine.next()
                        WilyPhase1State.HOVER -> if (phase1Handler.hover(delta)) phase1StateMachine.next()
                        WilyPhase1State.FIRE_LAZORS -> {}
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

        body.preProcess.put(ConstKeys.DEFAULT) {
            when (currentPhase) {
                WilyFinalBossPhase.PHASE_1 -> {
                    val state =
                        stateMachines.get(currentPhase).getCurrentElement() as WilyPhase1State

                    if (state.equalsAny(WilyPhase1State.FLY_OUT, WilyPhase1State.SWOOP))
                        body.forEachFixture { it.setActive(false) }
                    else body.forEachFixture { it.setActive(true) }
                }
                else -> body.forEachFixture { it.setActive(true) }
            }
        }

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
                .also { sprite ->
                    sprite.setSize(
                        WILY_DEATH_PLANE_SPRITE_WIDTH * ConstVals.PPM,
                        WILY_DEATH_PLANE_SPRITE_HEIGHT * ConstVals.PPM
                    )
                }
        )
        .preProcess { _, sprite ->
            val center = body.getCenter().add(0f, 1f * ConstVals.PPM)
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
                            else -> 0
                        }
                        else -> 0
                    }

                    val flipX = when (state) {
                        WilyPhase1State.FLY_IN,
                        WilyPhase1State.FLY_BY -> body.physics.velocity.x > 0f
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
                        WilyPhase1State.FLY_IN ->
                            if (phase1Handler.flyInDecelerating) "tilt" else "fly_by"
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

    private inner class Phase1Handler : Resettable {

        // col 0 = left, col 1 = right; row 0 = bottom, row 1 = top
        val flyInStartPositions = Matrix<Vector2>(2, 2)
        val flyInTargetPositions = Matrix<Vector2>(2, 2)

        var currentFlyInCol = 0
        var currentFlyInRow = 1

        val swoopEntryDelayTimer = Timer(SWOOP_ENTRY_DELAY)

        var flyInDecelerating = false

        var flyByCount = 0
        var flyByDone = false
        var flyByMovingRight = false

        var currentFlyByStartCol = 0
        var currentFlyByStartRow = 0

        val stateQueue = Queue<WilyPhase1State>()

        val stateTimers = orderedMapOf(
            WilyPhase1State.SWOOP pairTo Timer(0.75f),
            WilyPhase1State.HOVER pairTo Timer(0.75f),
            WilyPhase1State.FLY_BY pairTo Timer(0.5f),
            WilyPhase1State.FLY_OUT pairTo Timer(0.1f),
            WilyPhase1State.SHOOT_MISSILES pairTo Timer(2f)
        )

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

        private fun shouldApplyStateDelay(
            current: WilyPhase1State,
            previous: WilyPhase1State
        ): Boolean {
            if (current == WilyPhase1State.FLY_OUT &&
                previous == WilyPhase1State.HOVER
            ) return false

            if (current.equalsAny(
                    WilyPhase1State.SWOOP,
                    WilyPhase1State.SHOOT_MISSILES
                )
            ) return true

            return false
        }

        fun onChangeState(current: WilyPhase1State, previous: WilyPhase1State) {
            GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

            if (shouldApplyStateDelay(current, previous)) delayBetweenStates.reset()
            else delayBetweenStates.setToEnd()

            if (stateTimers.containsKey(previous)) stateTimers[previous].reset()

            if (stateQueue.isEmpty) stateQueue.addFirst(current)
            stateQueue.addLast(current)
            if (stateQueue.size > STATE_QUEUE_MAX_SIZE) stateQueue.removeFirst()

            when (current) {
                WilyPhase1State.SWOOP -> {
                    val position = flyInTargetPositions[0, 1]!!
                    body.setCenter(position)

                    swoopEntryDelayTimer.reset()

                    requestToPlaySound(SoundAsset.JET_SOUND, false)
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
                    requestToPlaySound(SoundAsset.JET_SOUND, false)
                }
                WilyPhase1State.HOVER -> {
                    if (initSequence) {
                        GameLogger.debug(TAG, "onChangeState(): end init sequence")
                        initSequence = false
                    }
                }
                WilyPhase1State.FLY_OUT -> body.physics.velocity.setZero()
                else -> {}
            }
        }

        fun shouldSwoop() =
            !stateQueue.contains(WilyPhase1State.SWOOP) && UtilMethods.getRandomBool()

        fun shouldFlyBy() = !initSequence && UtilMethods.getRandomBool()

        fun shouldFlyOut() = currentFlyInRow == 1

        fun shouldShootMissiles() = UtilMethods.getRandomBool()

        fun shouldFireLazors() = false

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
                "beginFlyByFromHover(): col=$currentFlyByStartCol, row=$currentFlyByStartRow, movingRight=$flyByMovingRight"
            )

            requestToPlaySound(SoundAsset.JET_SOUND, false)
        }

        fun beginNextFlyBy() {
            flyByMovingRight = currentFlyByStartCol == 0

            body.setCenter(flyInStartPositions[currentFlyByStartCol, currentFlyByStartRow]!!)
            body.physics.velocity.x =
                if (flyByMovingRight) FLY_BY_SPEED * ConstVals.PPM else -FLY_BY_SPEED * ConstVals.PPM
            body.physics.velocity.y = 0f

            GameLogger.debug(
                TAG, "beginNextFlyBy(): flyByCount=$flyByCount, " +
                    "col=$currentFlyByStartCol, row=$currentFlyByStartRow, movingRight=$flyByMovingRight"
            )

            requestToPlaySound(SoundAsset.JET_SOUND, false)
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

            GameLogger.debug(TAG, "updateFlyBy(): fly-by complete, flyByCount=$flyByCount")

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
        }
    }

    private fun buildStateMachines() {
        stateMachines.put(WilyFinalBossPhase.PHASE_1, phase1Handler.buildStateMachine())
    }

    override fun getTag() = TAG
}
