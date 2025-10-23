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
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.SteamRoller
import com.megaman.maverick.game.entities.blocks.SteamRoller.SteamRollerState
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.AsteroidExplosion
import com.megaman.maverick.game.entities.projectiles.Rock
import com.megaman.maverick.game.entities.projectiles.Rock.RockSize
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class SteamRollerMan(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity,
    IFaceable {

    companion object {
        const val TAG = "SteamRollerMan"

        private const val IDLE_DUR = 0.5f
        private const val SMASH_DUR = 0.4f

        private const val MAX_ROLL_SPEED = 3f
        private const val ROLL_IMPULSE = 5f

        private const val REVERSE_SPEED = 2f

        private const val SMASH_AREA_WIDTH = 2.25f
        private const val SMASH_AREA_HEIGHT = 1f
        private const val SMASH_AREA_OFFSET_X = 1f
        private const val SMASH_AREA_OFFSET_Y = 0.25f
        private const val SMASH_ROCKS = 5
        private const val SMASH_ROCKS_MIN_OFFSET_X = -0.35f
        private const val SMASH_ROCKS_MAX_OFFSET_X = 0.5f
        private const val SMASH_ROCKS_MIN_OFFSET_Y = -0.25f
        private const val SMASH_ROCKS_MAX_OFFSET_Y = -0.1f
        private const val SMASH_ROCKS_MIN_X_IMPULSE = -6f
        private const val SMASH_ROCKS_MAX_X_IMPULSE = 6f
        private const val SMASH_ROCKS_MIN_Y_IMPULSE = 2f
        private const val SMASH_ROCKS_MAX_Y_IMPULSE = 6f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAV = -0.01f

        private val animDefs = orderedMapOf(
            "idle" pairTo AnimationDef(2, 1, gdxArrayOf(0.4f, 0.1f), true),
            "reverse" pairTo AnimationDef(2, 1, 0.1f, true),
            "roll" pairTo AnimationDef(2, 1, 0.1f, true),
            "smash" pairTo AnimationDef(2, 2, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SteamRollerManState { IDLE, ROLL, SMASH, REVERSE }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<SteamRollerManState>
    private val currentState: SteamRollerManState
        get() = stateMachine.getCurrentElement()
    private val stateTimers = orderedMapOf(
        SteamRollerManState.IDLE pairTo Timer(IDLE_DUR),
        SteamRollerManState.SMASH pairTo Timer(SMASH_DUR).setRunOnJustFinished { smash() }
    )
    private val currentStateTimer: Timer?
        get() = stateTimers[currentState]

    private val smashArea = GameRectangle().setSize(
        SMASH_AREA_WIDTH * ConstVals.PPM,
        SMASH_AREA_HEIGHT * ConstVals.PPM
    ).also { it.drawingColor = Color.GREEN }

    private lateinit var shieldFixture1: Fixture

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
        addDebugShapeSupplier { smashArea }
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        if (!super.canSpawn(spawnProps)) return false

        val id = spawnProps.get(ConstKeys.ID, Int::class)!!
        return MegaGameEntities.getOfTag(SteamRoller.TAG).none { it.id == id }
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        facing = Facing.valueOf(spawnProps.get(ConstKeys.FACING, String::class)!!.uppercase())

        val position = if (isFacing(Facing.LEFT)) Position.BOTTOM_RIGHT else Position.BOTTOM_LEFT
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position)

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()

        explode()

        smashArea.setBottomCenterToPoint(
            body.getPositionPoint(Position.BOTTOM_CENTER).add(
                SMASH_AREA_OFFSET_X * ConstVals.PPM * facing.value,
                SMASH_AREA_OFFSET_Y * ConstVals.PPM
            )
        )
        val smashAreaBlockBound = GameObjectPools.fetch(GameRectangle::class)
            .setSize(SMASH_AREA_WIDTH * ConstVals.PPM, 0.1f * ConstVals.PPM)
            .setTopCenterToPoint(smashArea.getPositionPoint(Position.TOP_CENTER))

        val shieldShape = shieldFixture1.getShape().getBoundingRectangle()
        val shieldAreaBlockBound = GameObjectPools.fetch(GameRectangle::class)
            .setSize(shieldShape.getWidth(), 0.1f * ConstVals.PPM)
            .setTopCenterToPoint(shieldShape.getPositionPoint(Position.TOP_CENTER))

        val steamRollerState = when (currentState) {
            SteamRollerManState.IDLE,
            SteamRollerManState.ROLL,
            SteamRollerManState.SMASH -> SteamRollerState.SMASH
            SteamRollerManState.REVERSE -> SteamRollerState.REVERSE
        }

        val steamRoller = MegaEntityFactory.fetch(SteamRoller::class)!!
        steamRoller.spawn(
            props(
                ConstKeys.FACING pairTo facing,
                ConstKeys.ID pairTo id,
                ConstKeys.STATE pairTo steamRollerState,
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER),
                ConstKeys.BLOCKS pairTo gdxArrayOf(smashAreaBlockBound, shieldAreaBlockBound)
            )
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            currentStateTimer?.let {
                it.update(delta)
                if (it.isFinished()) {
                    stateMachine.next()
                    it.reset()
                }
            }

            setSmashAreaPosition()

            when (currentState) {
                SteamRollerManState.IDLE, SteamRollerManState.SMASH -> body.physics.velocity.setZero()
                SteamRollerManState.ROLL -> {
                    if ((isFacing(Facing.LEFT) && body.physics.velocity.x > -MAX_ROLL_SPEED * ConstVals.PPM) ||
                        (isFacing(Facing.RIGHT) && body.physics.velocity.x < MAX_ROLL_SPEED * ConstVals.PPM)
                    ) body.physics.velocity.x += ROLL_IMPULSE * ConstVals.PPM * facing.value * delta

                    if (shouldStopRolling()) stateMachine.next()
                }
                SteamRollerManState.REVERSE -> {
                    body.physics.velocity.x = -REVERSE_SPEED * ConstVals.PPM * facing.value

                    if (shouldStopReversing()) stateMachine.next()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(4.75f * ConstVals.PPM, 4f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(4.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.25f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.set(-body.getWidth() / 2f, -0.5f * ConstVals.PPM)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.25f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.set(body.getWidth() / 2f, -0.5f * ConstVals.PPM)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val damagerFixture1 = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture1)
        damagerFixture1.drawingColor = Color.RED
        debugShapes.add { damagerFixture1 }

        val damagerFixture2 = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(2f * ConstVals.PPM, 1.75f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture2)
        damagerFixture2.drawingColor = Color.RED
        debugShapes.add { damagerFixture2 }

        val damagerFixture3 = Fixture(body, FixtureType.DAMAGER, smashArea)
        damagerFixture3.attachedToBody = false
        body.addFixture(damagerFixture3)

        shieldFixture1 = Fixture(
            body,
            FixtureType.SHIELD,
            GameRectangle().setSize(2f * ConstVals.PPM, 1.75f * ConstVals.PPM)
        )
        body.addFixture(shieldFixture1)
        shieldFixture1.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture1 }

        val shieldFixture2 = Fixture(body, FixtureType.SHIELD, smashArea)
        shieldFixture2.attachedToBody = false
        body.addFixture(shieldFixture2)

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(ConstVals.PPM.toFloat(), 1.25f * ConstVals.PPM)
        )
        body.addFixture(damageableFixture)
        damageableFixture.drawingColor = Color.PURPLE
        debugShapes.add { damageableFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAV else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

            damageableFixture.offsetFromBodyAttachment.set(
                1f * ConstVals.PPM * -facing.value,
                0.35f * ConstVals.PPM
            )

            damagerFixture1.offsetFromBodyAttachment.set(
                1f * ConstVals.PPM * -facing.value,
                0.35f * ConstVals.PPM
            )

            damagerFixture2.offsetFromBodyAttachment.set(
                1.25f * ConstVals.PPM * -facing.value,
                -1.25f * ConstVals.PPM
            )

            shieldFixture1.offsetFromBodyAttachment.set(
                1.25f * ConstVals.PPM * -facing.value,
                -1.25f * ConstVals.PPM
            )
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(7f * ConstVals.PPM, 6f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.LEFT), false)
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(0.75f * ConstVals.PPM * -facing.value)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<SteamRollerManState>()
        .initialState(SteamRollerManState.IDLE)
        .onChangeState(this::onChangeState)
        // idle
        .transition(SteamRollerManState.IDLE, SteamRollerManState.REVERSE) { FacingUtils.isFacingBlock(this) }
        .transition(SteamRollerManState.IDLE, SteamRollerManState.ROLL) { true }
        // reverse
        .transition(SteamRollerManState.REVERSE, SteamRollerManState.IDLE) { true }
        // roll
        .transition(SteamRollerManState.ROLL, SteamRollerManState.IDLE) { FacingUtils.isFacingBlock(this) }
        .transition(SteamRollerManState.ROLL, SteamRollerManState.SMASH) { canSmash() }
        .transition(SteamRollerManState.ROLL, SteamRollerManState.REVERSE) { true }
        // smash
        .transition(SteamRollerManState.SMASH, SteamRollerManState.REVERSE) { FacingUtils.isFacingBlock(this) }
        .transition(SteamRollerManState.SMASH, SteamRollerManState.IDLE) { true }
        // build
        .build()

    private fun onChangeState(current: SteamRollerManState, previous: SteamRollerManState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")
        stateTimers[current]?.reset()
    }

    private fun canSmash() = MegaGameEntities.getOfType(EntityType.BLOCK).none {
        (it as IBodyEntity).body.getBounds().overlaps(smashArea)
    }

    private fun shouldStopRolling() = FacingUtils.isFacingBlock(this) ||
        (canSmash() && megaman.body.getBounds().overlaps(smashArea))

    private fun shouldStopReversing(): Boolean {
        val sense = when (facing) {
            Facing.LEFT -> BodySense.SIDE_TOUCHING_BLOCK_RIGHT
            else -> BodySense.SIDE_TOUCHING_BLOCK_LEFT
        }
        return body.isSensing(sense)
    }

    private fun setSmashAreaPosition() = when {
        currentState == SteamRollerManState.SMASH && currentStateTimer!!.time < 0.1f ->
            smashArea.setCenter(body.getCenter().add(2.75f * ConstVals.PPM * facing.value, -0.25f * ConstVals.PPM))
        currentState == SteamRollerManState.SMASH && currentStateTimer!!.time < 0.3f ->
            smashArea.setCenter(body.getCenter().add(1.75f * ConstVals.PPM * facing.value, ConstVals.PPM.toFloat()))
        else -> smashArea.setBottomCenterToPoint(
            body.getPositionPoint(Position.BOTTOM_CENTER).add(
                SMASH_AREA_OFFSET_X * ConstVals.PPM * facing.value,
                SMASH_AREA_OFFSET_Y * ConstVals.PPM
            )
        )
    }

    private fun smash() {
        GameLogger.debug(TAG, "smash()")

        (0 until SMASH_ROCKS).forEach {
            val spawn = smashArea.getCenter().add(
                UtilMethods.getRandom(
                    SMASH_ROCKS_MIN_OFFSET_X,
                    SMASH_ROCKS_MAX_OFFSET_X
                ) * ConstVals.PPM * facing.value,
                UtilMethods.getRandom(SMASH_ROCKS_MIN_OFFSET_Y, SMASH_ROCKS_MAX_OFFSET_Y) * ConstVals.PPM
            )

            val size = if (it % 2 == 0) RockSize.BIG else RockSize.SMALL

            val impulse = GameObjectPools.fetch(Vector2::class)
                .setX(UtilMethods.getRandom(SMASH_ROCKS_MIN_X_IMPULSE, SMASH_ROCKS_MAX_X_IMPULSE) * ConstVals.PPM)
                .setY(UtilMethods.getRandom(SMASH_ROCKS_MIN_Y_IMPULSE, SMASH_ROCKS_MAX_Y_IMPULSE) * ConstVals.PPM)

            val rock = MegaEntityFactory.fetch(Rock::class)!!
            rock.spawn(
                props(
                    ConstKeys.SIZE pairTo size,
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.IMPULSE pairTo impulse
                )
            )

            val explosion = MegaEntityFactory.fetch(AsteroidExplosion::class)!!
            explosion.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.SOUND pairTo false,
                    ConstKeys.POSITION pairTo spawn
                )
            )
        }

        requestToPlaySound(SoundAsset.ASTEROID_EXPLODE_SOUND, false)
    }
}
