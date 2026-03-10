package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Resettable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.events.Event
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
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.FLY_IN_SLOW_DOWN_DISTANCE
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.MAX_FLY_BYS
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.OFF_SCREEN_BUFFER
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss.Phase1ConstVals.STATE_QUEUE_MAX_SIZE
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.WarningSign
import com.megaman.maverick.game.entities.decorations.WilySkullHead
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.GroundExplosion
import com.megaman.maverick.game.entities.hazards.WilyDeathPlaneLazor
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.HomingMissile
import com.megaman.maverick.game.entities.projectiles.WilyPlaneBomb
import com.megaman.maverick.game.entities.utils.hardMode
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import com.megaman.maverick.game.world.body.getCenter
import javax.crypto.spec.DESKeySpec
import kotlin.math.abs
import kotlin.math.min

class WilyFinalBoss(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity {

    companion object {
        const val TAG = "WilyFinalBoss"

        private const val WILY_DEATH_PLANE_LAZOR_RESIDUAL = "WilyDeathPlaneLazorResidual"

        private const val WILY_DEATH_PLANE_SPRITE_WIDTH = 16f
        private const val WILY_DEATH_PLANE_SPRITE_HEIGHT = 16f

        private val DESTROY_ON_TRANS = gdxArrayOf(HomingMissile.TAG, Bullet.TAG)

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = orderedMapOf(
            "phase_1/fly_by" pairTo AnimationDef(),
            "phase_1/hover" pairTo AnimationDef(),
            // "phase_1/lazors" pairTo AnimationDef(3, 1, 0.1f, true),
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
            // "phase_1/tilt_lazors" pairTo AnimationDef(2, 1, 0.1f, true),
            "phase_1/fly_out" pairTo AnimationDef(2, 1, 0.05f, false),
            "phase_1/open_mouth" pairTo AnimationDef(
                rows = 5,
                cols = 1,
                durations = gdxFilledArrayOf(5, 0.05f).also { it[2] = 0.25f },
                loop = false
            )
        )
    }

    private enum class WilyFinalBossPhase {
        PHASE_1, PHASE_2, PHASE_3
    }

    private enum class WilyPhase1State {
        SWOOP, HOVER, FLY_IN, FLY_BY, FLY_OUT, FIRE_LAZORS, SHOOT_MISSILES, DROP_BOMB
    }

    private enum class WilyPhase2State { HOVER }

    private enum class WilyPhaseTransState { INIT, FLY_UP, PAUSE, DROP_DOWN, END }

    private lateinit var currentPhase: WilyFinalBossPhase
    private val stateMachines = OrderedMap<WilyFinalBossPhase, StateMachine<*>>()
    private val delayBetweenStates = Timer()

    private val phaseTransitionHandler = PhaseTransitionHandler()

    private val phase1Handler = Phase1Handler()
    private val phase2Handler = Phase2Handler()

    private var initSequence = false

    private val room = GameRectangle()
    private val spawnCenter = Vector2()

    private val tempEntities = OrderedSet<MegaGameEntity>()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val bossAtlas = game.assMan.getTextureAtlas(TextureAsset.WILY_FINAL_BOSS.source)
            animDefs.keys().forEach { regions.put(it, bossAtlas.findRegion(it)) }

            val decorationsAtlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            regions.put(WILY_DEATH_PLANE_LAZOR_RESIDUAL, decorationsAtlas.findRegion(WILY_DEATH_PLANE_LAZOR_RESIDUAL))
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

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        spawnCenter.set(spawn)

        val room = spawnProps.get(ConstKeys.ROOM, RectangleMapObject::class)!!.rectangle
        this.room.set(room)

        phase1Handler.init(spawnProps)

        currentPhase = WilyFinalBossPhase.PHASE_1
        stateMachines.values().forEach { it.reset() }

        phaseTransitionHandler.reset()
        val skullHeadTarget = spawnProps
            .get("skull_head_target", RectangleMapObject::class)!!
            .rectangle.getCenter()
        phaseTransitionHandler.skullHeadTarget.set(skullHeadTarget)

        initSequence = true

        delayBetweenStates.setToEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        phase1Handler.reset()
    }

    override fun onEndBossSpawnEvent() {
        GameLogger.debug(TAG, "onEndBossSpawnEvent()")
        super.onEndBossSpawnEvent()
        phaseTransitionHandler.active = false
    }

    override fun playBossMusic() = game.audioMan.getCurrentMusicAsset() == null

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        if (phaseTransitionHandler.active) return false
        if (!super.canBeDamagedBy(damager)) return false
        return damager !is Explosion
    }

    override fun shouldTriggerDefeat(health: Int) =
        super.shouldTriggerDefeat(health) && !phaseTransitionHandler.active

    override fun triggerDefeat() {
        GameLogger.debug(TAG, "triggerDefeat(): currentPhase=$currentPhase")
        when (currentPhase) {
            WilyFinalBossPhase.PHASE_3 -> super.triggerDefeat()
            else -> phaseTransitionHandler.start()
        }
    }

    override fun onDefeated(delta: Float) {
        GameLogger.debug(TAG, "onDefeated()")
        super.onDefeated(delta)
    }

    override fun spawnDefeatExplosion() {
        val position = Position.entries.random()

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.POSITION pairTo body.getCenter().add(
                    (position.x - 1) * 2f * ConstVals.PPM, (position.y - 1) * 2f * ConstVals.PPM
                ),
                ConstKeys.DAMAGER pairTo false,
            )
        )

        playSoundNow(SoundAsset.EXPLOSION_2_SOUND, false)
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

            if (phaseTransitionHandler.active) {
                phaseTransitionHandler.update(delta)
                game.setDebugText("${phaseTransitionHandler.state}")
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

                        WilyPhase1State.DROP_BOMB -> {
                            if (delayBetweenStates.isJustFinished()) requestToPlaySound(SoundAsset.JET_SOUND, false)
                            if (phase1Handler.updateDropBomb(delta)) {
                                GameLogger.debug(TAG, "update(): phase 1 - drop bomb: go to next state")
                                phase1StateMachine.next()
                            }
                        }
                    }
                }

                WilyFinalBossPhase.PHASE_2 -> {
                    // TODO: implement behaviors for phase 2
                    game.setDebugText("PHASE 2")
                    body.physics.velocity.setZero()
                }

                WilyFinalBossPhase.PHASE_3 -> TODO()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.collisionOn = false
        body.physics.receiveFrictionX = false
        body.physics.receiveFrictionY = false
        body.setSize(8f * ConstVals.PPM, 4f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        // debugShapes.add { body.getBounds() }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        val headDamageable = Fixture(body, FixtureType.DAMAGEABLE, GameCircle())
        body.addFixture(headDamageable)
        headDamageable.drawingColor = Color.PURPLE
        debugShapes.add { headDamageable }

        val flyByUnderbellyDamager =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(6f * ConstVals.PPM, 1f * ConstVals.PPM))
        flyByUnderbellyDamager.attachedToBody = false
        body.addFixture(flyByUnderbellyDamager)
        debugShapes.add add@{
            flyByUnderbellyDamager.drawingColor = if (flyByUnderbellyDamager.isActive()) Color.RED else Color.GRAY
            return@add flyByUnderbellyDamager
        }

        val flyByBodyShield = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(4f * ConstVals.PPM))
        flyByBodyShield.attachedToBody = false
        body.addFixture(flyByBodyShield)
        debugShapes.add add@{
            flyByBodyShield.drawingColor = if (flyByBodyShield.isActive()) Color.BLUE else Color.GRAY
            return@add flyByBodyShield
        }

        val bodyDamager = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(bodyDamager)
        /*
        debugShapes.add add@{
            bodyDamager.drawingColor = if (bodyDamager.isActive()) Color.RED else Color.GRAY
            return@add bodyDamager
        }
         */

        val leftThrusterDamager =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM))
        leftThrusterDamager.attachedToBody = false
        body.addFixture(leftThrusterDamager)
        /*
        debugShapes.add add@{
            leftThrusterDamager.drawingColor = if (leftThrusterDamager.isActive()) Color.RED else Color.GRAY
            return@add leftThrusterDamager
        }
         */

        val leftThrusterShield =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM))
        leftThrusterShield.attachedToBody = false
        body.addFixture(leftThrusterShield)
        debugShapes.add add@{
            leftThrusterShield.drawingColor = if (leftThrusterShield.isActive()) Color.RED else Color.GRAY
            return@add leftThrusterShield
        }

        val rightThrusterDamager =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM))
        rightThrusterDamager.attachedToBody = false
        body.addFixture(rightThrusterDamager)
        /*
        debugShapes.add add@{
            rightThrusterDamager.drawingColor = if (rightThrusterDamager.isActive()) Color.RED else Color.GRAY
            return@add rightThrusterDamager
        }
         */

        val rightThrusterShield =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM))
        rightThrusterShield.attachedToBody = false
        body.addFixture(rightThrusterShield)
        debugShapes.add add@{
            rightThrusterShield.drawingColor = if (rightThrusterShield.isActive()) Color.RED else Color.GRAY
            return@add rightThrusterShield
        }

        val flyByTailDamager = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(2f * ConstVals.PPM, 4f * ConstVals.PPM)
        )
        flyByTailDamager.offsetFromBodyAttachment.y = 2f * ConstVals.PPM
        body.addFixture(flyByTailDamager)
        /*
        debugShapes.add add@{
            flyByTailDamager.drawingColor = if (flyByTailDamager.isActive()) Color.RED else Color.GRAY
            return@add flyByTailDamager
        }
         */

        val leftWingHoverDamager =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(3f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        leftWingHoverDamager.offsetFromBodyAttachment.set(-6f * ConstVals.PPM, 0f)
        body.addFixture(leftWingHoverDamager)
        /*
        debugShapes.add add@{
            leftWingHoverDamager.drawingColor = if (leftWingHoverDamager.isActive()) Color.RED else Color.GRAY
            return@add leftWingHoverDamager
        }
         */

        val leftWingHoverShield =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(3f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        leftWingHoverShield.offsetFromBodyAttachment.set(-6f * ConstVals.PPM, 0f)
        body.addFixture(leftWingHoverShield)
        debugShapes.add add@{
            leftWingHoverShield.drawingColor = if (leftWingHoverShield.isActive()) Color.BLUE else Color.GRAY
            return@add leftWingHoverShield
        }

        val rightWingHoverDamager =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(3f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        rightWingHoverDamager.offsetFromBodyAttachment.set(6f * ConstVals.PPM, 0f)
        body.addFixture(rightWingHoverDamager)
        /*
        debugShapes.add add@{
            rightWingHoverDamager.drawingColor = if (rightWingHoverDamager.isActive()) Color.RED else Color.GRAY
            return@add rightWingHoverDamager
        }
         */

        val rightWingHoverShield =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(3f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        rightWingHoverShield.offsetFromBodyAttachment.set(-6f * ConstVals.PPM, 0f)
        body.addFixture(rightWingHoverShield)
        debugShapes.add add@{
            rightWingHoverShield.drawingColor = if (rightWingHoverShield.isActive()) Color.BLUE else Color.GRAY
            return@add rightWingHoverShield
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (phaseTransitionHandler.active) {
                body.forEachFixture { it.setActive(false) }
                return@put
            }

            when (currentPhase) {
                WilyFinalBossPhase.PHASE_1 -> {
                    val state =
                        stateMachines.get(currentPhase).getCurrentElement() as WilyPhase1State

                    if (state == WilyPhase1State.FLY_OUT ||
                        (state == WilyPhase1State.SWOOP &&
                            phase1Handler.stateTimers[state].time !in 0.45f..0.65f)
                    ) {
                        body.forEachFixture { it.setActive(false) }
                        return@put
                    } else body.forEachFixture { it.setActive(true) }

                    if ((state == WilyPhase1State.FLY_BY && delayBetweenStates.isFinished()) ||
                        (state == WilyPhase1State.FLY_IN && !phase1Handler.flyInDecelerating) ||
                        state == WilyPhase1State.DROP_BOMB
                    ) {
                        bodyDamager.offsetFromBodyAttachment.setZero()

                        flyByTailDamager.setActive(true)
                        flyByTailDamager.offsetFromBodyAttachment.x = 4f * ConstVals.PPM
                        if (body.physics.velocity.x >= 0f) flyByTailDamager.offsetFromBodyAttachment.x *= -1f

                        flyByUnderbellyDamager.setActive(true)
                        (flyByUnderbellyDamager.rawShape as GameRectangle)
                            .setTopCenterToPoint(body.getPositionPoint(Position.BOTTOM_CENTER))
                    } else {
                        bodyDamager.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM
                        flyByTailDamager.setActive(false)
                        flyByUnderbellyDamager.setActive(false)
                    }

                    val hovering = state.equalsAny(
                        WilyPhase1State.HOVER,
                        WilyPhase1State.SWOOP,
                        WilyPhase1State.FIRE_LAZORS,
                        WilyPhase1State.SHOOT_MISSILES,
                    ) || (state == WilyPhase1State.FLY_IN && phase1Handler.flyInDecelerating)

                    if (hovering) {
                        (headDamageable.rawShape as GameCircle).setRadius(2.5f * ConstVals.PPM)
                        headDamageable.offsetFromBodyAttachment.setZero()
                        flyByBodyShield.setActive(false)
                    } else {
                        (headDamageable.rawShape as GameCircle).setRadius(3f * ConstVals.PPM)
                        headDamageable.offsetFromBodyAttachment.x = 2f * ConstVals.PPM
                        val left = body.physics.velocity.x < 0f
                        if (left) headDamageable.offsetFromBodyAttachment.x *= -1f
                        headDamageable.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM

                        flyByBodyShield.setActive(true)
                        (flyByBodyShield.rawShape as GameRectangle).let {
                            val position = if (left) Position.CENTER_LEFT else Position.CENTER_RIGHT
                            val point = (headDamageable.rawShape as GameCircle)
                                .getBoundingRectangle()
                                .getPositionPoint(position.opposite())
                            it.positionOnPoint(point, position)
                        }
                    }

                    leftWingHoverDamager.setActive(hovering)
                    leftWingHoverShield.setActive(hovering)
                    rightWingHoverDamager.setActive(hovering)
                    rightWingHoverShield.setActive(hovering)

                    if (hovering) {
                        leftThrusterDamager.setActive(true)
                        (leftThrusterDamager.rawShape as GameRectangle)
                            .setCenterX(body.getMaxX())
                            .setMaxY(body.getMaxY() - 1f * ConstVals.PPM)
                        (leftThrusterShield.rawShape as GameRectangle)
                            .setCenterX(body.getMaxX())
                            .setMaxY(body.getMaxY() - 1f * ConstVals.PPM)

                        rightThrusterDamager.setActive(true)
                        (rightThrusterDamager.rawShape as GameRectangle)
                            .setCenterX(body.getMaxX())
                            .setMaxY(body.getMaxY() - 1f * ConstVals.PPM)
                        (rightThrusterShield.rawShape as GameRectangle)
                            .setCenterX(body.getMaxX())
                            .setMaxY(body.getMaxY() - 1f * ConstVals.PPM)
                    } else {
                        leftThrusterDamager.setActive(false)
                        leftThrusterShield.setActive(false)
                        rightThrusterDamager.setActive(false)
                        rightThrusterShield.setActive(false)
                    }
                }

                else -> body.forEachFixture { it.setActive(false) }
            }
        }

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
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

            if (phaseTransitionHandler.active) {
                sprite.setFlip(false, false)
                sprite.priority.section = DrawingSection.PLAYGROUND
                sprite.priority.value = 3
                sprite.hidden = when (phaseTransitionHandler.state) {
                    WilyPhaseTransState.FLY_UP -> damageBlink || body.getY() > room.getMaxY()
                    WilyPhaseTransState.PAUSE -> true
                    else -> damageBlink
                }
                return@preProcess
            } else when (currentPhase) {
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
                        WilyPhase1State.FLY_BY, WilyPhase1State.DROP_BOMB ->
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
        .sprite(
            WILY_DEATH_PLANE_LAZOR_RESIDUAL,
            GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
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

            val visible = when (currentPhase) {
                WilyFinalBossPhase.PHASE_1 -> {
                    val phase1StateMachine =
                        stateMachines.get(currentPhase) as StateMachine<WilyPhase1State>
                    val state = phase1StateMachine.getCurrentElement()
                    state == WilyPhase1State.FIRE_LAZORS && !phase1Handler.lazorCompletionStarted
                }

                else -> false
            }
            sprite.hidden = !visible
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    if (phaseTransitionHandler.active || currentPhase == WilyFinalBossPhase.PHASE_2)
                        return@keySupplier "phase_1/hover"

                    val prefix = currentPhase.name.lowercase()

                    val suffix = when (val state = stateMachines.get(currentPhase).getCurrentElement()) {
                        WilyPhase1State.FLY_BY ->
                            if (!delayBetweenStates.isFinished()) "tilt" else "fly_by"

                        WilyPhase1State.FLY_IN ->
                            if (phase1Handler.flyInDecelerating) "tilt" else "fly_by"

                        WilyPhase1State.FIRE_LAZORS ->
                            if (phase1Handler.shootBulletsTimer.isFinished()) "hover" else "open_mouth"

                        WilyPhase1State.DROP_BOMB ->
                            if (phase1Handler.dropBombHatchOpen) "open_hatch" else "fly_by"

                        else -> (state as Enum<*>).name.lowercase()
                    }

                    return@keySupplier "${prefix}/${suffix}"
                }
                .shouldAnimate shouldAnimate@{
                    if (phaseTransitionHandler.active) return@shouldAnimate true

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
        .key(WILY_DEATH_PLANE_LAZOR_RESIDUAL)
        .animator(Animator(Animation(regions[WILY_DEATH_PLANE_LAZOR_RESIDUAL], 2, 1, 0.1f, true)))
        .build()

    private fun startNextPhase() {
        val oldPhase = currentPhase
        currentPhase = WilyFinalBossPhase.entries[currentPhase.ordinal + 1]
        GameLogger.debug(TAG, "goToNextPhase(): oldPhase=$oldPhase, currentPhase=$currentPhase")
    }

    object Phase1ConstVals {
        const val MAX_ATTACK_CHANCE = 0.8f

        const val INIT_SWOOP_CHANCE = 0.2f
        const val INIT_MISSILES_CHANCE = 0.2f
        const val INIT_LAZOR_CHANCE = 0.2f
        const val INIT_DROP_BOMB_CHANCE = 0.2f

        const val SWOOP_CHANCE_INCR = 0.2f
        const val LAZOR_CHANCE_INCR = 0.2f
        const val MISSILES_CHANCE_INCR = 0.2f
        const val DROP_BOMB_CHANCE_INCR = 0.2f

        const val FLY_IN_SPEED = 16f
        const val FLY_IN_SPEED_HARD = 20f
        const val FLY_IN_SLOW_DOWN_DISTANCE = 1.5f

        const val MAX_FLY_BYS = 3
        const val FLY_BY_SPEED = 16f
        const val FLY_BY_SPEED_HARD = 20f
        const val FLY_BY_CHANCE = 0.5f
        const val FLY_BY_AFTER_SWOOP_REPEAT_CHANCE = 0.15f
        const val OFF_SCREEN_BUFFER = 4f

        const val STATE_QUEUE_MAX_SIZE = 4

        const val SWOOP_ENTRY_DELAY = 0.75f
        const val SWOOP_STATE_DUR = 0.75f
        const val HOVER_STATE_DUR = 1.0f
        const val HOVER_STATE_DUR_HARD = 0.75f
        const val FLY_BY_STATE_DUR = 0.5f
        const val FLY_OUT_STATE_DUR = 0.1f
        const val SHOOT_MISSILES_STATE_DUR = 2.5f
        const val SHOOT_MISSILES_STATE_DUR_HARD = 2f

        const val FLY_IN_STATE_DELAY = 1.0f
        const val FLY_IN_STATE_DELAY_HARD = 0.75f
        const val FLY_BY_FROM_OUT_SWOOP_STATE_DELAY = 1.25f
        const val FLY_BY_FROM_OUT_SWOOP_STATE_DELAY_HARD = 1f
        const val SHOOT_MISSILES_STATE_DELAY = 0.4f
        const val SHOOT_MISSILES_STATE_DELAY_HARD = 0.25f
        const val DROP_BOMB_STATE_DELAY = 1f
        const val DROP_BOMB_STATE_DELAY_HARD = 0.75f
        const val BOMB_RUN_DELAY = 0.75f

        const val LAZOR_MAX_PASSES = 3
        const val LAZOR_CENTER_EPSILON = 0.15f
        const val LAZOR_VERTICAL_BOB = 0.6f
        const val LAZOR_ANGULAR_SPEED = MathUtils.PI2 / 10f
        const val LAZOR_BOB_ANGULAR_SPEED_MULT = 4f
        const val LAZOR_START_PAUSE_TIME = 1f
        const val LAZOR_END_PAUSE_TIME = 0.5f
        const val LAZOR_NEAR_CENTER_SIN_THRESHOLD = 0.3f
        const val LAZOR_WARM_UP_START = 0.25f
        const val LAZOR_WARM_UP_RATE = 0.5f

        const val SHOOT_BULLETS_DELAY = 1.25f
        const val SHOOT_BULLETS_DELAY_HARD = 0.75f
        const val SHOOT_BULLETS_DUR = 0.5f
        const val BULLET_SPEED = 10f
        val BULLET_TRAJECTORIES_1 = gdxArrayOf(
            Vector2(-1f, -1f),
            Vector2(-0.75f, -1f),
            Vector2(0.75f, -1f),
            Vector2(1f, -1f)
        )
        val BULLET_TRAJECTORIES_2 = gdxArrayOf(
            Vector2(-1f, -1f),
            Vector2(-0.1f, -1f),
            Vector2(0.1f, -1f),
            Vector2(1f, -1f)
        )
        val BULLET_TRAJECTORIES_1_HARD = gdxArrayOf(
            Vector2(-0.75f, -1f),
            Vector2(-0.5f, -1f),
            Vector2(-0.05f, -1f),
            Vector2(0.05f, -1f),
            Vector2(0.5f, -1f),
            Vector2(0.75f, -1f)
        )
        val BULLET_TRAJECTORIES_2_HARD = gdxArrayOf(
            Vector2(-1f, -1f),
            Vector2(-0.75f, -1f),
            Vector2(-0.5f, -1f),
            Vector2(0.5f, -1f),
            Vector2(0.75f, -1f),
            Vector2(1f, -1f)
        )

        val MISSILE_ANGLES = gdxArrayOf(225f, 180f, 180f, 135f)
        const val TIME_BEFORE_FIRST_RECALC = 1f
        const val TIME_BEFORE_FIRST_RECALC_HARD = 0.5f

        const val FLY_IN_ALT_ROW_CHANCE = 0.67f

        const val GROUND_WARNING_SIGN_START_INDEX = 1
        const val GROUND_WARNING_SIGN_END_INDEX = 11
        const val GROUND_WARNING_SIGNS_TO_BLOW = 4
        const val GROUND_WARNING_SIGNS_TO_BLOW_HARD = 5
        val WARNING_SIGN_KEYS = Array<String>()
            .also { keys ->
                Position.getDiagonalPositions().forEach {
                    keys.add("${it.name.lowercase()}_warning_sign")
                }
            }
            .also { keys ->
                for (i in GROUND_WARNING_SIGN_START_INDEX..GROUND_WARNING_SIGN_END_INDEX) keys
                    .add("ground_warning_sign_$i")
            }
            .also { keys ->
                keys.add("bomb_left_warning_sign")
                keys.add("bomb_right_warning_sign")
            }
    }

    private inner class Phase1Handler : Initializable, Resettable {

        val attackChances = orderedMapOf(
            WilyPhase1State.SWOOP pairTo Phase1ConstVals.INIT_SWOOP_CHANCE,
            WilyPhase1State.SHOOT_MISSILES pairTo Phase1ConstVals.INIT_MISSILES_CHANCE,
            WilyPhase1State.FIRE_LAZORS pairTo Phase1ConstVals.INIT_LAZOR_CHANCE,
            WilyPhase1State.DROP_BOMB pairTo Phase1ConstVals.INIT_DROP_BOMB_CHANCE,
        )

        var hoverPatternIndex = 0

        val flyInStartPositions = Matrix<Vector2>(2, 2)
        val flyInTargetPositions = Matrix<Vector2>(2, 2)
        var currentFlyInCol = 0
        var currentFlyInRow = 1
        var flyInDecelerating = false

        var flyByCount = 0
        var flyByDone = false
        var flyByMovingRight = false
        var flyByRenderOnlyOneWarning = false
        var currentFlyByStartCol = 0
        var currentFlyByStartRow = 0

        var swoopExplosionIndices = gdxArrayOf<Int>()
        var lastSwoopFollowedByFlyBy = false
        val swoopEntryDelayTimer = Timer(Phase1ConstVals.SWOOP_ENTRY_DELAY)
            .setRunOnJustFinished {
                MegaUtilMethods.delayRun(game, 0.1f) {
                    requestToPlaySound(SoundAsset.JET_SOUND, false)
                }
            }

        val bombStartLeft = Vector2()
        val bombStartRight = Vector2()
        var dropBombHatchOpen = false
        var bombDropIndex = 0
        val bombXTriggers = Array<Float>()
        val bombDelay = Timer(Phase1ConstVals.BOMB_RUN_DELAY)

        val lazorLeftBound = Vector2()
        val lazorRightBound = Vector2()
        var lazorLeft = true
        var lazorPasses = 0
        var lazorTheta = 0f
        val lazorAnchor = Vector2()
        var lazorDirectionSign = 1f
        var lazorNearCenter = false
        var lazorMovingRight = false
        var lazorCompletionStarted = false
        var lazorWarmUpScalar = Phase1ConstVals.LAZOR_WARM_UP_START
        val lazorStartPauseTimer = Timer(Phase1ConstVals.LAZOR_START_PAUSE_TIME)
        val lazorEndPauseTimer = Timer(Phase1ConstVals.LAZOR_END_PAUSE_TIME)
        var leftLazor: WilyDeathPlaneLazor? = null
        var rightLazor: WilyDeathPlaneLazor? = null

        var bulletTraj1 = true
        val shootBulletsDelay = Timer()
        val shootBulletsTimer = Timer(Phase1ConstVals.SHOOT_BULLETS_DUR)
            .setToEnd()
            .addRunnable(TimeMarkedRunnable(0.15f) { shootBullets() })

        val warningSigns = OrderedMap<String, WarningSign>()

        val stateQueue = Queue<WilyPhase1State>()
        val stateTimers = orderedMapOf(
            WilyPhase1State.SWOOP pairTo Timer(Phase1ConstVals.SWOOP_STATE_DUR),
            WilyPhase1State.HOVER pairTo Timer(Phase1ConstVals.HOVER_STATE_DUR),
            WilyPhase1State.FLY_BY pairTo Timer(Phase1ConstVals.FLY_BY_STATE_DUR),
            WilyPhase1State.FLY_OUT pairTo Timer(Phase1ConstVals.FLY_OUT_STATE_DUR),
            WilyPhase1State.SHOOT_MISSILES pairTo Timer(Phase1ConstVals.SHOOT_MISSILES_STATE_DUR)
                .addRunnable(TimeMarkedRunnable(0.45f) {
                    shootMissiles(currentFlyInRow == 0)
                })
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

            val colRowOrder = gdxArrayOf(0 to 1, 1 to 1, 1 to 0, 0 to 0)
            for (i in 1..4) {
                val (col, row) = colRowOrder[i - 1]

                val flyInStart = spawnProps
                    .get("fly_in_start_$i", RectangleMapObject::class)!!
                    .rectangle.getCenter(false)
                flyInStartPositions[col, row] = flyInStart

                val flyInTarget = spawnProps
                    .get("fly_in_target_$i", RectangleMapObject::class)!!
                    .rectangle.getCenter(false)
                flyInTargetPositions[col, row] = flyInTarget
            }

            lazorLeftBound.set(
                spawnProps.get("lazor_left_bound", RectangleMapObject::class)!!.rectangle.getCenter()
            )
            lazorRightBound.set(
                spawnProps.get("lazor_right_bound", RectangleMapObject::class)!!.rectangle.getCenter()
            )

            bombStartLeft.set(
                spawnProps.get("bomb_start_left", RectangleMapObject::class)!!.rectangle.getCenter()
            )
            bombStartRight.set(
                spawnProps.get("bomb_start_right", RectangleMapObject::class)!!.rectangle.getCenter()
            )

            for (i in 1..3) bombXTriggers.add(
                spawnProps.get("bomb_pos_$i", RectangleMapObject::class)!!.rectangle.getCenter().x
            )
        }

        fun buildStateMachine() = EnumStateMachineBuilder
            .create<WilyPhase1State>()
            .initialState(WilyPhase1State.SWOOP)
            .onChangeState(::onChangeState)
            // swoop
            .transition(WilyPhase1State.SWOOP, WilyPhase1State.FLY_BY) { shouldFlyByAfterSwoop() }
            .transition(WilyPhase1State.SWOOP, WilyPhase1State.FLY_IN) { true }
            // fly in
            .transition(WilyPhase1State.FLY_IN, WilyPhase1State.HOVER) { true }
            // fly by
            .transition(WilyPhase1State.FLY_BY, WilyPhase1State.SWOOP) { shouldSwoop() }
            .transition(WilyPhase1State.FLY_BY, WilyPhase1State.FLY_IN) { true }
            // hover
            .transition(WilyPhase1State.HOVER, WilyPhase1State.FIRE_LAZORS) { shouldFireLazors() }
            .transition(WilyPhase1State.HOVER, WilyPhase1State.SHOOT_MISSILES) { shouldShootMissiles() }
            .transition(WilyPhase1State.HOVER, WilyPhase1State.FLY_OUT) { shouldFlyOut() }
            .transition(WilyPhase1State.HOVER, WilyPhase1State.FLY_BY) { true }
            // shoot missiles
            .transition(WilyPhase1State.SHOOT_MISSILES, WilyPhase1State.FLY_BY) { true }
            // drop bomb
            .transition(WilyPhase1State.DROP_BOMB, WilyPhase1State.FLY_IN) { true }
            // fire lazors
            .transition(WilyPhase1State.FIRE_LAZORS, WilyPhase1State.FLY_OUT) { true }
            // fly out
            .transition(WilyPhase1State.FLY_OUT, WilyPhase1State.DROP_BOMB) { shouldDropBomb() }
            .transition(WilyPhase1State.FLY_OUT, WilyPhase1State.SWOOP) { shouldSwoop() }
            .transition(WilyPhase1State.FLY_OUT, WilyPhase1State.FLY_BY) { shouldFlyBy() }
            .transition(WilyPhase1State.FLY_OUT, WilyPhase1State.FLY_IN) { true }
            // build
            .build()

        private fun getStateDelay(current: WilyPhase1State, previous: WilyPhase1State): Float {
            val hardMode = game.state.hardMode

            if (previous == WilyPhase1State.DROP_BOMB) return 1f

            if (current == WilyPhase1State.FLY_IN)
                return if (hardMode) Phase1ConstVals.FLY_IN_STATE_DELAY_HARD else Phase1ConstVals.FLY_IN_STATE_DELAY

            if (current == WilyPhase1State.FLY_BY) return when {
                previous.equalsAny(WilyPhase1State.FLY_OUT, WilyPhase1State.SWOOP) ->
                    if (hardMode) Phase1ConstVals.FLY_BY_FROM_OUT_SWOOP_STATE_DELAY_HARD
                    else Phase1ConstVals.FLY_BY_FROM_OUT_SWOOP_STATE_DELAY

                previous.equalsAny(WilyPhase1State.HOVER, WilyPhase1State.SHOOT_MISSILES) -> 0.1f
                else -> 0.25f
            }

            if (current == WilyPhase1State.SWOOP && initSequence) return 0.25f

            if (current == WilyPhase1State.SHOOT_MISSILES)
                return if (hardMode) Phase1ConstVals.SHOOT_MISSILES_STATE_DELAY_HARD
                else Phase1ConstVals.SHOOT_MISSILES_STATE_DELAY

            if (current == WilyPhase1State.DROP_BOMB)
                return if (hardMode) Phase1ConstVals.DROP_BOMB_STATE_DELAY_HARD
                else Phase1ConstVals.DROP_BOMB_STATE_DELAY

            if (current == WilyPhase1State.SWOOP)
                return 0.1f

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
                    resetChance(WilyPhase1State.SWOOP, Phase1ConstVals.INIT_SWOOP_CHANCE)

                    swoopExplosionIndices.clear()
                    val signsToBlow = if (game.state.hardMode) Phase1ConstVals.GROUND_WARNING_SIGNS_TO_BLOW_HARD
                    else Phase1ConstVals.GROUND_WARNING_SIGNS_TO_BLOW
                    (Phase1ConstVals.GROUND_WARNING_SIGN_START_INDEX..Phase1ConstVals.GROUND_WARNING_SIGN_END_INDEX)
                        .shuffled()
                        .take(signsToBlow)
                        .forEach { swoopExplosionIndices.add(it) }

                    val position = flyInTargetPositions[0, 1]!!
                    body.setCenter(position)

                    swoopEntryDelayTimer.reset()
                }

                WilyPhase1State.FLY_BY -> {
                    flyByCount = 0
                    flyByDone = false

                    incrementChance(WilyPhase1State.SHOOT_MISSILES, Phase1ConstVals.MISSILES_CHANCE_INCR)
                    incrementChance(WilyPhase1State.FIRE_LAZORS, Phase1ConstVals.LAZOR_CHANCE_INCR)
                    incrementChance(WilyPhase1State.DROP_BOMB, Phase1ConstVals.DROP_BOMB_CHANCE_INCR)

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
                        currentFlyInRow = when {
                            UtilMethods.getRandom(0f, 1f) <=
                                Phase1ConstVals.FLY_IN_ALT_ROW_CHANCE -> 1 - currentFlyInRow

                            else -> currentFlyInRow
                        }
                    }
                    body.setCenter(flyInStartPositions[currentFlyInCol, currentFlyInRow]!!)
                }

                WilyPhase1State.HOVER -> {
                    stateTimers[WilyPhase1State.HOVER]!!.resetDuration(
                        if (game.state.hardMode) Phase1ConstVals.HOVER_STATE_DUR_HARD else Phase1ConstVals.HOVER_STATE_DUR
                    )
                    if (initSequence) {
                        GameLogger.debug(TAG, "onChangeState(): end init sequence")
                        initSequence = false
                    }
                }

                WilyPhase1State.FIRE_LAZORS -> {
                    resetChance(WilyPhase1State.FIRE_LAZORS, Phase1ConstVals.INIT_LAZOR_CHANCE)
                    incrementChance(WilyPhase1State.SHOOT_MISSILES, Phase1ConstVals.MISSILES_CHANCE_INCR)
                    incrementChance(WilyPhase1State.DROP_BOMB, Phase1ConstVals.DROP_BOMB_CHANCE_INCR)
                    startLazors()
                }

                WilyPhase1State.FLY_OUT -> {
                    body.physics.velocity.setZero()
                    requestToPlaySound(SoundAsset.JET_SOUND, false)
                    incrementChance(WilyPhase1State.FIRE_LAZORS, Phase1ConstVals.LAZOR_CHANCE_INCR)
                    incrementChance(WilyPhase1State.SHOOT_MISSILES, Phase1ConstVals.MISSILES_CHANCE_INCR)
                    incrementChance(WilyPhase1State.DROP_BOMB, Phase1ConstVals.DROP_BOMB_CHANCE_INCR)
                }

                WilyPhase1State.SHOOT_MISSILES -> {
                    stateTimers[WilyPhase1State.SHOOT_MISSILES]!!.resetDuration(
                        if (game.state.hardMode) Phase1ConstVals.SHOOT_MISSILES_STATE_DUR_HARD
                        else Phase1ConstVals.SHOOT_MISSILES_STATE_DUR
                    )
                    resetChance(WilyPhase1State.SHOOT_MISSILES, Phase1ConstVals.INIT_MISSILES_CHANCE)
                    incrementChance(WilyPhase1State.FIRE_LAZORS, Phase1ConstVals.LAZOR_CHANCE_INCR)
                    incrementChance(WilyPhase1State.DROP_BOMB, Phase1ConstVals.DROP_BOMB_CHANCE_INCR)
                }

                WilyPhase1State.DROP_BOMB -> {
                    resetChance(WilyPhase1State.DROP_BOMB, Phase1ConstVals.INIT_DROP_BOMB_CHANCE)
                    incrementChance(WilyPhase1State.SHOOT_MISSILES, Phase1ConstVals.MISSILES_CHANCE_INCR)
                    incrementChance(WilyPhase1State.FIRE_LAZORS, Phase1ConstVals.LAZOR_CHANCE_INCR)
                    beginDropBomb()
                }
            }

            when (previous) {
                WilyPhase1State.HOVER -> hoverPatternIndex++
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
                incrementChance(WilyPhase1State.SWOOP, Phase1ConstVals.SWOOP_CHANCE_INCR)
                return
            }

            // Increment when FLY_BY or FLY_OUT skipped swoop in favour of FLY_IN
            if (current == WilyPhase1State.FLY_IN &&
                previous.equalsAny(WilyPhase1State.FLY_BY, WilyPhase1State.FLY_OUT)
            ) incrementChance(WilyPhase1State.SWOOP, Phase1ConstVals.SWOOP_CHANCE_INCR)

            // Track whether the last swoop was followed by a fly-by
            if (previous == WilyPhase1State.SWOOP) lastSwoopFollowedByFlyBy = current == WilyPhase1State.FLY_BY
        }

        fun shouldSwoop() =
            !stateQueue.contains(WilyPhase1State.SWOOP) && rollChance(WilyPhase1State.SWOOP)

        fun shouldFlyBy() = !initSequence && UtilMethods.getRandomBool()

        fun shouldFlyByAfterSwoop(): Boolean {
            if (initSequence) return false

            val chance = when {
                lastSwoopFollowedByFlyBy -> Phase1ConstVals.FLY_BY_AFTER_SWOOP_REPEAT_CHANCE
                else -> Phase1ConstVals.FLY_BY_CHANCE
            }

            return UtilMethods.getRandom(0f, 1f) <= chance
        }

        fun shouldFlyOut() =
            currentFlyInRow == 1 &&
                UtilMethods.getRandomBool() &&
                !stateQueue.contains(WilyPhase1State.FLY_OUT)

        fun shouldShootMissiles() = (hoverPatternIndex % 4 == 1 || hoverPatternIndex % 4 == 3) &&
            rollChance(WilyPhase1State.SHOOT_MISSILES)

        fun shouldFireLazors() =
            (hoverPatternIndex % 4 == 3 || rollChance(WilyPhase1State.FIRE_LAZORS)) &&
                stateQueue.size >= STATE_QUEUE_MAX_SIZE && // Do not trigger lazor too soon after boss spawns
                !stateQueue.contains(WilyPhase1State.FIRE_LAZORS) && currentFlyInRow == 1

        fun shouldDropBomb() =
            stateQueue.size >= Phase1ConstVals.STATE_QUEUE_MAX_SIZE &&
                !stateQueue.contains(WilyPhase1State.DROP_BOMB) &&
                rollChance(WilyPhase1State.DROP_BOMB) &&
                currentFlyInRow == 1

        fun resetChance(state: WilyPhase1State, value: Float) {
            attackChances.put(state, value)
        }

        fun incrementChance(state: WilyPhase1State, amount: Float) {
            attackChances.put(state, min(Phase1ConstVals.MAX_ATTACK_CHANCE, attackChances[state]!! + amount))
        }

        fun rollChance(state: WilyPhase1State) = UtilMethods.getRandom(0f, 1f) <= attackChances[state]!!

        private fun getGridSignKey(col: Int, row: Int) = when {
            col == 0 && row == 0 -> "bottom_left_warning_sign"
            col == 1 && row == 0 -> "bottom_right_warning_sign"
            col == 0 && row == 1 -> "top_left_warning_sign"
            else -> "top_right_warning_sign"
        }

        fun updateWarningSigns(state: WilyPhase1State) {
            warningSigns.values().forEach { it.on = false }

            if (initSequence) return

            when (state) {
                WilyPhase1State.FLY_IN ->
                    warningSigns[getGridSignKey(currentFlyInCol, currentFlyInRow)]?.on = true

                WilyPhase1State.FLY_BY ->
                    if (flyByRenderOnlyOneWarning) {
                        val destCol = if (flyByMovingRight) 1 else 0
                        warningSigns[getGridSignKey(destCol, currentFlyByStartRow)]?.on = true
                    } else for (col in 0..1) warningSigns[getGridSignKey(col, currentFlyByStartRow)]?.on = true

                WilyPhase1State.DROP_BOMB -> if (delayBetweenStates.isFinished()) {
                    warningSigns["bomb_left_warning_sign"]?.on = true
                    warningSigns["bomb_right_warning_sign"]?.on = true
                    warningSigns["ground_warning_sign_1"]?.on = true
                    warningSigns["ground_warning_sign_6"]?.on = true
                    warningSigns["ground_warning_sign_11"]?.on = true
                }

                WilyPhase1State.SWOOP ->
                    swoopExplosionIndices.forEach { i -> warningSigns["ground_warning_sign_$i"]?.on = true }

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
            if (!initSequence && timer.isAtBeginning()) loadSwoopExplosions()
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

            val flyInSpeed =
                if (game.state.hardMode) Phase1ConstVals.FLY_IN_SPEED_HARD else Phase1ConstVals.FLY_IN_SPEED
            val speed = when {
                flyInDecelerating -> minOf(
                    flyInSpeed * ConstVals.PPM * (distance / slowDownThreshold),
                    distance / delta
                )

                else -> flyInSpeed * ConstVals.PPM
            }
            body.physics.velocity.set(toTarget.nor().scl(speed))

            return false
        }

        private fun applyFlyByVelocity() {
            val flyBySpeed =
                if (game.state.hardMode) Phase1ConstVals.FLY_BY_SPEED_HARD else Phase1ConstVals.FLY_BY_SPEED
            body.physics.velocity.x = if (flyByMovingRight) flyBySpeed * ConstVals.PPM else -flyBySpeed * ConstVals.PPM
            body.physics.velocity.y = 0f
        }

        fun beginFlyByFromHover() {
            flyByRenderOnlyOneWarning = true
            currentFlyByStartCol = currentFlyInCol
            currentFlyByStartRow = currentFlyInRow
            flyByMovingRight = currentFlyByStartCol == 0
            applyFlyByVelocity()
            GameLogger.debug(
                TAG,
                "Phase1Handler: beginFlyByFromHover(): " +
                    "col=$currentFlyByStartCol, row=$currentFlyByStartRow, movingRight=$flyByMovingRight"
            )
        }

        fun beginNextFlyBy() {
            flyByRenderOnlyOneWarning = false
            flyByMovingRight = currentFlyByStartCol == 0
            body.setCenter(flyInStartPositions[currentFlyByStartCol, currentFlyByStartRow]!!)
            applyFlyByVelocity()
            GameLogger.debug(
                TAG, "Phase1Handler: beginNextFlyBy(): flyByCount=$flyByCount, " +
                    "col=$currentFlyByStartCol, row=$currentFlyByStartRow, movingRight=$flyByMovingRight"
            )
            if (flyByCount != 0) requestToPlaySound(SoundAsset.JET_SOUND, false)
        }

        fun beginDropBomb() {
            currentFlyByStartCol = UtilMethods.getRandom(0, 1)
            currentFlyByStartRow = 1

            dropBombHatchOpen = false
            bombDropIndex = if (currentFlyByStartCol == 1) 2 else 0

            flyByMovingRight = currentFlyByStartCol == 0

            val startPos = if (flyByMovingRight) bombStartLeft else bombStartRight
            body.setCenter(startPos)

            GameLogger.debug(
                TAG, "Phase1Handler: beginDropBomb(): " +
                    "col=$currentFlyByStartCol, movingRight=$flyByMovingRight"
            )
        }

        fun updateFlyBy(delta: Float): Boolean {
            if (flyByDone) {
                val timer = stateTimers[WilyPhase1State.FLY_BY]
                timer.update(delta)
                return timer.isFinished()
            }

            applyFlyByVelocity()

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

        fun updateDropBomb(delta: Float): Boolean {
            bombDelay.update(delta)
            if (!bombDelay.isFinished()) {
                body.physics.velocity.setZero()
                return false
            }

            applyFlyByVelocity()

            if (bombDropIndex in 0 until bombXTriggers.size) {
                val nextX = bombXTriggers[bombDropIndex]
                val crossed = when {
                    flyByMovingRight -> body.getCenter().x >= nextX + ConstVals.PPM
                    else -> body.getCenter().x <= nextX - ConstVals.PPM
                }
                if (crossed) {
                    dropBombHatchOpen = true

                    val position = GameObjectPools.fetch(Vector2::class)
                        .set(body.getPositionPoint(Position.BOTTOM_CENTER))
                        .add(1f * ConstVals.PPM * if (flyByMovingRight) -1f else 1f, -1f * ConstVals.PPM)
                    dropBomb(position)

                    if (flyByMovingRight) bombDropIndex++ else bombDropIndex--
                }
            }

            val camBounds = game.getGameCamera().getRotatedBounds()
            return when {
                flyByMovingRight -> body.getX() > camBounds.getMaxX() + OFF_SCREEN_BUFFER * ConstVals.PPM
                else -> body.getMaxX() < camBounds.getX() - OFF_SCREEN_BUFFER * ConstVals.PPM
            }
        }

        fun dropBomb(position: Vector2) {
            GameLogger.debug(TAG, "Phase1Handler: dropBomb()")

            val bomb = MegaEntityFactory.fetch(WilyPlaneBomb::class)!!
            bomb.spawn(
                props(
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.OWNER pairTo this@WilyFinalBoss,
                )
            )

            requestToPlaySound(SoundAsset.POUND_SOUND, false)
        }

        private fun startLazors() {
            GameLogger.debug(TAG, "Phase1Handler: startLazors()")

            lazorPasses = 0
            lazorTheta = 0f
            lazorNearCenter = false
            lazorMovingRight = false
            lazorCompletionStarted = false
            lazorWarmUpScalar = Phase1ConstVals.LAZOR_WARM_UP_START

            val centerX = (lazorLeftBound.x + lazorRightBound.x) / 2f
            lazorAnchor.set(centerX, body.getCenter().y)

            lazorDirectionSign = if (lazorLeft) -1f else 1f
            lazorLeft = !lazorLeft

            lazorStartPauseTimer.reset()
            lazorEndPauseTimer.reset()

            shootBulletsDelay.resetDuration(
                if (game.state.hardMode) Phase1ConstVals.SHOOT_BULLETS_DELAY_HARD
                else Phase1ConstVals.SHOOT_BULLETS_DELAY
            )
            shootBulletsTimer.setToEnd()

            bulletTraj1 = true
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
            lazorStartPauseTimer.update(delta)
            if (!lazorStartPauseTimer.isFinished()) {
                body.setCenter(lazorAnchor)
                body.physics.velocity.setZero()
                return false
            }

            if (lazorStartPauseTimer.isJustFinished()) requestToPlaySound(SoundAsset.LASER_BEAM_SOUND, false)

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

            lazorPasses = (lazorTheta / MathUtils.PI).toInt()

            val nearCenter = MathUtils.isEqual(x, centerX, Phase1ConstVals.LAZOR_CENTER_EPSILON * ConstVals.PPM)
            if (lazorPasses >= Phase1ConstVals.LAZOR_MAX_PASSES && nearCenter) {
                body.physics.velocity.setZero()
                lazorCompletionStarted = true
                lazorEndPauseTimer.reset()
            }

            leftLazor?.on = true
            rightLazor?.on = true

            leftLazor?.body?.setTopCenterToPoint(x - 4f * ConstVals.PPM, y - 2f * ConstVals.PPM)
            rightLazor?.body?.setTopCenterToPoint(x + 4f * ConstVals.PPM, y - 2f * ConstVals.PPM)

            shootBulletsDelay.update(delta)
            if (shootBulletsDelay.isJustFinished()) shootBulletsTimer.reset()
            if (shootBulletsDelay.isFinished()) {
                shootBulletsTimer.update(delta)
                if (shootBulletsTimer.isFinished()) shootBulletsDelay.reset()
            }

            return false
        }

        fun shootBullets() {
            GameLogger.debug(TAG, "Phase1Handler: shootBullets()")

            val center = body.getCenter().sub(0f, 1.75f * ConstVals.PPM)

            val trajectories = when {
                game.state.hardMode -> when {
                    bulletTraj1 -> Phase1ConstVals.BULLET_TRAJECTORIES_1_HARD
                    else -> Phase1ConstVals.BULLET_TRAJECTORIES_2_HARD
                }

                bulletTraj1 -> Phase1ConstVals.BULLET_TRAJECTORIES_1
                else -> Phase1ConstVals.BULLET_TRAJECTORIES_2
            }

            trajectories.forEach {
                val trajectory = GameObjectPools.fetch(Vector2::class)
                    .set(it)
                    .scl(Phase1ConstVals.BULLET_SPEED * ConstVals.PPM)

                val bullet = MegaEntityFactory.fetch(Bullet::class)!!
                bullet.spawn(
                    props(
                        ConstKeys.COLLIDE pairTo false,
                        ConstKeys.POSITION pairTo center,
                        ConstKeys.TYPE pairTo Bullet.LAZOR,
                        ConstKeys.TRAJECTORY pairTo trajectory,
                        ConstKeys.OWNER pairTo this@WilyFinalBoss,
                    )
                )
            }

            bulletTraj1 = !bulletTraj1

            requestToPlaySound(SoundAsset.SOLAR_BLAZE_SOUND, false)
        }

        fun shootMissiles(up: Boolean) {
            for (i in 0 until Phase1ConstVals.MISSILE_ANGLES.size) {
                val bodyCenter = body.getCenter()

                val offsetY = if (up) -1.5f else -2f
                val position = when (i) {
                    0 -> bodyCenter.add(-1f * ConstVals.PPM, offsetY * ConstVals.PPM)
                    1 -> bodyCenter.add(-0.5f * ConstVals.PPM, offsetY * ConstVals.PPM)
                    2 -> bodyCenter.add(0.5f * ConstVals.PPM, offsetY * ConstVals.PPM)
                    3 -> bodyCenter.add(1f * ConstVals.PPM, offsetY * ConstVals.PPM)
                    else -> throw IllegalStateException("Index not supported: $i")
                }

                var angle = Phase1ConstVals.MISSILE_ANGLES[if (up) 3 - i else i].toInt()
                if (up) angle = (angle + 180) % 360

                var initDelay = when {
                    game.state.hardMode -> Phase1ConstVals.TIME_BEFORE_FIRST_RECALC_HARD
                    else -> Phase1ConstVals.TIME_BEFORE_FIRST_RECALC
                }
                if (i == 0 || i == 3) initDelay *= 0.25f

                val missile = MegaEntityFactory.fetch(HomingMissile::class)!!
                missile.spawn(
                    props(
                        ConstKeys.ANGLE pairTo angle,
                        ConstKeys.POSITION pairTo position,
                        ConstKeys.OWNER pairTo this@WilyFinalBoss,
                    "${ConstKeys.INIT}_${ConstKeys.DELAY}" pairTo initDelay
                    )
                )
            }

            requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)
        }

        fun loadSwoopExplosions() {
            for (i in swoopExplosionIndices) {
                val key = "ground_warning_sign_$i"

                val warningSign = warningSigns.get(key)

                val bottomCenter = GameObjectPools.fetch(Vector2::class, false)
                    .set(warningSign.center.x, warningSign.center.y - 0.5f * ConstVals.PPM)

                val delay = UtilMethods.getRandom(0.5f, 0.75f)

                MegaUtilMethods.delayRun(game, delay) {
                    val explosion = MegaEntityFactory.fetch(GroundExplosion::class)!!
                    explosion.spawn(
                        props(
                            ConstKeys.POSITION pairTo bottomCenter,
                            ConstKeys.OWNER pairTo this@WilyFinalBoss
                        )
                    )

                    GameObjectPools.free(bottomCenter)
                }

                MegaUtilMethods.delayRun(game, delay) { requestToPlaySound(SoundAsset.ASTEROID_EXPLODE_SOUND, false) }
            }
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
            flyByRenderOnlyOneWarning = false

            currentFlyByStartCol = 0
            currentFlyByStartRow = 0

            attackChances.put(WilyPhase1State.SWOOP, Phase1ConstVals.INIT_SWOOP_CHANCE)
            attackChances.put(WilyPhase1State.SHOOT_MISSILES, Phase1ConstVals.INIT_MISSILES_CHANCE)
            attackChances.put(WilyPhase1State.FIRE_LAZORS, Phase1ConstVals.INIT_LAZOR_CHANCE)
            attackChances.put(WilyPhase1State.DROP_BOMB, Phase1ConstVals.INIT_DROP_BOMB_CHANCE)

            dropBombHatchOpen = false
            bombDropIndex = 0
            bombXTriggers.clear()

            leftLazor?.destroy()
            leftLazor = null
            rightLazor?.destroy()
            rightLazor = null

            lazorLeft = true

            warningSigns.values().forEach { it.destroy() }
            warningSigns.clear()

            shootBulletsDelay.reset()
            shootBulletsTimer.setToEnd()
        }
    }

    object PhaseTransitionConstVals {
        const val EXPLODE_DUR = 3f
        const val FLY_UP_SPEED = 16f
        const val PAUSE_DUR = 2f
        const val DROP_DOWN_SPEED = 8f
    }

    private inner class PhaseTransitionHandler : Updatable, Resettable {

        var active = false
        var state = WilyPhaseTransState.INIT

        val explodeTimer = Timer(PhaseTransitionConstVals.EXPLODE_DUR)
        val pauseTimer = Timer(PhaseTransitionConstVals.PAUSE_DUR)

        val skullHeadTarget = Vector2()

        fun start() {
            GameLogger.debug(TAG, "PhaseTransitionHandler: start()")

            active = true
            explodeTimer.reset()
            state = WilyPhaseTransState.INIT

            phase1Handler.leftLazor?.on = false
            phase1Handler.rightLazor?.on = false
            phase1Handler.warningSigns.values().forEach { it.on = false }

            body.physics.velocity.setZero()

            game.eventsMan.submitEvent(
                Event(
                    EventType.TURN_CONTROLLER_OFF, props(
                        "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.OFF}" pairTo false
                    )
                )
            )
            megaman.body.physics.velocity.x = 0f
            megaman.canBeDamaged = false

            val entitiesToDestroy = MegaGameEntities.getOfTags(tempEntities, DESTROY_ON_TRANS)
            entitiesToDestroy.forEach { it.destroy() }
            entitiesToDestroy.clear()
        }

        override fun update(delta: Float) {
            when (state) {
                WilyPhaseTransState.INIT -> {
                    body.physics.velocity.setZero()

                    // Reset the damage timer to activate the default sprite blink for when the entity is damaged
                    if (damageTimer.isFinished()) damageTimer.reset()

                    explodeOnDefeat(delta)
                    explodeTimer.update(delta)
                    if (explodeTimer.isFinished()) {
                        GameLogger.debug(TAG, "PhaseTransitionHandler: EXPLODING -> FLY_UP")
                        state = WilyPhaseTransState.FLY_UP
                    }
                }

                WilyPhaseTransState.FLY_UP -> {
                    body.physics.velocity.set(0f, PhaseTransitionConstVals.FLY_UP_SPEED * ConstVals.PPM)

                    // Reset the damage timer to activate the default sprite blink for when the entity is damaged
                    if (damageTimer.isFinished()) damageTimer.reset()

                    explodeOnDefeat(delta)
                    if (body.getY() - 5f * ConstVals.PPM > room.getMaxY()) {
                        GameLogger.debug(TAG, "PhaseTransitionHandler: FLY_UP -> PAUSE")

                        body.setCenter(spawnCenter.x, room.getMaxY() + body.getHeight())
                        body.physics.velocity.setZero()

                        state = WilyPhaseTransState.PAUSE
                        pauseTimer.reset()
                        spawnSkullHead()
                    }
                }

                WilyPhaseTransState.PAUSE -> {
                    damageTimer.setToEnd()

                    pauseTimer.update(delta)
                    if (pauseTimer.isFinished()) {
                        GameLogger.debug(TAG, "PhaseTransitionHandler: PAUSE -> DROP_DOWN")
                        body.setCenter(spawnCenter.x, room.getMaxY() + body.getHeight())
                        state = WilyPhaseTransState.DROP_DOWN
                        // Go ahead and start the next phase before dropping down so that the animations
                        // are updated to use the next phase
                        startNextPhase()
                    }
                }

                WilyPhaseTransState.DROP_DOWN -> {
                    body.physics.velocity.set(0f, -PhaseTransitionConstVals.DROP_DOWN_SPEED * ConstVals.PPM)
                    if (body.getCenter().y <= spawnCenter.y) {
                        GameLogger.debug(TAG, "PhaseTransitionHandler: DROP_DOWN complete, starting next phase")
                        body.setCenter(spawnCenter)
                        body.physics.velocity.setZero()
                        state = WilyPhaseTransState.END
                    }
                }

                WilyPhaseTransState.END -> {
                    // TODO: Add timer here so that end of transition can be filled with an animation

                    active = false

                    game.eventsMan.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
                    megaman.body.physics.velocity.x = 0f
                    megaman.canBeDamaged = true

                    setHealthToMax()
                }
            }
        }

        private fun spawnSkullHead() {
            GameLogger.debug(TAG, "spawnSkullHead()")
            val skullHead = MegaEntityFactory.fetch(WilySkullHead::class)!!
            skullHead.spawn(
                props(
                    ConstKeys.TARGET pairTo skullHeadTarget,
                    ConstKeys.POSITION pairTo body.getCenter(),
                )
            )
        }

        override fun reset() {
            GameLogger.debug(TAG, "PhaseTransitionHandler: reset()")
            state = WilyPhaseTransState.INIT
            active = false
            explodeTimer.reset()
            pauseTimer.reset()
        }
    }

    private inner class Phase2Handler {

        fun buildStateMachine() = EnumStateMachineBuilder
            .create<WilyPhase2State>()
            .initialState(WilyPhase2State.HOVER)
            .build()
    }

    private fun buildStateMachines() {
        stateMachines.put(WilyFinalBossPhase.PHASE_1, phase1Handler.buildStateMachine())
        stateMachines.put(WilyFinalBossPhase.PHASE_2, phase2Handler.buildStateMachine())
    }

    override fun getTag() = TAG
}
