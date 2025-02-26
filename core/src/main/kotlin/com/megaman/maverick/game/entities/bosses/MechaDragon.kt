package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.SmoothOscillationTimer
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.SpitFireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class MechaDragon(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "MechaDragon"

        private const val LEFT_HOVER_TARGET = "${ConstKeys.LEFT}_${ConstKeys.HOVER}_${ConstKeys.TARGET}"
        private const val RIGHT_HOVER_TARGET = "${ConstKeys.RIGHT}_${ConstKeys.HOVER}_${ConstKeys.TARGET}"

        private const val LEFT_CHARGE_TARGET = "${ConstKeys.LEFT}_${ConstKeys.CHARGE}_${ConstKeys.TARGET}"
        private const val RIGHT_CHARGE_TARGET = "${ConstKeys.RIGHT}_${ConstKeys.CHARGE}_${ConstKeys.TARGET}"

        private const val BLOCK_ON_SPAWN = "${ConstKeys.BLOCK}_${ConstKeys.ON}_${ConstKeys.SPAWN}"

        private const val INIT_DUR = 0.5f

        private const val HOVER_IN_PLACE_DUR = 5f
        private const val HOVER_SWAY_DUR = 2f
        private const val HOVER_SWAY_X = 1f

        private const val FLY_TO_TARGET_SPEED = 6f

        private const val TURNING_AROUND_DUR = 0.5f

        private const val CHARGE_SPEED = 8f
        private const val CHARGE_BEGIN_MAX_DELAY = 2f
        private const val CHARGE_BEGIN_MIN_DELAY = 1f
        private const val CHARGE_END_DELAY = 0.1f
        private const val CHARGE_CHANCE_DELTA = 35

        private const val FIRE_DUR = 2.5f
        private const val FIRE_TIME = 1.6f
        private const val FIRE_MIN_DELAY = 0.5f
        private const val FIRE_MAX_DELAY = 1f
        private const val FIRE_SPEED = 10f
        private const val FIRE_ANGLE_DELTA = 75f

        private const val SUBSEQUENT_FIRE_ANGLE_DIFF = 10f

        private val animDefs = orderedMapOf(
            "init" pairTo AnimationDef(),
            "fly" pairTo AnimationDef(3, 2, 0.1f, true),
            "fire1" pairTo AnimationDef(4, 4, 0.1f, false),
            "fire2" pairTo AnimationDef(3, 2, 0.1f, true),
            "turning" pairTo AnimationDef(5, 1, 0.1f, false),
            "defeated" pairTo AnimationDef(2, 2, 0.1f, true),
            "charging" pairTo AnimationDef(3, 2, 0.05f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()

        private fun extractVector2(value: Any?) = (value as RectangleMapObject).rectangle.getCenter(false)
    }

    private enum class MechaDragonState { INIT, HOVER_IN_PLACE, FLY_TO_TARGET, CHARGE_TO_OTHER_SIDE, TURNING_AROUND }

    private enum class RoomSide { LEFT, RIGHT }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<MechaDragonState>
    private val currentState: MechaDragonState
        get() = stateMachine.getCurrent()
    private val stateTimers = OrderedMap<MechaDragonState, Timer>()

    private val hoverScalar = SmoothOscillationTimer(duration = HOVER_SWAY_DUR, start = -1f, end = 1f)

    private val leftSideHoverTargets = Array<Vector2>()
    private val rightSideHoverTargets = Array<Vector2>()
    private val reusableHoverTargetsSet = OrderedSet<Vector2>()

    private val leftSideChargeTargets = Array<Vector2>()
    private val rightSideChargeTargets = Array<Vector2>()

    private val min = Vector2()
    private val max = Vector2()

    private val roomCenter = Vector2()

    private val currentTarget = Vector2()

    private lateinit var chargeState: ProcessState
    private var chargeChance = 0
    private val beginChargeDelay = Timer()
    private val endChargeDelay = Timer(CHARGE_END_DELAY)

    private val fireDelay = Timer()
    private val fireTimer = Timer(FIRE_DUR)
        .addRunnable(TimeMarkedRunnable(FIRE_TIME) { spitFireballs() }.setToRunOnlyWhenJustPassedTime(true))
    private val firing: Boolean
        get() = !fireTimer.isFinished()

    private val blockIds = OrderedSet<Int>()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }

        if (stateTimers.isEmpty) stateTimers.putAll(
            MechaDragonState.INIT pairTo Timer(INIT_DUR),
            MechaDragonState.HOVER_IN_PLACE pairTo Timer(HOVER_IN_PLACE_DUR),
            MechaDragonState.TURNING_AROUND pairTo Timer(TURNING_AROUND_DUR)
        )

        super.init()

        addComponent(defineAnimationsComponent())

        stateMachine = buildStateMachine()

        damageOverrides.put(ChargedShotExplosion::class, dmgNeg(1))
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.MINI, true)
        spawnProps.put(ConstKeys.ORB, false)

        super.onSpawn(spawnProps)

        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setBottomCenterToPoint(spawn)

        val firstTarget = extractVector2(spawnProps.get("${ConstKeys.FIRST}_${ConstKeys.TARGET}"))
        currentTarget.set(firstTarget)

        facing = if (firstTarget.x < spawn.x) Facing.LEFT else Facing.RIGHT

        val roomCenter = extractVector2(spawnProps.get("${ConstKeys.ROOM}_${ConstKeys.CENTER}"))
        this.roomCenter.set(roomCenter)

        val min = extractVector2(spawnProps.get(ConstKeys.MIN))
        this.min.set(min)

        val max = extractVector2(spawnProps.get(ConstKeys.MAX))
        this.max.set(max)

        spawnProps.forEach { key, value ->
            key.toString().let {
                when {
                    it.contains(LEFT_HOVER_TARGET) -> leftSideHoverTargets.add(extractVector2(value))
                    it.contains(RIGHT_HOVER_TARGET) -> rightSideHoverTargets.add(extractVector2(value))
                    it.contains(LEFT_CHARGE_TARGET) -> leftSideChargeTargets.add(extractVector2(value))
                    it.contains(RIGHT_CHARGE_TARGET) -> rightSideChargeTargets.add(extractVector2((value)))
                    it.contains(ConstKeys.BLOCK) -> {
                        val id = (value as RectangleMapObject).properties.get(ConstKeys.ID, Int::class.java)
                        blockIds.add(id)
                    }
                }
            }
        }

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        chargeState = ProcessState.BEGIN
        chargeChance = 0
        endChargeDelay.reset()

        fireDelay.resetDuration(FIRE_MAX_DELAY)
        fireTimer.setToEnd()

        hoverScalar.reset()

        val blockOnSpawn = spawnProps.get(BLOCK_ON_SPAWN, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        val block = MegaEntityFactory.fetch(Block::class)!!
        block.spawn(props(ConstKeys.BOUNDS pairTo blockOnSpawn))

        GameLogger.debug(
            TAG, "onSpawn():\n" +
                "spawnProps=$spawnProps,\n" +
                "leftSideHoverTargets=$leftSideHoverTargets,\n" +
                "rightSideHoverTargets=$rightSideHoverTargets,\n" +
                "leftSideChargeSpots=$leftSideChargeTargets,\n" +
                "rightSideChargeSpots=$rightSideChargeTargets,\n" +
                "blockIds=$blockIds"
        )
    }

    override fun isReady(delta: Float) = stateTimers[MechaDragonState.INIT].isFinished()

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val oldHealth = getCurrentHealth()
        val damageTaken = super.takeDamageFrom(damager)
        GameLogger.debug(TAG, "takeDamageFrom(): health=${getCurrentHealth()}, oldHealth=$oldHealth, damager=$damager")
        return damageTaken
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        leftSideHoverTargets.clear()
        rightSideHoverTargets.clear()

        leftSideChargeTargets.clear()
        rightSideChargeTargets.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            when (currentState) {
                MechaDragonState.INIT -> {
                    if (!isTargetReached(currentTarget)) {
                        flyToTarget(currentTarget, FLY_TO_TARGET_SPEED * ConstVals.PPM)
                        return@add
                    }

                    body.physics.velocity.setZero()

                    val stateTimer = stateTimers[MechaDragonState.INIT]
                    if (stateTimer.isAtBeginning()) requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
                    stateTimer.update(delta)
                    if (stateTimer.isFinished()) stateMachine.next()
                }

                MechaDragonState.HOVER_IN_PLACE -> {
                    val roomSide = getRoomSideOf(body.getCenter())
                    updateFacingByRoomSide(roomSide)

                    hoverScalar.update(delta)
                    val velX = HOVER_SWAY_X * ConstVals.PPM * hoverScalar.getValue()
                    body.physics.velocity.set(velX, 0f)

                    val stateTimer = stateTimers[MechaDragonState.HOVER_IN_PLACE]
                    stateTimer.update(delta)

                    fireTimer.update(delta)
                    if (fireTimer.isFinished()) fireDelay.update(delta)
                    if (!stateTimer.isFinished() && fireDelay.isFinished()) {
                        fireTimer.reset()

                        val fireDelayDur =
                            UtilMethods.interpolate(FIRE_MIN_DELAY, FIRE_MAX_DELAY, 1f - getHealthRatio())
                        fireDelay.resetDuration(fireDelayDur)
                    }

                    if (stateTimer.isFinished() && fireTimer.isFinished()) {
                        stateMachine.next()
                        stateTimer.reset()
                    }
                }

                MechaDragonState.FLY_TO_TARGET -> {
                    val roomSide = getRoomSideOf(body.getCenter())
                    updateFacingByRoomSide(roomSide)

                    if (!isTargetReached(currentTarget)) {
                        flyToTarget(currentTarget, FLY_TO_TARGET_SPEED * ConstVals.PPM)
                        return@add
                    }

                    stateMachine.next()
                }

                MechaDragonState.CHARGE_TO_OTHER_SIDE -> when (chargeState) {
                    ProcessState.BEGIN -> {
                        val target = body.getCenter().setY(currentTarget.y)
                        flyToTarget(target, FLY_TO_TARGET_SPEED * ConstVals.PPM)

                        if (isTargetReached(target)) {
                            chargeState = ProcessState.CONTINUE

                            body.physics.velocity.setZero()

                            val delay = UtilMethods.interpolate(
                                CHARGE_BEGIN_MIN_DELAY,
                                CHARGE_BEGIN_MAX_DELAY,
                                1f - getHealthRatio()
                            )
                            beginChargeDelay.resetDuration(delay)
                        }
                    }

                    ProcessState.CONTINUE -> {
                        beginChargeDelay.update(delta)
                        if (!beginChargeDelay.isFinished()) {
                            body.physics.velocity.setZero()
                            return@add
                        }

                        flyToTarget(currentTarget, CHARGE_SPEED * ConstVals.PPM)

                        if (isTargetReached(currentTarget)) {
                            chargeState = ProcessState.END
                            body.physics.velocity.setZero()
                        }
                    }

                    ProcessState.END -> {
                        endChargeDelay.update(delta)
                        if (endChargeDelay.isFinished()) {
                            stateMachine.next()
                            endChargeDelay.reset()
                        }
                    }
                }

                MechaDragonState.TURNING_AROUND -> {
                    body.physics.velocity.setZero()

                    val stateTimer = stateTimers[MechaDragonState.TURNING_AROUND]
                    stateTimer.update(delta)
                    if (stateTimer.isFinished()) {
                        stateMachine.next()
                        stateTimer.reset()
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, 5f * ConstVals.PPM)
        body.drawingColor = Color.DARK_GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headDamagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(1.5f * ConstVals.PPM, 2.5f * ConstVals.PPM)
        )
        headDamagerFixture.offsetFromBodyAttachment.y = 1.25f * ConstVals.PPM
        body.addFixture(headDamagerFixture)
        debugShapes.add { headDamagerFixture }

        val headDamageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(1.5f * ConstVals.PPM, 2.5f * ConstVals.PPM)
        )
        headDamageableFixture.offsetFromBodyAttachment.y = 1.25f * ConstVals.PPM
        body.addFixture(headDamageableFixture)
        // debugShapes.add { headDamageableFixture }

        val neckDamagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(ConstVals.PPM.toFloat()))
        neckDamagerFixture.offsetFromBodyAttachment.y = 1.25f * ConstVals.PPM
        body.addFixture(neckDamagerFixture)
        debugShapes.add { neckDamagerFixture }

        val neckDamageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        neckDamageableFixture.offsetFromBodyAttachment.y = 1.25f * ConstVals.PPM
        body.addFixture(neckDamageableFixture)
        // debugShapes.add { neckDamageableFixture }

        val bodyDamagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2.5f * ConstVals.PPM, 6f * ConstVals.PPM))
        body.addFixture(bodyDamagerFixture)
        debugShapes.add { bodyDamagerFixture }

        val bodyDamageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(2.5f * ConstVals.PPM, 6f * ConstVals.PPM))
        body.addFixture(bodyDamageableFixture)
        // debugShapes.add { bodyDamageableFixture }

        val tailDamagerFixture1 = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM, 2f * ConstVals.PPM)
        )
        tailDamagerFixture1.offsetFromBodyAttachment.y = -1.25f * ConstVals.PPM
        body.addFixture(tailDamagerFixture1)
        debugShapes.add { tailDamagerFixture1 }

        val tailDamageableFixture1 = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.75f * ConstVals.PPM, 2f * ConstVals.PPM)
        )
        tailDamageableFixture1.offsetFromBodyAttachment.y = -1.25f * ConstVals.PPM
        body.addFixture(tailDamageableFixture1)
        // debugShapes.add { tailDamageableFixture1 }

        val tailDamagerFixture2 = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.5f * ConstVals.PPM)
        )

        tailDamagerFixture2.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM
        body.addFixture(tailDamagerFixture2)
        debugShapes.add { tailDamagerFixture2 }

        val tailDamageableFixture2 = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.5f * ConstVals.PPM)
        )
        tailDamageableFixture2.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM
        body.addFixture(tailDamageableFixture2)
        // debugShapes.add { tailDamageableFixture2 }

        gdxArrayOf(
            tailDamageableFixture1,
            bodyDamageableFixture,
            neckDamageableFixture,
            headDamageableFixture
        ).forEach { t ->
            val bodyFixture1 = Fixture(
                body = body,
                type = FixtureType.BODY,
                rawShape = t.rawShape.copy(),
                offsetFromBodyAttachment = t.offsetFromBodyAttachment.cpy()
            )

            val width = bodyFixture1.getShape().getBoundingRectangle().getWidth() * 0.9f
            val height = bodyFixture1.getShape().getBoundingRectangle().getHeight() * 0.9f
            bodyFixture1.rawShape.setWithProps(props(ConstKeys.WIDTH pairTo width, ConstKeys.HEIGHT pairTo height))

            body.addFixture(bodyFixture1)
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val headOffsetX = 2.5f * ConstVals.PPM * facing.value
            headDamagerFixture.offsetFromBodyAttachment.x = headOffsetX
            headDamageableFixture.offsetFromBodyAttachment.x = headOffsetX

            val neckOffsetX = 0.25f * ConstVals.PPM * facing.value
            neckDamagerFixture.offsetFromBodyAttachment.x = neckOffsetX
            neckDamageableFixture.offsetFromBodyAttachment.x = neckOffsetX

            val tail1OffsetX = 2f * ConstVals.PPM * -facing.value
            tailDamagerFixture1.offsetFromBodyAttachment.x = tail1OffsetX
            tailDamageableFixture1.offsetFromBodyAttachment.x = tail1OffsetX

            val tail2OffsetX = 3f * ConstVals.PPM * -facing.value
            tailDamagerFixture2.offsetFromBodyAttachment.x = tail2OffsetX
            tailDamageableFixture2.offsetFromBodyAttachment.x = tail2OffsetX
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(8f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setAlpha(if (defeated) 1f - defeatTimer.getRatio() else 1f)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    if (defeated) "defeated" else when (currentState) {
                        MechaDragonState.INIT -> if (isTargetReached(currentTarget)) "init" else "fly"
                        MechaDragonState.CHARGE_TO_OTHER_SIDE -> "charging"
                        MechaDragonState.TURNING_AROUND -> "turning"
                        else -> when {
                            firing -> if (fireTimer.time <= FIRE_TIME) "fire1" else "fire2"
                            else -> "fly"
                        }
                    }
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, cols, durations, loop) = entry.value
                        try {
                            animations.put(key, Animation(regions[key], rows, cols, durations, loop))
                        } catch (e: Exception) {
                            throw IllegalStateException("Failed to create animation for key=$key", e)
                        }
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder
        .create<MechaDragonState>()
        .initialState(MechaDragonState.INIT)
        .setOnChangeState(this::onChangeState)
        // init
        .transition(MechaDragonState.INIT, MechaDragonState.TURNING_AROUND) { shouldTurnAround() }
        .transition(MechaDragonState.INIT, MechaDragonState.HOVER_IN_PLACE) { true }
        // hover in place
        .transition(MechaDragonState.HOVER_IN_PLACE, MechaDragonState.CHARGE_TO_OTHER_SIDE) {
            shouldChargeToOtherSide()
        }
        .transition(MechaDragonState.HOVER_IN_PLACE, MechaDragonState.FLY_TO_TARGET) { true }
        // charge to other side
        .transition(MechaDragonState.CHARGE_TO_OTHER_SIDE, MechaDragonState.TURNING_AROUND) { true }
        // turning around
        .transition(MechaDragonState.TURNING_AROUND, MechaDragonState.HOVER_IN_PLACE) { true }
        // fly to target
        .transition(MechaDragonState.FLY_TO_TARGET, MechaDragonState.HOVER_IN_PLACE) { true }
        // build
        .build()

    private fun onChangeState(current: MechaDragonState, previous: MechaDragonState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        if (previous == MechaDragonState.HOVER_IN_PLACE && current != MechaDragonState.CHARGE_TO_OTHER_SIDE) {
            val oldChance = chargeChance
            chargeChance += CHARGE_CHANCE_DELTA
            if (chargeChance > 100) chargeChance = 100

            GameLogger.debug(TAG, "onChangeState(): chargeChance=$chargeChance, oldChargeChange=$oldChance")
        }

        if (current == MechaDragonState.CHARGE_TO_OTHER_SIDE) {
            val roomSide = getRoomSideOf(body.getCenter())
            val target = when (roomSide) {
                RoomSide.LEFT -> rightSideChargeTargets.superRandom()
                RoomSide.RIGHT -> leftSideChargeTargets.superRandom()
            }
            currentTarget.set(target)

            chargeState = ProcessState.BEGIN
            chargeChance = 0

            GameLogger.debug(
                TAG,
                "onChangeState(): target=$target, roomSide=$roomSide, body.getCenter()=${body.getCenter()}"
            )
        } else if (current == MechaDragonState.FLY_TO_TARGET) {
            val roomSide = getRoomSideOf(body.getCenter())
            val candidateTargets = when (roomSide) {
                RoomSide.LEFT -> leftSideHoverTargets
                RoomSide.RIGHT -> rightSideHoverTargets
            }
            reusableHoverTargetsSet.addAll(candidateTargets)
            reusableHoverTargetsSet.remove(currentTarget)

            val target = reusableHoverTargetsSet.superRandom()
            currentTarget.set(target)

            reusableHoverTargetsSet.clear()

            GameLogger.debug(
                TAG,
                "onChangeState(): target=$target, roomSide=$roomSide, body.getCenter()=${body.getCenter()}"
            )
        } else if (current == MechaDragonState.HOVER_IN_PLACE) {
            fireDelay.reset()
            fireTimer.setToEnd()
        }
    }

    private fun shouldChargeToOtherSide() = getRandom(0, 100) <= chargeChance

    private fun shouldTurnAround() = getRoomSideOf(body.getCenter()).name == facing.name

    private fun flyToTarget(target: Vector2, speed: Float) {
        val velocity = target.cpy().sub(body.getCenter()).nor().scl(speed)
        body.physics.velocity.set(velocity)
    }

    private fun isTargetReached(target: Vector2) = body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM)

    private fun getRoomSideOf(target: Vector2) = if (target.x < roomCenter.x) RoomSide.LEFT else RoomSide.RIGHT

    private fun spitFireballs() {
        val fireballsToLaunch = when {
            getHealthRatio() > 0.5f -> 1
            getHealthRatio() > 0.25f -> 2
            else -> 3
        }

        (0 until fireballsToLaunch).forEach { i ->
            val spawn = body.getCenter().add(3f * ConstVals.PPM * facing.value, 0f)

            val maxAngle: Float
            val minAngle: Float
            when (facing) {
                Facing.LEFT -> {
                    maxAngle = 90f + FIRE_ANGLE_DELTA
                    minAngle = 90f - FIRE_ANGLE_DELTA
                }

                Facing.RIGHT -> {
                    maxAngle = 270f + FIRE_ANGLE_DELTA
                    minAngle = 270f - FIRE_ANGLE_DELTA
                }
            }

            var megamanToSpawnAngle = megaman.body.getCenter().sub(spawn).nor().angleDeg() - 90f
            if (megamanToSpawnAngle < 0f) megamanToSpawnAngle += 360f

            var angle = megamanToSpawnAngle
            when {
                angle > maxAngle -> angle = maxAngle
                angle < minAngle -> angle = minAngle
            }

            when (i) {
                1 -> angle += SUBSEQUENT_FIRE_ANGLE_DIFF
                2 -> angle -= SUBSEQUENT_FIRE_ANGLE_DIFF
            }

            val trajectory = GameObjectPools.fetch(Vector2::class)
                .set(0f, FIRE_SPEED * ConstVals.PPM)
                .rotateDeg(angle)

            GameLogger.debug(
                TAG,
                "spitFireball(): " +
                    "spawn=$spawn, " +
                    "megamanToSpawnAngle=${megamanToSpawnAngle}, " +
                    "angle=$angle, " +
                    "trajectory=$trajectory"
            )

            val fireball = MegaEntityFactory.fetch(SpitFireball::class)!!
            fireball.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.BLOCKS pairTo blockIds,
                    ConstKeys.TRAJECTORY pairTo trajectory
                )
            )

            requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
        }
    }

    private fun updateFacingByRoomSide(roomSide: RoomSide) {
        val oldFacing = facing
        facing = if (roomSide == RoomSide.LEFT) Facing.RIGHT else Facing.LEFT

        if (oldFacing != facing) GameLogger.debug(
            TAG,
            "updateFacingByRoomSide(): roomSide=$roomSide, facing=$facing, oldFacing=$oldFacing"
        )
    }

    override fun getTag() = TAG
}
