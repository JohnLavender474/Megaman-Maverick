package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
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
import com.mega.game.engine.cullables.ICullable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.state.EnumStateMachineBuilder
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.DarkSerketClaw
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class DarkSerket(game: MegamanMaverickGame) : AbstractEnemy(game), IFreezableEntity, IAnimatedEntity, IFaceable, ICullable {

    companion object {
        const val TAG = "DarkSerket"

        private const val CULL_TIME = 1f

        private const val STAND_DUR = 0.5f

        private const val SCURRY_DUR = 0.75f
        private const val SCURRY_GROUND_VEL_X = 8f
        private const val SCURRY_AIR_VEL_X = 4f

        private const val JUMP_IMPULSE_X = 15f
        private const val JUMP_IMPULSE_Y = 10f
        private const val RANDOM_CLOSE_RANGE_JUMP_CHANCE = 35f

        private const val LAUNCH_CLAWS_DUR = 1f
        private const val LAUNCH_CLAW_1_TIME = 0.1f
        private const val LAUNCH_CLAW_2_TIME = 0.3f
        private const val LAUNCH_CLAW_X_VEL = 10f

        private const val GROW_CLAWS_DUR = 0.1f

        private const val SCANNER_WIDTH = 6f
        private const val SCANNER_HEIGHT = 0.75f

        private const val GRAVITY = -0.375f
        private const val GROUND_GRAV = -0.01f

        private val animDefs = orderedMapOf(
            "stand" pairTo AnimationDef(2, 1, gdxArrayOf(0.9f, 0.1f), true),
            "scurry" pairTo AnimationDef(2, 1, 0.1f, true),
            "launch_claws" pairTo AnimationDef(2, 2, 0.1f, false),
            "jump" pairTo AnimationDef(),
            "grow_claws" pairTo AnimationDef(),
            "frozen" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DarkSerkerState { STAND, SCURRY, JUMP, LAUNCH_CLAWS, GROW_CLAWS }

    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(
        this,
        onFrozen = {
            stateMachine.reset()
            stateTimers.values().forEach { it.reset() }
        }
    )

    private lateinit var stateMachine: StateMachine<DarkSerkerState>
    private val currentState: DarkSerkerState
        get() = stateMachine.getCurrentElement()
    private val stateTimers = orderedMapOf(
        DarkSerkerState.STAND pairTo Timer(STAND_DUR),
        DarkSerkerState.SCURRY pairTo Timer(SCURRY_DUR),
        DarkSerkerState.LAUNCH_CLAWS pairTo Timer(LAUNCH_CLAWS_DUR)
            .addRunnable(TimeMarkedRunnable(LAUNCH_CLAW_1_TIME) { launchClaw(0) })
            .addRunnable(TimeMarkedRunnable(LAUNCH_CLAW_2_TIME) { launchClaw(1) }),
        DarkSerkerState.GROW_CLAWS pairTo Timer(GROW_CLAWS_DUR)
    )
    private val currentStateTimer: Timer?
        get() = stateTimers[currentState]
    private var stateCycles = 0

    private lateinit var leftFootFixture: Fixture
    private lateinit var rightFootFixture: Fixture

    private val scanner = GameRectangle().setSize(SCANNER_WIDTH * ConstVals.PPM, SCANNER_HEIGHT * ConstVals.PPM)

    private val cullTimer = Timer(CULL_TIME)

    private val reusableFixtureArray = Array<IFixture>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        stateCycles = 0
        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        FacingUtils.setFacingOf(this)

        cullTimer.reset()
        putCullable(ConstKeys.CUSTOM_CULL, this)

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        frozen = false
    }

    override fun shouldBeCulled(delta: Float) = cullTimer.isFinished()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            freezeHandler.update(delta)

            if (frozen) {
                body.physics.velocity.setZero()
                return@add
            }

            val fixtures = reusableFixtureArray
            body.getAllFixtures(fixtures)

            val updateCullTimer = fixtures.none { fixture ->
                fixture.getShape().overlaps(game.getGameCamera().getRotatedBounds())
            }
            if (updateCullTimer) cullTimer.update(delta) else cullTimer.reset()

            fixtures.clear()

            if (shouldUpdateStateTimer()) {
                currentStateTimer?.update(delta)
                if (currentStateTimer?.isFinished() == true) onStateTimerFinished()
            }

            when (currentState) {
                DarkSerkerState.SCURRY -> {
                    val scurryVelX = when {
                        body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_SAND) -> SCURRY_GROUND_VEL_X
                        else -> SCURRY_AIR_VEL_X
                    }
                    body.physics.velocity.x = scurryVelX * ConstVals.PPM * facing.value

                    if (shouldStopScurrying()) stateMachine.next()
                }
                DarkSerkerState.JUMP -> {
                    if (FacingUtils.isFacingBlock(this)) body.physics.velocity.x = 0f
                    if (shouldEndJump()) stateMachine.next()
                }
                else -> {}
            }
        }
    }

    private fun shouldUpdateStateTimer() = when (currentState) {
        DarkSerkerState.STAND -> body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_SAND)
        DarkSerkerState.JUMP -> false
        else -> true
    }

    private fun onStateTimerFinished() {
        GameLogger.debug(TAG, "onStateTimerFinished(): currentState=$currentState")
        currentStateTimer?.reset()
        stateMachine.next()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(1.75f * ConstVals.PPM, 0.875f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val leftSideFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()
            )
        )
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftSideFixture.offsetFromBodyAttachment.set(-body.getWidth() / 2f, 0.5f * ConstVals.PPM)
        body.addFixture(leftSideFixture)
        debugShapes.add { leftSideFixture }

        val rightSideFixture = Fixture(
            body, FixtureType.SIDE, GameRectangle().setSize(
                0.1f * ConstVals.PPM, ConstVals.PPM.toFloat()
            )
        )
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightSideFixture.offsetFromBodyAttachment.set(body.getWidth() / 2f, 0.5f * ConstVals.PPM)
        body.addFixture(rightSideFixture)
        debugShapes.add { rightSideFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        // This is set to prevent the scorpion from sinking in quick sand due to the "feet-rise-sink block" falling.
        // However, setting this to false makes it so that the scorpion's feet won't "stick" to any blocks. If this
        // becomes an issue, then "STICK_TO_BLICK" will need to be a callback instead of a boolean flag.
        feetFixture.setShouldStickToBlock(false)
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GRAY
        body.addFixture(feetFixture)

        val stingerDamager1 =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.25f * ConstVals.PPM, 1f * ConstVals.PPM))
        body.addFixture(stingerDamager1)
        stingerDamager1.drawingColor = Color.RED
        debugShapes.add { stingerDamager1 }

        val stingerDamageable1 =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.25f * ConstVals.PPM, 1f * ConstVals.PPM))
        body.addFixture(stingerDamageable1)

        /*
        val stingerShield1 =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.25f * ConstVals.PPM, 1f * ConstVals.PPM))
        body.addFixture(stingerShield1)
         */

        val stingerDamager2 = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.5f * ConstVals.PPM))
        body.addFixture(stingerDamager2)
        stingerDamager2.drawingColor = Color.RED
        debugShapes.add { stingerDamager2 }

        val stingerDamageable2 = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.5f * ConstVals.PPM))
        body.addFixture(stingerDamageable2)

        /*
        val stingerShield2 = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.5f * ConstVals.PPM))
        body.addFixture(stingerShield2)
         */

        val clawDamager = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.5f * ConstVals.PPM))
        body.addFixture(clawDamager)
        clawDamager.drawingColor = Color.RED
        debugShapes.add { clawDamager }

        val clawDamageable = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.5f * ConstVals.PPM))
        body.addFixture(clawDamageable)

        /*
        val clawShield = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.5f * ConstVals.PPM))
        body.addFixture(clawShield)
         */

        leftFootFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        leftFootFixture.offsetFromBodyAttachment.set(
            (-0.25f * ConstVals.PPM) - (body.getWidth() / 2f),
            -body.getHeight() / 2f
        )
        leftFootFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH) }
        leftFootFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.BLOCK -> leftFootFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> leftFootFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(leftFootFixture)
        debugShapes.add { leftFootFixture }

        rightFootFixture = Fixture(
            body, FixtureType.CONSUMER, GameRectangle().setSize(
                0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM
            )
        )
        rightFootFixture.offsetFromBodyAttachment.set(
            (0.25f * ConstVals.PPM) + (body.getWidth() / 2f),
            -body.getHeight() / 2f
        )
        rightFootFixture.setFilter { fixture -> fixture.getType().equalsAny(FixtureType.BLOCK, FixtureType.DEATH) }
        rightFootFixture.setConsumer { _, fixture ->
            when (fixture.getType()) {
                FixtureType.BLOCK -> rightFootFixture.putProperty(ConstKeys.BLOCK, true)
                FixtureType.DEATH -> rightFootFixture.putProperty(ConstKeys.DEATH, true)
            }
        }
        body.addFixture(rightFootFixture)
        debugShapes.add { rightFootFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            leftFootFixture.putProperty(ConstKeys.BLOCK, false)
            leftFootFixture.putProperty(ConstKeys.DEATH, false)
            rightFootFixture.putProperty(ConstKeys.BLOCK, false)
            rightFootFixture.putProperty(ConstKeys.DEATH, false)

            val gravity =
                if (body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_SAND)) GROUND_GRAV else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

            if (body.isSensing(BodySense.FEET_ON_SAND)) {
                body.physics.gravityOn = false
                if (body.physics.velocity.y < 0f) body.physics.velocity.y = 0f
            } else body.physics.gravityOn = true

            stingerDamager1.offsetFromBodyAttachment.set(
                0.75f * ConstVals.PPM * -facing.value,
                0.75f * ConstVals.PPM
            )
            stingerDamageable1.offsetFromBodyAttachment.set(
                0.75f * ConstVals.PPM * -facing.value,
                0.75f * ConstVals.PPM
            )
            /*
            stingerShield1.offsetFromBodyAttachment.set(
                0.74f * ConstVals.PPM * -facing.value,
                0.75f * ConstVals.PPM
            )
             */

            stingerDamager2.offsetFromBodyAttachment.set(
                0.2f * ConstVals.PPM * -facing.value,
                1f * ConstVals.PPM
            )
            stingerDamageable2.offsetFromBodyAttachment.set(
                0.2f * ConstVals.PPM * -facing.value,
                1f * ConstVals.PPM
            )
            /*
            stingerShield2.offsetFromBodyAttachment.set(
                0.2f * ConstVals.PPM * -facing.value,
                1f * ConstVals.PPM
            )
             */

            clawDamager.offsetFromBodyAttachment.set(
                1.25f * ConstVals.PPM * facing.value, 0f
            )
            /*
            clawShield.offsetFromBodyAttachment.set(
                1.25f * ConstVals.PPM * facing.value, 0f
            )
            clawShield.setActive(!currentState.equalsAny(DarkSerkerState.LAUNCH_CLAWS, DarkSerkerState.GROW_CLAWS))
             */
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
        .preProcess { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(0.2f * ConstVals.PPM * facing.value)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (frozen) "frozen" else currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<DarkSerkerState>()
        .initialState(DarkSerkerState.STAND)
        .onChangeState(this::onChangeState)
        // stand
        .transition(DarkSerkerState.STAND, DarkSerkerState.JUMP) { shouldJump() }
        .transition(DarkSerkerState.STAND, DarkSerkerState.LAUNCH_CLAWS) { shouldLaunchClaws() }
        .transition(DarkSerkerState.STAND, DarkSerkerState.SCURRY) { true }
        // jump
        .transition(DarkSerkerState.JUMP, DarkSerkerState.STAND) { true }
        // scurry
        .transition(DarkSerkerState.SCURRY, DarkSerkerState.JUMP) { shouldJump() }
        .transition(DarkSerkerState.SCURRY, DarkSerkerState.LAUNCH_CLAWS) { shouldLaunchClaws() }
        .transition(DarkSerkerState.SCURRY, DarkSerkerState.STAND) { true }
        // launch claws
        .transition(DarkSerkerState.LAUNCH_CLAWS, DarkSerkerState.GROW_CLAWS) { true }
        // grow claws
        .transition(DarkSerkerState.GROW_CLAWS, DarkSerkerState.STAND) { true }
        // build
        .build()

    private fun onChangeState(current: DarkSerkerState, previous: DarkSerkerState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        currentStateTimer?.reset()

        when (current) {
            DarkSerkerState.STAND -> {
                body.physics.velocity.x = 0f
                stateCycles++
            }
            DarkSerkerState.JUMP -> {
                FacingUtils.setFacingOf(this)
                jump()
            }
            DarkSerkerState.SCURRY -> when {
                FacingUtils.isFacingBlock(this) || isFootOnDeath(facing) -> swapFacing()
                else -> FacingUtils.setFacingOf(this)
            }
            DarkSerkerState.LAUNCH_CLAWS -> {
                body.physics.velocity.x = 0f
                FacingUtils.setFacingOf(this)
            }
            else -> {}
        }

        when (previous) {
            DarkSerkerState.JUMP -> body.physics.velocity.setZero()
            else -> {}
        }
    }

    private fun isAnyBlockInTheWay() = MegaGameEntities.getOfType(EntityType.BLOCK).any any@{
        val blockBounds = (it as IBodyEntity).body.getBounds()
        return@any blockBounds.overlaps(scanner) && when (facing) {
            Facing.LEFT -> blockBounds.getCenter().x > megaman.body.getCenter().x
            Facing.RIGHT -> blockBounds.getCenter().x < megaman.body.getCenter().x
        }
    }

    private fun shouldStopScurrying() =
        FacingUtils.isFacingBlock(this) || isFootOnDeath(facing) || shouldJump()

    private fun shouldJump() = shouldJumpAtLedge() || shouldJumpToAttack()

    private fun shouldJumpAtLedge() = !isFootOnBlock(facing) &&
        (facing == FacingUtils.getPreferredFacingFor(this) ||
            megaman.body.getBounds().getY() >= body.getBounds().getMaxY() ||
            megaman.body.getBounds().getMaxY() <= body.getBounds().getY())

    private fun shouldJumpToAttack() = isMegamanOverlappingScanner() && !isAnyBlockInTheWay() &&
        UtilMethods.getRandom(0f, 100f) <= RANDOM_CLOSE_RANGE_JUMP_CHANCE

    private fun jump() {
        GameLogger.debug(TAG, "jump()")
        body.physics.velocity.set(JUMP_IMPULSE_X * facing.value, JUMP_IMPULSE_Y).scl(ConstVals.PPM.toFloat())
    }

    private fun isFootOnBlock(facing: Facing) = when (facing) {
        Facing.LEFT -> leftFootFixture.isProperty(ConstKeys.BLOCK, true)
        Facing.RIGHT -> rightFootFixture.isProperty(ConstKeys.BLOCK, true)
    }

    private fun isFootOnDeath(facing: Facing) = when (facing) {
        Facing.LEFT -> leftFootFixture.isProperty(ConstKeys.DEATH, true)
        Facing.RIGHT -> rightFootFixture.isProperty(ConstKeys.DEATH, true)
    }

    private fun shouldEndJump() = body.physics.velocity.y <= 0f &&
        body.isSensingAny(BodySense.FEET_ON_GROUND, BodySense.FEET_ON_SAND)

    private fun shouldLaunchClaws() = isMegamanOverlappingScanner() && !isAnyBlockInTheWay()

    private fun launchClaw(index: Int) {
        GameLogger.debug(TAG, "launchClaw(): index=$index")

        val position = when (index) {
            0 -> body.getCenter().add(1f * ConstVals.PPM * facing.value, 0.1f * ConstVals.PPM)
            else -> body.getCenter().add(0.1f * ConstVals.PPM * -facing.value, 0.1f * ConstVals.PPM)
        }

        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(LAUNCH_CLAW_X_VEL * ConstVals.PPM * facing.value, 0f)

        /*
        val gravity = GameObjectPools.fetch(Vector2::class)
            .set(0f, LAUNCH_CLAW_GRAV_Y * ConstVals.PPM)
         */

        val claw = MegaEntityFactory.fetch(DarkSerketClaw::class)!!
        claw.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.FACING pairTo facing,
                // ConstKeys.GRAVITY pairTo gravity,
                ConstKeys.POSITION pairTo position,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        requestToPlaySound(SoundAsset.BURST_SOUND, false)
    }

    private fun isMegamanOverlappingScanner(): Boolean {
        val position = if (isFacing(Facing.LEFT)) Position.CENTER_RIGHT else Position.CENTER_LEFT
        scanner.positionOnPoint(body.getCenter(), position)
        return scanner.overlaps(megaman.body.getBounds())
    }
}
