package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.coerceIn
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class IceskatePeng(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "IceSkaterPeng"

        private const val BRAKE_MAX_DUR = 0.5f

        private const val SKATE_IMPULSE_X = 15f
        private const val SKATE_MAX_VEL_X = 8f

        private const val JUMP_IMPULSE_Y = 10f
        private const val JUMP_MAX_VEL_X = 2f

        private const val SENSOR_WIDTH = 12f
        private const val SENSOR_HEIGHT = 2f

        private const val DEFAULT_FRICTION_X = 1.25f
        private const val BRAKE_FRICTION_X = 2.5f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val CULL_TIME = 1f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class IceSkaterPengState { SKATE, BRAKE, JUMP }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<IceSkaterPengState>
    private val currentState: IceSkaterPengState
        get() = stateMachine.getCurrent()

    private val brakeTimer = Timer(BRAKE_MAX_DUR)

    private val sensor = GameRectangle().setSize(SENSOR_WIDTH * ConstVals.PPM, SENSOR_HEIGHT * ConstVals.PPM)

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            IceSkaterPengState.entries.forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }

        super.init()

        addComponent(defineAnimationsComponent())

        stateMachine = buildStateMachine()
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)

        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        stateMachine.reset()
        brakeTimer.reset()

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            sensor.setCenter(body.getCenter())

            when (currentState) {
                IceSkaterPengState.SKATE -> {
                    body.physics.velocity.let { velocity ->
                        when {
                            shouldBounceWall() -> {
                                velocity.x = -velocity.x
                                swapFacing()
                            }

                            shouldJump() || shouldBrake() -> stateMachine.next()
                        }

                        val impulseX = SKATE_IMPULSE_X * ConstVals.PPM * delta * facing.value
                        velocity.x += impulseX

                        velocity.x = velocity.x.coerceIn(SKATE_MAX_VEL_X * ConstVals.PPM)
                    }
                }

                IceSkaterPengState.BRAKE -> {
                    brakeTimer.update(delta)

                    if (brakeTimer.isFinished() || shouldBounceWall()) {
                        updateFacing()
                        brakeTimer.reset()
                        stateMachine.next()
                    }
                }

                IceSkaterPengState.JUMP -> {
                    updateFacing()

                    if ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                        (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
                    ) body.physics.velocity.x = 0f

                    body.physics.velocity.let { velocity ->
                        velocity.x = velocity.x.coerceIn(JUMP_MAX_VEL_X * ConstVals.PPM)
                    }

                    if (body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f) stateMachine.next()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionY = false
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                ConstVals.PPM * if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY

            if (body.physics.velocity.y > 0f && body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))
                body.physics.velocity.y = 0f
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    IceSkaterPengState.entries.forEach { state ->
                        val key = state.name.lowercase()
                        animations.put(key, Animation(regions[key], 2, 1, 0.1f, true))
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = StateMachineBuilder<IceSkaterPengState>()
        .setOnChangeState(this::onChangeState)
        .initialState(IceSkaterPengState.SKATE.name)
        .states { states -> IceSkaterPengState.entries.forEach { state -> states.put(state.name, state) } }
        .transition(IceSkaterPengState.SKATE.name, IceSkaterPengState.JUMP.name) { shouldJump() }
        .transition(IceSkaterPengState.SKATE.name, IceSkaterPengState.BRAKE.name) { shouldBrake() }
        .transition(IceSkaterPengState.BRAKE.name, IceSkaterPengState.SKATE.name) { true }
        .transition(IceSkaterPengState.JUMP.name, IceSkaterPengState.BRAKE.name) { true }
        .build()

    private fun onChangeState(current: IceSkaterPengState, previous: IceSkaterPengState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        when (current) {
            IceSkaterPengState.BRAKE -> {
                body.physics.defaultFrictionOnSelf.x = BRAKE_FRICTION_X
                brakeTimer.reset()
            }

            IceSkaterPengState.JUMP -> jump()

            else -> {}
        }

        if (previous == IceSkaterPengState.BRAKE) body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
    }

    private fun shouldBounceWall() =
        (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
            (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

    private fun shouldBrake() =
        sensor.overlaps(megaman.body.getBounds()) &&
            ((isFacing(Facing.LEFT) && megaman.body.getX() > body.getMaxX()) ||
                (isFacing(Facing.RIGHT) && megaman.body.getMaxX() < body.getX()))

    private fun shouldJump(): Boolean {
        val center = megaman.body.getCenter()
        return center.y >= body.getY() && center.x >= body.getX() && center.x <= body.getMaxX()
    }

    private fun jump() {
        GameLogger.debug(TAG, "jump()")
        body.physics.velocity.y = JUMP_IMPULSE_Y * ConstVals.PPM
    }

    private fun updateFacing() {
        when {
            megaman.body.getX() > body.getMaxX() -> facing = Facing.RIGHT
            megaman.body.getMaxX() < body.getX() -> facing = Facing.LEFT
        }
    }
}
