package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
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
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.MockingByteNest
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.MockingByteEgg
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.midPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class MockingByte(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable, IFreezableEntity {

    companion object {
        const val TAG = "MockingByte"

        private const val MAX_SPAWNED = 2

        private const val AWAKEN_DUR = 0.4f
        private const val AWAKEN_RADIUS = 8f

        private const val FALL_ASLEEP_DUR = 1f

        private const val LEAVE_NEST_DUR = 0.2f
        private const val ENTER_NEST_DUR = 0.2f

        private const val RISE_DUR = 1f
        private const val MIN_RISE_SPEED = 1f
        private const val MAX_RISE_SPEED = 8f

        private const val HOVER_DELAY = 0.5f
        private const val HOVER_MIN_SPEED = 4f
        private const val HOVER_MAX_SPEED = 12f
        private const val HOVER_DROP_EGG_INDEX = 1
        private const val HOVER_DROP_EGG_TIME = 0.1f
        private const val HOVER_TIMES_BEFORE_DIVE = 3

        private const val DROP_EGG_COOLDOWN = 0.5f

        private const val DIVE_DELAY = 0.5f
        private const val DIVE_SPEED = 12f
        private const val DIVE_OUT_OF_BOUNDS_DUR = 1f

        private val animDefs = orderedMapOf(
            "sleep" pairTo AnimationDef(),
            "awaken" pairTo AnimationDef(2, 2, 0.1f, false),
            "fall_asleep" pairTo AnimationDef(3, 1, 0.1f, false),
            "leave_nest" pairTo AnimationDef(2, 1, 0.1f, false),
            "enter_nest" pairTo AnimationDef(2, 1, 0.1f, false),
            "return_to_nest" pairTo AnimationDef(2, 1, 0.1f, true),
            "rise" pairTo AnimationDef(2, 1, 0.1f, true),
            "hover" pairTo AnimationDef(2, 1, 0.1f, true),
            "dive1" pairTo AnimationDef(),
            "dive2" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class MockingByteState { SLEEP, AWAKEN, LEAVE_NEST, RETURN_TO_NEST, ENTER_NEST, RISE, HOVER, DIVE, FALL_ASLEEP }

    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = !frozenTimer.isFinished()
        set(value) {
            if (value) frozenTimer.reset() else frozenTimer.setToEnd()
        }
    private val frozenTimer = Timer(1f)

    private lateinit var stateMachine: StateMachine<MockingByteState>
    private val stateTimers = orderedMapOf(
        MockingByteState.RISE pairTo Timer(RISE_DUR),
        MockingByteState.AWAKEN pairTo Timer(AWAKEN_DUR),
        MockingByteState.FALL_ASLEEP pairTo Timer(FALL_ASLEEP_DUR),
        MockingByteState.LEAVE_NEST pairTo Timer(LEAVE_NEST_DUR),
        MockingByteState.ENTER_NEST pairTo Timer(ENTER_NEST_DUR)
    )
    private val currentState: MockingByteState
        get() = stateMachine.getCurrentElement()

    private val awakenArea = GameCircle().setRadius(AWAKEN_RADIUS * ConstVals.PPM)

    private val hoverDelay = Timer(HOVER_DELAY)
        .addRunnable(TimeMarkedRunnable(HOVER_DROP_EGG_TIME) {
            if (hoverTimes == HOVER_DROP_EGG_INDEX) dropEgg()
        })
    private val dropEggCooldown = Timer(DROP_EGG_COOLDOWN)

    private val diveDelay = Timer(DIVE_DELAY)
    private val diveOutOfBoundsTimer = Timer(DIVE_OUT_OF_BOUNDS_DUR)

    private val startPosition = Vector2()
    private val targetPosition = Vector2()

    private var nestId = -1
    private val reusableNestArray = Array<MockingByteNest>()
    private val reusableNestSet = OrderedSet<MockingByteNest>()

    private val reusableVec2Array = Array<Vector2>()

    private var hoverTimes = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        stateMachine = buildStateMachine()
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties) = super.canSpawn(spawnProps) &&
        MegaGameEntities.getOfTag(TAG).size < MAX_SPAWNED

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
            .getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        hoverDelay.reset()
        dropEggCooldown.setToEnd()

        diveDelay.reset()
        diveOutOfBoundsTimer.reset()

        nestId = spawnProps
            .get(ConstKeys.NEST, RectangleMapObject::class)!!.properties
            .get(ConstKeys.ID, Int::class.java)
        val nest = getNest()
        if (nest != null) nest.owner = this

        facing = Facing.LEFT

        hoverTimes = 0

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        val nest = getNest()
        if (nest?.owner == this) {
            nest.owner = null
            nest.hidden = false
        }
    }

    override fun canBeDamagedBy(damager: IDamager) =
        damager is Axe || super.canBeDamagedBy(damager)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && damager is IFreezerEntity) frozen = true
        return damaged
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add update@{ delta ->
            frozenTimer.update(delta)
            if (frozen) {
                body.physics.velocity.set(0f, -10f * ConstVals.PPM)
                return@update
            }
            if (frozenTimer.isJustFinished()) IceShard.spawn5(body.getCenter())

            val stateTimer = stateTimers[currentState]
            if (stateTimer != null) {
                stateTimer.update(delta)
                if (stateTimer.isFinished()) {
                    GameLogger.debug(TAG, "onSpawn(): state timer finished")
                    stateMachine.next()
                }
            }

            var nest = getNest()
            if (nest != null) {
                when (currentState) {
                    MockingByteState.SLEEP,
                    MockingByteState.AWAKEN,
                    MockingByteState.ENTER_NEST,
                    MockingByteState.LEAVE_NEST,
                    MockingByteState.FALL_ASLEEP -> {
                        nest.owner = this
                        nest.hidden = true
                    }
                    else -> nest.hidden = false
                }
            }

            when (currentState) {
                MockingByteState.SLEEP,
                MockingByteState.AWAKEN,
                MockingByteState.FALL_ASLEEP,
                MockingByteState.ENTER_NEST,
                MockingByteState.LEAVE_NEST -> {
                    facing = Facing.LEFT

                    body.physics.velocity.setZero()

                    if (nest == null) {
                        GameLogger.error(TAG, "Nest should not be null when state=$currentState")
                        stateMachine.next()
                        return@update
                    }

                    body.positionOnPoint(nest.position, Position.BOTTOM_CENTER)

                    if (currentState == MockingByteState.SLEEP) {
                        awakenArea.setCenter(body.getCenter())

                        if (megaman.body.getBounds().overlaps(awakenArea)) {
                            GameLogger.debug(TAG, "update(): megaman entered awaken area, wake up")
                            stateMachine.next()
                        }
                    }
                }
                MockingByteState.RISE, MockingByteState.HOVER, MockingByteState.RETURN_TO_NEST -> {
                    FacingUtils.setFacingOf(this)

                    if (currentState == MockingByteState.HOVER) {
                        dropEggCooldown.update(delta)
                        if (!dropEggCooldown.isFinished()) {
                            body.physics.velocity.setZero()
                            return@update
                        }

                        hoverDelay.update(delta)
                        if (!hoverDelay.isFinished()) {
                            body.physics.velocity.setZero()
                            return@update
                        }

                        if (hoverDelay.isJustFinished()) {
                            startPosition.set(body.getCenter())
                            targetPosition.set(megaman.body.getCenter())

                            GameLogger.debug(
                                TAG, "update(): hover delay just finished: " +
                                    "startPos=$startPosition, targetPos=$targetPosition"
                            )
                        }
                    }

                    val minSpeed: Float
                    val maxSpeed: Float
                    if (currentState == MockingByteState.RISE) {
                        minSpeed = MIN_RISE_SPEED
                        maxSpeed = MAX_RISE_SPEED
                    } else {
                        minSpeed = HOVER_MIN_SPEED
                        maxSpeed = HOVER_MAX_SPEED
                    }

                    val trajectory = GameObjectPools.fetch(Vector2::class)
                        .set(targetPosition)
                        .sub(startPosition)
                        .nor()
                        .scl(getSmoothSpeedByMidpointDist(minSpeed, maxSpeed))
                        .scl(ConstVals.PPM.toFloat())
                    body.physics.velocity.set(trajectory)

                    if (body.getBounds().getCenter().epsilonEquals(targetPosition, 0.25f * ConstVals.PPM)) {
                        GameLogger.debug(
                            TAG, "update(): reached target position: " +
                                "actualPos=${body.getCenter()}, targetPos=$targetPosition"
                        )

                        stateMachine.next()
                    }
                }
                MockingByteState.DIVE -> {
                    diveDelay.update(delta)
                    if (!diveDelay.isFinished()) {
                        body.physics.velocity.setZero()
                        FacingUtils.setFacingOf(this)
                        return@update
                    }
                    if (diveDelay.isJustFinished()) {
                        startPosition.set(body.getCenter())
                        targetPosition.set(megaman.body.getCenter())
                        GameLogger.debug(
                            TAG, "update(): dive delay just finished: " +
                                "startPos=$startPosition, targetPos=$targetPosition"
                        )
                    }

                    val trajectory = GameObjectPools.fetch(Vector2::class)
                        .set(targetPosition)
                        .sub(startPosition)
                        .nor()
                        .scl(DIVE_SPEED * ConstVals.PPM)
                    body.physics.velocity.set(trajectory)

                    facing = if (body.physics.velocity.x > 0f) Facing.RIGHT else Facing.LEFT

                    val camBounds = game.getGameCamera().getRotatedBounds()
                    val bodyBounds = body.getBounds()

                    if (!camBounds.overlaps(bodyBounds)) {
                        if (camBounds.overlaps(bodyBounds.translate(body.getPositionDelta()))) GameLogger.debug(
                            TAG,
                            "update(): just left game camera bounds: " +
                                "body.getBounds=${body.getBounds()}, " +
                                "camera.getBounds=${game.getGameCamera().getRotatedBounds()}"
                        )

                        diveOutOfBoundsTimer.update(delta)
                        if (diveOutOfBoundsTimer.isFinished()) {
                            GameLogger.debug(
                                TAG, "update(): max dive time out of bounds: " +
                                    "actualPos=${body.getCenter()}, targetPos=$targetPosition"
                            )

                            val oldNest = getNest()

                            val foundNewNest = findNewNest()
                            if (foundNewNest) {
                                if (oldNest?.owner == this) oldNest.owner = null

                                val newNest = getNest()!!
                                newNest.owner = this
                            }

                            GameLogger.debug(
                                TAG,
                                "update(): lost nest; found new nest: $foundNewNest; nestId=$nestId"
                            )

                            stateMachine.next()
                        }
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
                .also { sprite -> sprite.setSize(1.5f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            if (!currentState.equalsAny(
                    MockingByteState.SLEEP,
                    MockingByteState.AWAKEN,
                    MockingByteState.ENTER_NEST,
                    MockingByteState.LEAVE_NEST,
                    MockingByteState.FALL_ASLEEP
                )
            ) sprite.translate(0f, -0.25f * ConstVals.PPM)

            sprite.setOriginCenter()
            sprite.rotation = when {
                currentState == MockingByteState.DIVE && diveDelay.isFinished() ->
                    body.physics.velocity.angleDeg() - 90f
                else -> 0f
            }

            sprite.setFlip(isFacing(Facing.RIGHT), false)

            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        frozen -> "frozen"
                        currentState == MockingByteState.DIVE -> if (!diveDelay.isFinished()) "dive1" else "dive2"
                        else -> currentState.name.lowercase()
                    }
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<MockingByteState>()
        .initialState(MockingByteState.SLEEP)
        .onChangeState(this::onChangeState)
        .triggerChangeWhenSameElement(true)
        // sleep
        .transition(MockingByteState.SLEEP, MockingByteState.AWAKEN) { true }
        // awaken
        .transition(MockingByteState.AWAKEN, MockingByteState.LEAVE_NEST) { true }
        // leave nest
        .transition(MockingByteState.LEAVE_NEST, MockingByteState.RISE) { true }
        // rise
        .transition(MockingByteState.RISE, MockingByteState.HOVER) { true }
        // hover
        .transition(MockingByteState.HOVER, MockingByteState.DIVE) { shouldDive() }
        .transition(MockingByteState.HOVER, MockingByteState.HOVER) { true }
        // dive
        .transition(MockingByteState.DIVE, MockingByteState.RETURN_TO_NEST) { hasNest() }
        .transition(MockingByteState.DIVE, MockingByteState.HOVER) { true }
        // return to nest
        .transition(MockingByteState.RETURN_TO_NEST, MockingByteState.FALL_ASLEEP) { true }
        // fall asleep
        .transition(MockingByteState.FALL_ASLEEP, MockingByteState.SLEEP) { true }
        // build
        .build()

    private fun onChangeState(current: MockingByteState, previous: MockingByteState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        stateTimers[current]?.reset()

        when (current) {
            MockingByteState.RISE -> {
                body.translate(0f, 0.5f * ConstVals.PPM)
                startPosition.set(body.getCenter())

                val riseAmount = getNest()?.riseAmount ?: 4f
                targetPosition.set(startPosition).add(0f, riseAmount * ConstVals.PPM)

                GameLogger.debug(
                    TAG, "onChangeState(): RISE: " +
                        "startPos=$startPosition, targetPos=$targetPosition, megamanPos=${megaman.body.getCenter()}"
                )
            }
            MockingByteState.HOVER -> {
                hoverTimes++
                hoverDelay.reset()

                GameLogger.debug(
                    TAG, "onChangeState(): HOVER: " +
                        "startPos=$startPosition, targetPos=$targetPosition, megamanPos=${megaman.body.getCenter()}"
                )
            }
            MockingByteState.DIVE -> {
                hoverTimes = 0

                diveDelay.reset()
                diveOutOfBoundsTimer.reset()

                body.physics.velocity.setZero()
            }
            MockingByteState.RETURN_TO_NEST -> {
                val nest = getNest()!!
                nest.owner = this

                val returnPositions = reusableVec2Array
                returnPositions.addAll(nest.returnPositions)
                returnPositions.sort { a, b ->
                    (a.dst2(megaman.body.getCenter()) - b.dst2(megaman.body.getCenter())).toInt()
                }

                startPosition.set(returnPositions[0])
                body.setCenter(startPosition)

                targetPosition.set(nest.position)

                body.physics.velocity.setZero()

                GameLogger.debug(
                    TAG,
                    "onChangeState(): startPos=$startPosition, targetPos=$targetPosition, nest=${nest.id}"
                )
            }
            else -> {}
        }

        if (previous == MockingByteState.LEAVE_NEST) {
            val nest = getNest()
            if (nest?.owner == this) {
                nest.owner = null
                GameLogger.debug(TAG, "onChangeState(): set nest owner to null")
            }
        }
    }

    private fun getNest() = MegaGameEntities.getOfId(nestId).firstOrNull() as MockingByteNest?

    private fun hasNest() = getNest() != null

    private fun isNestAttainable(nest: MockingByteNest) = nest.owner == null || nest.owner == this

    private fun findNewNest(): Boolean {
        reusableNestSet.clear()
        reusableNestArray.clear()

        val nests = MegaGameEntities.getOfTag<MockingByteNest>(MockingByteNest.TAG, reusableNestSet)
        if (nests.isEmpty) return false

        val sortedNests = reusableNestArray

        for (nest in nests) if (isNestAttainable(nest)) sortedNests.add(nest)
        if (sortedNests.isEmpty) return false
        sortedNests.sort { a, b ->
            (a.position.dst2(megaman.body.getCenter()) - b.position.dst2(megaman.body.getCenter())).toInt()
        }
        nestId = sortedNests[0].id

        return true
    }

    private fun getSmoothSpeedByMidpointDist(minSpeed: Float, maxSpeed: Float): Float {
        val midPoint = startPosition.midPoint(targetPosition)

        val maxDistance = targetPosition.dst(midPoint)

        var scalar = (maxDistance - body.getCenter().dst(midPoint)) / maxDistance
        if (scalar < 0f) scalar = 0.5f

        return minSpeed + (maxSpeed - minSpeed) * scalar
    }

    private fun shouldDive() = hoverTimes >= HOVER_TIMES_BEFORE_DIVE

    private fun dropEgg() {
        dropEggCooldown.reset()

        val spawn = body.getCenter().sub(0f, 0.5f * ConstVals.PPM)
        val egg = MegaEntityFactory.fetch(MockingByteEgg::class)!!
        egg.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo spawn))
    }
}
