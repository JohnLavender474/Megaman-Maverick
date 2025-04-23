package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
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
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.projectiles.ReactorManProjectile
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.utils.misc.HeadUtils
import com.megaman.maverick.game.world.body.*

class ReactorManV2(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "ReactorMan"

        private const val INIT_DUR = 1f

        private const val STAND_DUR = 1f
        private const val STAND_THROW_DUR = 1f
        private const val STAND_THROW_TWO_DUR = 0.5f

        private const val RUN_DUR = 0.5f
        private const val RUN_SPEED = 10f

        private const val JUMP_IMPULSE = 16f
        private const val JUMP_THROW_DUR = 0.75f
        private const val JUMP_THROW_TWO_DUR = 0.5f

        private const val GROW_PROJ_TIME = 0.5f
        private const val PROJ_SPEED = 10f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.001f

        private const val MIN_CYCLES_BEFORE_GIGA = 3
        private const val GIGA_CHANCE_DELTA = 25f

        private val animDefs = orderedMapOf(
            ReactorManState.INIT pairTo AnimationDef(),
            ReactorManState.STAND pairTo AnimationDef(1, 3, 0.1f, true),
            ReactorManState.STAND_THROW pairTo AnimationDef(3, 3, 0.1f, false),
            ReactorManState.STAND_THROW_TWO pairTo AnimationDef(5, 1, 0.1f, false),
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
        ReactorManState.INIT pairTo Timer(INIT_DUR),
        ReactorManState.STAND pairTo Timer(STAND_DUR),
        ReactorManState.STAND_THROW pairTo Timer(STAND_THROW_DUR),
        ReactorManState.STAND_THROW_TWO pairTo Timer(STAND_THROW_TWO_DUR),
        ReactorManState.JUMP_THROW pairTo Timer(JUMP_THROW_DUR),
        ReactorManState.JUMP_THROW_TWO pairTo Timer(JUMP_THROW_TWO_DUR),
    )
    private val currentStateTimer: Timer?
        get() = stateTimers[currentState]

    // incremented each time Reactor Man goes to the stand state
    private var stateCycles = -1

    private val projectiles = OrderedMap<ReactorManProjectile, Vector2>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            animDefs.keys().map { it.name.lowercase() }.forEach { key ->
                regions.put(key, atlas.findRegion("$TAG/$key"))
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

        FacingUtils.setFacingOf(this)

        stateCycles = -1
    }

    override fun isReady(delta: Float) = stateTimers[ReactorManState.INIT].isFinished()

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        projectiles.keys().forEach { it.destroy() }
        projectiles.clear()
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

            currentStateTimer?.let {
                if (shouldUpdateTimer()) {
                    it.update(delta)
                    if (it.isFinished() && shouldGoToNextState()) stateMachine.next()
                }
            }

            when (currentState) {
                else -> {}
            }
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

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

            body.physics.applyFrictionX = shouldApplyFrictionX()
            body.physics.frictionOnSelf.x = getFrictionX()

            body.physics.applyFrictionY = shouldApplyFrictionY()
            body.physics.frictionOnSelf.y = getFrictionY()
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
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
                .setKeySupplier {
                    when {
                        defeated -> "defeated"

                        currentState == ReactorManState.INIT -> when {
                            body.isSensing(BodySense.FEET_ON_GROUND) -> "init"
                            else -> "jump"
                        }

                        currentState == ReactorManState.STAND -> when {
                            body.isSensing(BodySense.FEET_ON_GROUND) -> "stand"
                            else -> "jump"
                        }

                        else -> currentState.name.lowercase()
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<ReactorManState>()
        .setOnChangeState(this::onChangeState)
        // init
        .transition(ReactorManState.INIT, ReactorManState.STAND) { true }
        // stand
        .transition(ReactorManState.STAND, ReactorManState.GIGA_STAND) { shouldPerformGigaAttack() }
        .transition(ReactorManState.STAND, ReactorManState.STAND_THROW) { true }
        .transition(ReactorManState.STAND, ReactorManState.STAND_THROW_TWO) { true }
        .transition(ReactorManState.STAND, ReactorManState.JUMP) { true }
        // stand throw
        .transition(ReactorManState.STAND_THROW, ReactorManState.JUMP) { true }
        // stand throw two
        .transition(ReactorManState.STAND_THROW_TWO, ReactorManState.JUMP) { true }
        // jump
        .transition(ReactorManState.JUMP, ReactorManState.STAND) { true }
        .transition(ReactorManState.JUMP, ReactorManState.JUMP_THROW) { true }
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

        if (current == ReactorManState.STAND) stateCycles++
    }

    private fun shouldApplyFrictionX() = true

    private fun getFrictionX() = 1f

    private fun shouldApplyFrictionY() = true

    private fun getFrictionY() = 1f

    private fun shouldUpdateTimer() = when (currentState) {
        ReactorManState.INIT, ReactorManState.STAND -> body.isSensing(BodySense.FEET_ON_GROUND)
        else -> true
    }

    private fun shouldGoToNextState() = when (currentState) {
        ReactorManState.JUMP_THROW, ReactorManState.JUMP_THROW_TWO -> body.isSensing(BodySense.FEET_ON_GROUND)
        else -> true
    }

    private fun shouldPerformGigaAttack(): Boolean {
        if (stateCycles < MIN_CYCLES_BEFORE_GIGA) return false

        val chance = UtilMethods.getRandom(0f, 100f)
        return chance < stateCycles * GIGA_CHANCE_DELTA
    }

    override fun getTag() = TAG
}
