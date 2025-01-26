package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class LumberJoe(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "AxeJoe"

        private const val STAND_DUR = 0.75f
        private const val THROW_DUR = 0.5f
        private const val THROW_TIME = 0.125f
        private const val COOLDOWN_DUR = 1.5f

        private const val AXES_BEFORE_COOLDOWN = 3

        private const val JUMP_SENSOR_WIDTH = 1f
        private const val JUMP_SENSOR_HEIGHT = 4f

        private const val JUMP_IMPULSE = 15f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val GROUND_FRICTION_X = 5f
        private const val AIR_FRICTION_X = 1f

        private const val AXE_MAX_IMPULSE_X = 10f
        private const val AXE_IMPULSE_Y = 12f

        private const val SHIELD_OFFSET = 0.5f

        private const val THROW_SHIELDED_BUT_EXPOSED_INDEX = 1

        private const val SHIELDED_REGION_SUFFIX = "_shielded"

        private val animDefs = orderedMapOf(
            "stand" pairTo AnimationDef(2, 1, gdxArrayOf(0.5f, 0.15f), true),
            "cooldown" pairTo AnimationDef(2, 1, gdxArrayOf(0.25f, 0.25f), true),
            "throw" pairTo AnimationDef(3, 1, gdxArrayOf(0.125f, 0.25f, 0.125f), false),
            "jump" pairTo AnimationDef(),
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class AxeJoeState { STAND, JUMP, THROW, COOLDOWN }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<AxeJoeState>
    private val currentState: AxeJoeState
        get() = stateMachine.getCurrent()
    private val stateTimers = OrderedMap<AxeJoeState, Timer>()
    private var axesThrown = 0
    private var hasShield = true

    private val jumpSensor =
        GameRectangle().setSize(JUMP_SENSOR_WIDTH * ConstVals.PPM, JUMP_SENSOR_HEIGHT * ConstVals.PPM)

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            AxeJoeState.entries.forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))

                val shieldedKey = "${key}$SHIELDED_REGION_SUFFIX"
                regions.put(shieldedKey, atlas.findRegion("$TAG/$shieldedKey"))
            }
        }

        if (stateTimers.isEmpty) {
            stateTimers.put(AxeJoeState.STAND, Timer(STAND_DUR))
            stateTimers.put(
                AxeJoeState.THROW,
                Timer(THROW_DUR).addRunnables(TimeMarkedRunnable(THROW_TIME) { throwAxe() })
            )
            stateTimers.put(AxeJoeState.COOLDOWN, Timer(COOLDOWN_DUR))
        }

        super.init()

        stateMachine = buildStateMachine()

        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(position)

        updateFacing()

        val initImpulseX = spawnProps.getOrDefault("${ConstKeys.IMPULSE}_${ConstKeys.X}", 0f, Float::class)
        val initImpulseY = spawnProps.getOrDefault("${ConstKeys.IMPULSE}_${ConstKeys.Y}", 0f, Float::class)
        body.physics.velocity.set(initImpulseX * facing.value, initImpulseY).scl(ConstVals.PPM.toFloat())

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        axesThrown = 0

        hasShield = true
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            jumpSensor.setBottomCenterToPoint(body.getPositionPoint(Position.TOP_CENTER))

            if (currentState.equalsAny(AxeJoeState.STAND, AxeJoeState.COOLDOWN)) updateFacing()

            when (currentState) {
                AxeJoeState.JUMP -> if (shouldStopJump()) stateMachine.next()
                else -> {
                    val timer = stateTimers[currentState]

                    timer.update(delta)

                    if (timer.isFinished()) {
                        stateMachine.next()

                        timer.reset()
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.8f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.8f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)

        val shieldFixture = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(0.5f * ConstVals.PPM, ConstVals.PPM.toFloat())
        )
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { if (shieldFixture.isActive()) shieldFixture else null }

        body.preProcess.put(ConstKeys.DEFAULT) {
            shieldFixture.setActive(isShielded())
            shieldFixture.offsetFromBodyAttachment.x = SHIELD_OFFSET * ConstVals.PPM * facing.value

            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            body.physics.defaultFrictionOnSelf.x =
                if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_FRICTION_X else AIR_FRICTION_X
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2.5f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    val key = currentState.name.lowercase()
                    return@keySupplier if (hasShield) "${key}${SHIELDED_REGION_SUFFIX}" else key
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        var key = entry.key
                        val def = entry.value
                        animations.put(key, Animation(regions[key], def.rows, def.cols, def.durations, def.loop))

                        key = "${key}$SHIELDED_REGION_SUFFIX"
                        animations.put(key, Animation(regions[key], def.rows, def.cols, def.durations, def.loop))
                    }
                }
                .build()
        )
        .build()

    private fun isShielded(): Boolean {
        if (!hasShield) return false

        if (currentState == AxeJoeState.THROW) {
            val animator = animators[TAG] as Animator

            val key = "${AxeJoeState.THROW.name.lowercase()}${SHIELDED_REGION_SUFFIX}"
            if (animator.currentKey != key) return false

            val index = animator.animations[key].getIndex()
            return index != THROW_SHIELDED_BUT_EXPOSED_INDEX
        }

        return true
    }

    private fun updateFacing() {
        when {
            megaman.body.getMaxX() <= body.getX() -> facing = Facing.LEFT
            megaman.body.getX() >= body.getMaxX() -> facing = Facing.RIGHT
        }
    }

    private fun buildStateMachine() = StateMachineBuilder<AxeJoeState>()
        .states { states -> AxeJoeState.entries.forEach { states.put(it.name, it) } }
        .setTriggerChangeWhenSameElement(false)
        .setOnChangeState(this::onChangeState)
        .initialState(AxeJoeState.JUMP.name)
        // stand
        .transition(AxeJoeState.STAND.name, AxeJoeState.JUMP.name) { shouldJump() }
        .transition(AxeJoeState.STAND.name, AxeJoeState.THROW.name) { true }
        // jump
        .transition(AxeJoeState.JUMP.name, AxeJoeState.STAND.name) { shouldStopJump() }
        // throw
        .transition(AxeJoeState.THROW.name, AxeJoeState.COOLDOWN.name) { shouldCooldown() }
        .transition(AxeJoeState.THROW.name, AxeJoeState.STAND.name) { true }
        // cooldown
        .transition(AxeJoeState.COOLDOWN.name, AxeJoeState.JUMP.name) { !body.isSensing(BodySense.FEET_ON_GROUND) }
        .transition(AxeJoeState.COOLDOWN.name, AxeJoeState.STAND.name) { true }
        .build()

    private fun onChangeState(current: AxeJoeState, previous: AxeJoeState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        when (current) {
            AxeJoeState.JUMP -> jump()
            AxeJoeState.THROW -> axesThrown++
            AxeJoeState.COOLDOWN -> axesThrown = 0
            else -> {}
        }
    }

    private fun shouldCooldown() = axesThrown >= AXES_BEFORE_COOLDOWN

    private fun shouldJump() =
        megaman.body.getBounds().overlaps(jumpSensor) || !body.isSensing(BodySense.FEET_ON_GROUND)

    private fun shouldStopJump() = body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f

    private fun jump() {
        GameLogger.debug(TAG, "jump()")

        body.physics.velocity.set(0f, JUMP_IMPULSE * ConstVals.PPM)
    }

    private fun throwAxe() {
        GameLogger.debug(TAG, "throwAxe()")

        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getPositionPoint(Position.TOP_CENTER),
            megaman.body.getPositionPoint(Position.BOTTOM_CENTER),
            AXE_IMPULSE_Y * ConstVals.PPM
        )
        impulse.x = impulse.x.coerceIn(-AXE_MAX_IMPULSE_X * ConstVals.PPM, AXE_MAX_IMPULSE_X * ConstVals.PPM)

        val spawn = body.getCenter().add(-0.25f * ConstVals.PPM * facing.value, ConstVals.PPM.toFloat())

        val axe = MegaEntityFactory.fetch(Axe::class)!!
        axe.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.IMPULSE pairTo impulse))
    }
}
