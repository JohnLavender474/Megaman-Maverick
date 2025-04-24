package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.entities.projectiles.Needle.NeedleType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class SpikeBot(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "SpikeBot"

        private const val STAND_DUR = 0.25f

        private const val SHOOT_DUR = 0.5f
        private const val SHOOT_TIME = 0.25f

        private const val WALK_DUR = 0.75f
        private const val WALK_SPEED = 4f

        private const val NEEDLES = 3
        private const val NEEDLE_GRAV = -0.1f
        private const val NEEDLE_IMPULSE = 10f
        private const val NEEDLE_Y_OFFSET = 0.1f

        private const val JUMP_VEL_X = 5f
        private const val JUMP_IMPULSE_Y = 10f
        private const val LEFT_FOOT = "${ConstKeys.LEFT}_${ConstKeys.FOOT}"
        private const val RIGHT_FOOT = "${ConstKeys.RIGHT}_${ConstKeys.FOOT}"

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val NORMAL_MOVEMENT_SCALAR = 1f
        private const val WATER_MOVEMENT_SCALAR = 0.5f

        private val ANGLES = gdxArrayOf(45f, 0f, 315f)
        private val X_OFFSETS = gdxArrayOf(-0.1f, 0f, 0.1f)

        private val animDefs = orderedMapOf(
            SpikeBotState.JUMP pairTo AnimationDef(),
            SpikeBotState.STAND pairTo AnimationDef(),
            SpikeBotState.WALK pairTo AnimationDef(2, 2, 0.1f, true),
            SpikeBotState.SHOOT pairTo AnimationDef(1, 5, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SpikeBotType { CACTUS, SNOW }
    private enum class SpikeBotState { STAND, WALK, SHOOT, JUMP }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<SpikeBotState>
    private val currentState: SpikeBotState
        get() = stateMachine.getCurrent()

    private val stateTimers = orderedMapOf(
        SpikeBotState.WALK pairTo Timer(WALK_DUR),
        SpikeBotState.STAND pairTo Timer(STAND_DUR),
        SpikeBotState.SHOOT pairTo Timer(SHOOT_DUR)
            .addRunnable(TimeMarkedRunnable(SHOOT_TIME) { shoot() }),
    )
    private val currentStateTimer: Timer?
        get() = stateTimers[currentState]

    private lateinit var type: SpikeBotType

    private val animator: Animator
        get() = animators[TAG] as Animator

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            SpikeBotType.entries.map { it.name.lowercase() }.forEach { type ->
                animDefs.keys().map { it.name.lowercase() }.forEach { key ->
                    regions.put("$type/$key", atlas.findRegion("$TAG/$type/$key"))
                }
            }
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

        type = when {
            spawnProps.containsKey(ConstKeys.TYPE) -> {
                val rawType = spawnProps.get(ConstKeys.TYPE)
                rawType as? SpikeBotType ?: when (rawType) {
                    is String -> SpikeBotType.valueOf(rawType.uppercase())
                    else -> throw IllegalArgumentException("Illegal value for type: $rawType")
                }
            }

            else -> SpikeBotType.CACTUS
        }

        FacingUtils.setFacingOf(this)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            movementScalar = if (body.isSensing(BodySense.IN_WATER)) WATER_MOVEMENT_SCALAR else NORMAL_MOVEMENT_SCALAR
            animator.updateScalar = movementScalar

            val timer = currentStateTimer
            if (timer != null) {
                timer.update(delta)
                if (timer.isFinished()) stateMachine.next()
            }

            when (currentState) {
                SpikeBotState.STAND, SpikeBotState.SHOOT -> body.physics.velocity.x = 0f
                SpikeBotState.JUMP -> if (shouldEndJump()) stateMachine.next()
                SpikeBotState.WALK -> {
                    body.physics.velocity.x = WALK_SPEED * ConstVals.PPM * facing.value * movementScalar
                    if (shouldSwapFacing()) swapFacing() else if (shouldJump()) stateMachine.next()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.putProperty(LEFT_FOOT, false)
        body.putProperty(RIGHT_FOOT, false)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val leftSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftSideFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftSideFixture)
        leftSideFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftSideFixture }

        val rightSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightSideFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightSideFixture)
        rightSideFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightSideFixture }

        val leftFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFootFixture.setFilter { fixture -> fixture.getType() == FixtureType.BLOCK }
        leftFootFixture.setConsumer { _, fixture -> body.putProperty(LEFT_FOOT, true) }
        leftFootFixture.offsetFromBodyAttachment.set(-body.getWidth() / 2f, -body.getHeight() / 2f)
        body.addFixture(leftFootFixture)
        leftFootFixture.drawingColor = Color.ORANGE
        debugShapes.add { leftFootFixture }

        val rightFootFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFootFixture.setFilter { fixture -> fixture.getType() == FixtureType.BLOCK }
        rightFootFixture.setConsumer { _, fixture -> body.putProperty(RIGHT_FOOT, true) }
        rightFootFixture.offsetFromBodyAttachment.set(body.getWidth() / 2f, -body.getHeight() / 2f)
        body.addFixture(rightFootFixture)
        rightFootFixture.drawingColor = Color.ORANGE
        debugShapes.add { rightFootFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.putProperty(LEFT_FOOT, false)
            body.putProperty(RIGHT_FOOT, false)

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM * movementScalar

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0)
                body.physics.velocity.y = 0f
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE, FixtureType.WATER_LISTENER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(1.5f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.LEFT), false)
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { "${type.name.lowercase()}/${currentState.name.lowercase()}" }
                .applyToAnimations { animations ->
                    SpikeBotType.entries.map { it.name.lowercase() }.forEach { type ->
                        animDefs.forEach { entry ->
                            val key = "$type/${entry.key.name.lowercase()}"
                            val (rows, columns, durations, loop) = entry.value
                            animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                        }
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<SpikeBotState>()
        .initialState(SpikeBotState.STAND)
        .onChangeState(this::onChangeState)
        .transition(SpikeBotState.STAND, SpikeBotState.WALK) { true }
        .transition(SpikeBotState.WALK, SpikeBotState.JUMP) { shouldJump() }
        .transition(SpikeBotState.WALK, SpikeBotState.SHOOT) { true }
        .transition(SpikeBotState.JUMP, SpikeBotState.WALK) { true }
        .transition(SpikeBotState.SHOOT, SpikeBotState.WALK) { true }
        .build()

    private fun onChangeState(current: SpikeBotState, previous: SpikeBotState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")
        if (!current.equalsAny(SpikeBotState.WALK, SpikeBotState.SHOOT)) FacingUtils.setFacingOf(this)
        if (current != SpikeBotState.WALK) stateTimers[current]?.reset()
        if (current == SpikeBotState.JUMP) jump()
        if (previous == SpikeBotState.WALK && stateTimers[SpikeBotState.WALK].isFinished())
            stateTimers[SpikeBotState.WALK].reset()
    }

    private fun shouldSwapFacing() = FacingUtils.isFacingBlock(this) ||
        (isFacing(Facing.LEFT) && !body.isProperty(LEFT_FOOT, true) && megaman.body.getX() > body.getX()) ||
        (isFacing(Facing.RIGHT) && !body.isProperty(RIGHT_FOOT, true) && megaman.body.getX() < body.getX())

    private fun shouldJump() =
        (isFacing(Facing.LEFT) && !body.isProperty(LEFT_FOOT, true) && megaman.body.getX() < body.getX()) ||
            (isFacing(Facing.RIGHT) && !body.isProperty(RIGHT_FOOT, true) && megaman.body.getX() > body.getX())

    private fun jump() {
        body.physics.velocity.set(
            JUMP_VEL_X * ConstVals.PPM * facing.value * movementScalar,
            JUMP_IMPULSE_Y * ConstVals.PPM * movementScalar
        )
    }

    private fun shouldEndJump() = body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND)

    private fun shoot() {
        for (i in 0 until NEEDLES) {
            val xOffset = X_OFFSETS[i]
            val position = body.getPositionPoint(Position.TOP_CENTER)
                .add(xOffset * ConstVals.PPM, NEEDLE_Y_OFFSET * ConstVals.PPM)

            val angle = ANGLES[i]
            val impulse = GameObjectPools.fetch(Vector2::class)
                .set(0f, NEEDLE_IMPULSE * ConstVals.PPM)
                .rotateDeg(angle)
                .scl(movementScalar)

            val gravity = NEEDLE_GRAV * ConstVals.PPM

            val needleType = when (type) {
                SpikeBotType.CACTUS -> NeedleType.DEFAULT
                SpikeBotType.SNOW -> NeedleType.ICE
            }

            val needle = MegaEntityFactory.fetch(Needle::class)!!
            needle.spawn(
                props(
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.GRAVITY pairTo gravity,
                    ConstKeys.TYPE pairTo needleType,
                    ConstKeys.OWNER pairTo this
                )
            )
        }

        if (overlapsGameCamera()) {
            val soundAss = when (type) {
                SpikeBotType.CACTUS -> SoundAsset.THUMP_SOUND
                SpikeBotType.SNOW -> SoundAsset.ICE_SHARD_1_SOUND
            }
            requestToPlaySound(soundAss, false)
        }
    }
}
