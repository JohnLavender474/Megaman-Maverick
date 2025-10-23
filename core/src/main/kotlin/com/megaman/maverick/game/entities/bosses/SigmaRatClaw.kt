package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IChildEntity
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.SigmaRatElectricBall
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.world.body.*

class SigmaRatClaw(game: MegamanMaverickGame) : AbstractEnemy(game), IChildEntity, IAnimatedEntity {

    companion object {
        const val TAG = "SigmaRatClaw"
        private const val DEGREES_ON_RESET = 90f
        private const val LAUNCH_PAUSE_DUR = 0.75f
        private const val RETURN_SPEED = 5f
        private const val LAUNCH_SPEED = 10f
        private const val SHOCK_PAUSE_DUR = 0.75f
        private const val SHOCK_BOLT_SCALE = 2.5f
        private const val SHOCK_VELOCITY_Y = 10f
        private const val EPSILON = 0.1f
        private var closedRegion: TextureRegion? = null
        private var openRegion: TextureRegion? = null
        private var shockRegion: TextureRegion? = null
    }

    enum class SigmaRatClawState { ROTATE, SHOCK, LAUNCH, TITTY_GRAB }

    override var parent: IGameEntity? = null
    // rat claw cannot be damaged
    override var invincible = true

    lateinit var clawState: SigmaRatClawState
        private set
    val shocking: Boolean
        get() = clawState == SigmaRatClawState.SHOCK
    val launched: Boolean
        get() = clawState == SigmaRatClawState.LAUNCH

    private val launchPauseTimer = Timer(LAUNCH_PAUSE_DUR)
    private val shockPauseTimer = Timer(SHOCK_PAUSE_DUR)

    private lateinit var rotatingLine: RotatingLine

    private val launchTarget = Vector2()
    private val returnTarget = Vector2()

    private var block: Block? = null
    private var shockBall: SigmaRatElectricBall? = null

    private var shocked = false
    private var reachedLaunchTarget = false

    private var maxY = 0f

    override fun init() {
        if (closedRegion == null || openRegion == null || shockRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            closedRegion = atlas.findRegion("SigmaRat/ClawClosed")
            openRegion = atlas.findRegion("SigmaRat/ClawOpen")
            shockRegion = atlas.findRegion("SigmaRat/ClawFlash")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        parent = spawnProps.get(ConstKeys.PARENT, GameEntity::class)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!

        rotatingLine = RotatingLine(spawn, ConstVals.PPM.toFloat(), speed * ConstVals.PPM, DEGREES_ON_RESET)

        val center = rotatingLine.getMotionValue()!!
        body.setCenter(center.x, center.y)

        block = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        block!!.spawn(
            props(
                ConstKeys.BOUNDS pairTo GameRectangle().setSize(1.35f * ConstVals.PPM, 0.1f * ConstVals.PPM)
                    .setTopCenterToPoint(body.getPositionPoint(Position.TOP_CENTER)),
                ConstKeys.BODY_LABELS pairTo objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY),
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                    FixtureLabel.NO_PROJECTILE_COLLISION, FixtureLabel.NO_SIDE_TOUCHIE
                )
            )
        )

