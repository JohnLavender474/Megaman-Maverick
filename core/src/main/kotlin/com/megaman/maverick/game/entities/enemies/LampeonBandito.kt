package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
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
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.LampeonBullet
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class LampeonBandito(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity,
    IFaceable {

    companion object {
        const val TAG = "LampeonBandito"

        private const val SPRAY = "spray"

        private const val BULLETS = 5
        private const val BULLET_SPEED = 12f

        private const val GRAVITY = -0.25f
        private const val GROUND_GRAVITY = -0.01f

        private const val SHOOT_DUR = 0.5f
        private const val SHOOT_DELAY = 1f
        private const val SHOOT_TIME = 0.1f

        private const val STAND_SHOOT_SCANNER_WIDTH = 10f
        private const val STAND_SHOOT_SCANNER_HEIGHT = 2f

        private const val JUMP_SHOOT_SCANNER_WIDTH = 10f
        private const val JUMP_SHOOT_SCANNER_HEIGHT = 1f
        private const val JUMP_SHOOT_SCANNER_OFFSET_Y = 0.25f

        private const val JUMP_HIT_SCANNER_WIDTH = 1f
        private const val JUMP_HIT_SCANNER_HEIGHT = 2f

        private const val JUMP_DELAY = 1f

        private const val JUMP_IMPULSE = 15f

        private const val FRICTION_Y = 1.05f

        private val animDefs = orderedMapOf(
            "stand" pairTo AnimationDef(2, 1, gdxArrayOf(0.9f, 0.1f), true),
            "stand_shoot" pairTo AnimationDef(2, 2, 0.1f, false),
            "jump" pairTo AnimationDef(),
            "jump_shoot" pairTo AnimationDef(2, 2, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class LampeonBanditoState { STAND, STAND_SHOOT, JUMP, JUMP_SHOOT }

    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<LampeonBanditoState>
    private val currentState: LampeonBanditoState
        get() = stateMachine.getCurrentElement()

    private val shootDelay = Timer(SHOOT_DELAY)
    private val shootTimer = Timer(SHOOT_DUR)
        .addRunnable(TimeMarkedRunnable(SHOOT_TIME) { shoot() })
    private val shooting: Boolean
        get() = !shootTimer.isFinished()

    private var spray = true

    private val jumpDelay = Timer(JUMP_DELAY)
    private val jumping: Boolean
        get() = currentState.equalsAny(LampeonBanditoState.JUMP, LampeonBanditoState.JUMP_SHOOT)

    private val standShootScanner = GameRectangle().setSize(
        STAND_SHOOT_SCANNER_WIDTH * ConstVals.PPM,
        STAND_SHOOT_SCANNER_HEIGHT * ConstVals.PPM
    )

    private val jumpShootScanner = GameRectangle().setSize(
        JUMP_SHOOT_SCANNER_WIDTH * ConstVals.PPM,
        JUMP_SHOOT_SCANNER_HEIGHT * ConstVals.PPM
    )

    private val jumpHitScanner = GameRectangle().setSize(
        JUMP_HIT_SCANNER_WIDTH * ConstVals.PPM,
        JUMP_HIT_SCANNER_HEIGHT * ConstVals.PPM
    )

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        stateMachine = buildStateMachine()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { standShootScanner }
        addDebugShapeSupplier { jumpShootScanner }
        addDebugShapeSupplier { jumpHitScanner }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        stateMachine.reset()

        spray = spawnProps.getOrDefault(SPRAY, true, Boolean::class)

        shootTimer.setToEnd()
        shootDelay.setToEnd()

        jumpDelay.setToEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!shooting) {
                shootDelay.update(delta)
                FacingUtils.setFacingOf(this)
            } else shootTimer.update(delta)

            if (!jumping) jumpDelay.update(delta)

            updateScannerPositions()

            when (currentState) {
                LampeonBanditoState.STAND -> if (canShoot() || shouldJump()) stateMachine.next()
                LampeonBanditoState.STAND_SHOOT -> if (shootTimer.isFinished()) stateMachine.next()
                else -> if (shouldEndJump() || (!shooting && shouldShootWhileJumping())) stateMachine.next()
            }
        }
    }

    private fun updateScannerPositions() {
        val position = if (isFacing(Facing.LEFT)) Position.CENTER_RIGHT else Position.CENTER_LEFT

        standShootScanner.positionOnPoint(body.getBounds().getCenter(), position)

        jumpShootScanner.positionOnPoint(
            body.getBounds()
                .getPositionPoint(Position.TOP_CENTER)
                .add(0f, JUMP_SHOOT_SCANNER_OFFSET_Y * ConstVals.PPM),
            position
        )

        jumpHitScanner.setBottomCenterToPoint(body.getBounds().getPositionPoint(Position.TOP_CENTER))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = true
        body.physics.defaultFrictionOnSelf.y = FRICTION_Y

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.GRAVITY) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(4f * ConstVals.PPM, 3f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
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

    private fun buildStateMachine() = EnumStateMachineBuilder.create<LampeonBanditoState>()
        .onChangeState(this::onChangeState)
        .initialState(LampeonBanditoState.STAND)
        // stand
        .transition(LampeonBanditoState.STAND, LampeonBanditoState.STAND_SHOOT) { canShoot() }
        .transition(LampeonBanditoState.STAND, LampeonBanditoState.JUMP) { shouldJump() }
        // stand-shoot
        .transition(LampeonBanditoState.STAND_SHOOT, LampeonBanditoState.STAND) { true }
        // jump
        .transition(LampeonBanditoState.JUMP, LampeonBanditoState.JUMP_SHOOT) { shouldShootWhileJumping() }
        .transition(LampeonBanditoState.JUMP, LampeonBanditoState.STAND) { true }
        // jump-shoot
        .transition(LampeonBanditoState.JUMP_SHOOT, LampeonBanditoState.STAND) { true }
        // build
        .build()

    private fun onChangeState(current: LampeonBanditoState, previous: LampeonBanditoState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        when (current) {
            LampeonBanditoState.STAND_SHOOT, LampeonBanditoState.JUMP_SHOOT -> {
                shootTimer.reset()
                shootDelay.reset()
            }
            LampeonBanditoState.JUMP -> {
                jump()
                jumpDelay.reset()
            }
            else -> {}
        }
    }

    private fun canShoot() = shootDelay.isFinished() && megaman.body.getBounds().overlaps(standShootScanner)

    private fun shouldJump() = jumpDelay.isFinished() &&
        ((canShoot() && megaman.body.getBounds().overlaps(jumpShootScanner)) ||
            megaman.body.getBounds().overlaps(jumpHitScanner))

    private fun shouldShootWhileJumping() =
        body.physics.velocity.y <= 0f && megaman.body.getBounds().overlaps(jumpHitScanner)

    private fun shouldEndJump() = body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND)

    private fun jump() {
        body.physics.velocity.y = JUMP_IMPULSE * ConstVals.PPM
    }

    private fun shoot() {
        val position = body.getCenter().add(1.5f * ConstVals.PPM * facing.value, 0.25f * ConstVals.PPM)

        if (spray) for (index in 0 until BULLETS) {
            val bullet = MegaEntityFactory.fetch(LampeonBullet::class)!!
            bullet.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.INDEX pairTo index,
                    ConstKeys.FACING pairTo facing,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.SPEED pairTo BULLET_SPEED * ConstVals.PPM
                )
            )
        } else {
            val bullet = MegaEntityFactory.fetch(LampeonBullet::class)!!
            bullet.spawn(
                props(
                    ConstKeys.INDEX pairTo 2,
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.FACING pairTo facing,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.SPEED pairTo BULLET_SPEED * ConstVals.PPM
                )
            )
        }

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)
    }
}
