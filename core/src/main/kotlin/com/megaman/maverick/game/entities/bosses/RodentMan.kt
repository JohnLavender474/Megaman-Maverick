package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class RodentMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "RodentMan"

        private const val INIT_DUR = 1f
        private const val STAND_DUR = 1.5f
        private const val WALLSLIDE_DUR = 1f
        private const val SLASH_DUR = 0.75f
        private const val RUN_DUR = 2f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val JUMP_IMPULSE_Y = 10f
        private const val JUMP_MAX_IMPULSE_X = 8f
        private const val WALL_JUMP_IMPULSE_X = 4f

        private val animDefs = orderedMapOf<String, AnimationDef>(
            "stand" pairTo AnimationDef(2, 1, gdxArrayOf(1f, 0.15f), true),
            "stand_slash" pairTo AnimationDef(2, 1, 0.1f, false),
            "run" pairTo AnimationDef(1, 5, 0.1f, true),
            "jump_down_look_down" pairTo AnimationDef(),
            "jump_down_look_straight" pairTo AnimationDef(),
            "jump_up_look_down" pairTo AnimationDef(),
            "jump_up_look_up" pairTo AnimationDef(),
            "wallslide" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class RodentManState { INIT, STAND, JUMP, SLASH, RUN, WALLSLIDE }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<RodentManState>
    private val currentState: RodentManState
        get() = stateMachine.getCurrent()
    private val stateTimers = orderedMapOf<RodentManState, Timer>(
        RodentManState.INIT pairTo Timer(INIT_DUR),
        RodentManState.STAND pairTo Timer(STAND_DUR),
        RodentManState.WALLSLIDE pairTo Timer(WALLSLIDE_DUR),
        RodentManState.SLASH pairTo Timer(SLASH_DUR),
        RodentManState.RUN pairTo Timer(RUN_DUR)
    )

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        stateMachine = buildStateMachine()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        body.physics.gravityOn = true

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        FacingUtils.setFacingOf(this)
    }

    override fun isReady(delta: Float) = stateTimers[RodentManState.INIT].isFinished()

    override fun triggerDefeat() {
        super.triggerDefeat()
        body.physics.velocity.setZero()
        body.physics.gravityOn = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
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

            val timer = stateTimers[currentState]
            if (timer != null && shouldUpdateStateTimer(currentState)) {
                timer.update(delta)
                if (timer.isFinished()) stateMachine.next()
            }

            when (currentState) {
                RodentManState.INIT -> TODO()
                RodentManState.STAND -> TODO()
                RodentManState.JUMP -> TODO()
                RodentManState.SLASH -> TODO()
                RodentManState.RUN -> TODO()
                RodentManState.WALLSLIDE -> TODO()
            }
        }
    }

    private fun shouldUpdateStateTimer(state: RodentManState) = when (state) {
        RodentManState.INIT -> body.isSensing(BodySense.FEET_ON_GROUND)
        else -> true
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM, 1.75f * ConstVals.PPM)
        body.physics.velocityClamp.set(10f * ConstVals.PPM, 25f * ConstVals.PPM)
        body.physics.applyFrictionX = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture = Fixture(
            body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM)
        )
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val leftFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat())
        )
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat())
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

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder()
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<RodentManState>()
        .setOnChangeState(this::onChangeState)
        .initialState(RodentManState.INIT)
        .build()

    private fun onChangeState(current: RodentManState, previous: RodentManState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")


    }
}