        clawState = SigmaRatClawState.ROTATE
        maxY = spawnProps.get(ConstKeys.MAX_Y, Float::class)!!
    }

    override fun onDestroy() {
        super.onDestroy()
        block?.destroy()
        block = null
    }

    internal fun enterLaunchState() {
        clawState = SigmaRatClawState.LAUNCH
        launchPauseTimer.reset()
        reachedLaunchTarget = false
        launchTarget.set(megaman.body.getCenter())
        GameLogger.debug(TAG, "Launch target: $launchTarget")
        returnTarget.set(body.getCenter())
        GameLogger.debug(TAG, "Return target: $returnTarget")
    }

    internal fun enterShockState() {
        clawState = SigmaRatClawState.SHOCK

        shockPauseTimer.reset()
        shocked = false

        shockBall = EntityFactories.fetch(
            EntityType.PROJECTILE, ProjectilesFactory.SIGMA_RAT_ELECTRIC_BALL
        ) as SigmaRatElectricBall

        shockBall!!.spawn(
            props(
                ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo body.getCenter().sub(0f, 0.15f * ConstVals.PPM)
            )
        )
    }

    private fun shock() {
        val shocks = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.BOLT, 2)

        shocks[0].spawn(
            props(
                ConstKeys.PARENT pairTo this,
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.DIRECTION pairTo Direction.UP,
                ConstKeys.TRAJECTORY pairTo Vector2(0f, SHOCK_VELOCITY_Y * ConstVals.PPM),
                ConstKeys.SCALE pairTo SHOCK_BOLT_SCALE
            )
        )

        shocks[1].spawn(
            props(
                ConstKeys.PARENT pairTo this,
                ConstKeys.POSITION pairTo body.getCenter(),
                ConstKeys.DIRECTION pairTo Direction.DOWN,
                ConstKeys.TRAJECTORY pairTo Vector2(0f, -SHOCK_VELOCITY_Y * ConstVals.PPM),
                ConstKeys.SCALE pairTo SHOCK_BOLT_SCALE
            )
        )

        requestToPlaySound(SoundAsset.BURST_SOUND, false)

        shockBall!!.launch(
            megaman.body.getCenter().sub(body.getCenter()).nor().scl(SHOCK_VELOCITY_Y * ConstVals.PPM)
        )
        shockBall = null

        requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (clawState) {
                SigmaRatClawState.ROTATE -> {
                    rotatingLine.update(delta)
                    val center = rotatingLine.getMotionValue()!!
                    body.setCenter(center.x, center.y)
                }

                SigmaRatClawState.SHOCK -> {
                    shockPauseTimer.update(delta)
                    if (shockPauseTimer.isJustFinished()) {
                        if (shocked) clawState = SigmaRatClawState.ROTATE
                        else {
                            shock()
                            shocked = true
                            shockPauseTimer.reset()
                        }
                    }
                }

                SigmaRatClawState.LAUNCH -> {
                    launchPauseTimer.update(delta)
                    when {
                        !launchPauseTimer.isFinished() -> body.physics.velocity.setZero()
                        reachedLaunchTarget -> {
                            val trajectory = GameObjectPools.fetch(Vector2::class)
                                .set(returnTarget)
                                .sub(body.getCenter())
                                .nor()
                                .scl(RETURN_SPEED * ConstVals.PPM)
                            body.physics.velocity.set(trajectory)

                            if (body.getCenter().epsilonEquals(returnTarget, EPSILON * ConstVals.PPM)) {
                                clawState = SigmaRatClawState.ROTATE
                                body.physics.velocity.setZero()
                            }
                        }

                        else -> {
                            val trajectory = GameObjectPools.fetch(Vector2::class)
                                .set(launchTarget)
                                .sub(body.getCenter())
                                .nor()
                                .scl(LAUNCH_SPEED * ConstVals.PPM)
                            body.physics.velocity.set(trajectory)

                            if (body.getCenter().epsilonEquals(launchTarget, EPSILON * ConstVals.PPM) ||
                                megaman.body.getBounds().contains(body.getCenter()) ||
                                body.getMaxY() >= maxY
                            ) {
                                launchPauseTimer.reset()
                                reachedLaunchTarget = true
                                body.physics.velocity.setZero()
                            }
                        }
                    }

                }

                SigmaRatClawState.TITTY_GRAB -> {
                    // TODO
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.85f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                1.5f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        damagerFixture.offsetFromBodyAttachment.y = -0.35f * ConstVals.PPM
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        /*
        TODO: should this have a shield fixture?
        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().set(body))
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture}
         */

        body.preProcess.put(ConstKeys.DEFAULT) {
            val velocity = GameObjectPools.fetch(Vector2::class)
                .set(body.getPositionPoint(Position.TOP_CENTER).sub(0f, 0.1f * ConstVals.PPM))
                .sub(block!!.body.getPositionPoint(Position.TOP_CENTER))
                .scl(1f / ConstVals.FIXED_TIME_STEP)
            block!!.body.physics.velocity.set(velocity)

            val swiping = clawState == SigmaRatClawState.LAUNCH
            /*
            TODO: shield fixture?
                shieldFixture.active = !swiping
                shieldFixture.drawingColor = if (!swiping) Color.BLUE else Color.GRAY
             */
            damageableFixture.setActive(swiping)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.TOP_CENTER), Position.TOP_CENTER)
            sprite.translateY(0.35f * ConstVals.PPM)
            sprite.hidden = damageBlink || !(parent as SigmaRat).ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when (clawState) {
                SigmaRatClawState.ROTATE, SigmaRatClawState.TITTY_GRAB -> "closed"
                SigmaRatClawState.LAUNCH -> "open"
                SigmaRatClawState.SHOCK -> if (shocked) "open" else "shock"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "closed" pairTo Animation(closedRegion!!),
            "open" pairTo Animation(openRegion!!),
            "shock" pairTo Animation(shockRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
