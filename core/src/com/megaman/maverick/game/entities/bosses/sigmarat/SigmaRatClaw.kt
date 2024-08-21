package com.megaman.maverick.game.entities.bosses.sigmarat

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IChildEntity
import com.engine.motion.RotatingLine
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.SigmaRatElectricBall
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodyLabel
import com.megaman.maverick.game.world.FixtureLabel
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

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

    enum class SigmaRatClawState {
        ROTATE, SHOCK, LAUNCH, TITTY_GRAB
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    override var parent: IGameEntity? = null

    lateinit var state: SigmaRatClawState
        private set

    val shocking: Boolean
        get() = state == SigmaRatClawState.SHOCK
    val launched: Boolean
        get() = state == SigmaRatClawState.LAUNCH

    private val launchPauseTimer = Timer(LAUNCH_PAUSE_DUR)
    private val shockPauseTimer = Timer(SHOCK_PAUSE_DUR)

    private lateinit var rotatingLine: RotatingLine
    private lateinit var launchTarget: Vector2
    private lateinit var returnTarget: Vector2

    private var block: Block? = null
    private var shockBall: SigmaRatElectricBall? = null
    private var shocked = false
    private var reachedLaunchTarget = false
    private var maxY = 0f

    override fun init() {
        if (closedRegion == null || openRegion == null || shockRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            closedRegion = atlas.findRegion("SigmaRat/ClawClosed")
            openRegion = atlas.findRegion("SigmaRat/ClawOpen")
            shockRegion = atlas.findRegion("SigmaRat/ClawFlash")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        parent = spawnProps.get(ConstKeys.PARENT, IGameEntity::class)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!
        rotatingLine = RotatingLine(spawn, ConstVals.PPM.toFloat(), speed * ConstVals.PPM, DEGREES_ON_RESET)
        body.setCenter(rotatingLine.getMotionValue())

        block = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        game.engine.spawn(
            block!!, props(
                ConstKeys.BOUNDS to GameRectangle().setSize(1.35f * ConstVals.PPM, 0.1f * ConstVals.PPM)
                    .setTopCenterToPoint(body.getTopCenterPoint()),
                ConstKeys.BODY_LABELS to objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY),
                ConstKeys.FIXTURE_LABELS to objectSetOf(
                    FixtureLabel.NO_PROJECTILE_COLLISION, FixtureLabel.NO_SIDE_TOUCHIE
                )
            )
        )

        state = SigmaRatClawState.ROTATE
        maxY = spawnProps.get(ConstKeys.MAX_Y, Float::class)!!
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        block?.kill()
        block = null
    }

    internal fun enterLaunchState() {
        state = SigmaRatClawState.LAUNCH
        launchPauseTimer.reset()
        reachedLaunchTarget = false
        launchTarget = getMegaman().body.getCenter()
        GameLogger.debug(TAG, "Launch target: $launchTarget")
        returnTarget = body.getCenter()
        GameLogger.debug(TAG, "Return target: $returnTarget")
    }

    internal fun enterShockState() {
        state = SigmaRatClawState.SHOCK
        shockPauseTimer.reset()
        shocked = false
        shockBall = EntityFactories.fetch(
            EntityType.PROJECTILE, ProjectilesFactory.SIGMA_RAT_ELECTRIC_BALL
        ) as SigmaRatElectricBall
        game.engine.spawn(
            shockBall!!, props(
                ConstKeys.OWNER to this, ConstKeys.POSITION to body.getCenter().sub(0f, 0.15f * ConstVals.PPM)
            )
        )
    }

    private fun shock() {
        val shocks = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.BOLT, 2)
        game.engine.spawn(
            shocks[0], props(
                ConstKeys.PARENT to this,
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.DIRECTION to Direction.UP,
                ConstKeys.TRAJECTORY to Vector2(0f, SHOCK_VELOCITY_Y * ConstVals.PPM),
                ConstKeys.SCALE to SHOCK_BOLT_SCALE
            )
        )
        game.engine.spawn(
            shocks[1], props(
                ConstKeys.PARENT to this,
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.DIRECTION to Direction.DOWN,
                ConstKeys.TRAJECTORY to Vector2(0f, -SHOCK_VELOCITY_Y * ConstVals.PPM),
                ConstKeys.SCALE to SHOCK_BOLT_SCALE
            )
        )
        requestToPlaySound(SoundAsset.BURST_SOUND, false)

        shockBall!!.launch(
            getMegaman().body.getCenter().sub(body.getCenter()).nor().scl(SHOCK_VELOCITY_Y * ConstVals.PPM)
        )
        shockBall = null
        requestToPlaySound(SoundAsset.BLAST_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                SigmaRatClawState.ROTATE -> {
                    rotatingLine.update(delta)
                    body.setCenter(rotatingLine.getMotionValue())
                }

                SigmaRatClawState.SHOCK -> {
                    shockPauseTimer.update(delta)
                    if (shockPauseTimer.isJustFinished()) {
                        if (shocked) state = SigmaRatClawState.ROTATE
                        else {
                            shock()
                            shocked = true
                            shockPauseTimer.reset()
                        }
                    }
                }

                SigmaRatClawState.LAUNCH -> {
                    launchPauseTimer.update(delta)
                    if (!launchPauseTimer.isFinished()) body.physics.velocity.setZero()
                    else if (reachedLaunchTarget) {
                        val trajectory =
                            returnTarget.cpy().sub(body.getCenter()).nor().scl(RETURN_SPEED * ConstVals.PPM)
                        body.physics.velocity = trajectory
                        if (body.getCenter().epsilonEquals(returnTarget, EPSILON * ConstVals.PPM)) {
                            state = SigmaRatClawState.ROTATE
                            body.physics.velocity.setZero()
                        }
                    } else {
                        val trajectory =
                            launchTarget.cpy().sub(body.getCenter()).nor().scl(LAUNCH_SPEED * ConstVals.PPM)
                        body.physics.velocity = trajectory
                        if (body.getCenter().epsilonEquals(launchTarget, EPSILON * ConstVals.PPM) ||
                            getMegaman().body.contains(body.getCenter()) ||
                            body.getMaxY() >= maxY
                        ) {
                            launchPauseTimer.reset()
                            reachedLaunchTarget = true
                            body.physics.velocity.setZero()
                        }
                    }

                }

                SigmaRatClawState.TITTY_GRAB -> {

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
        bodyFixture.rawShape.color = Color.YELLOW
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                1.5f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        damagerFixture.offsetFromBodyCenter.y = -0.35f * ConstVals.PPM
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture.getShape() }

        /*
        TODO: should this have a shield fixture?
        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().set(body))
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture.getShape() }
         */

        body.preProcess.put(ConstKeys.DEFAULT) { delta ->
            val target = body.getTopCenterPoint().sub(0f, 0.1f * ConstVals.PPM)
            val current = block!!.body.getTopCenterPoint()
            val diff = target.sub(current)
            block!!.body.physics.velocity = diff.scl(1f / delta)

            val swiping = state == SigmaRatClawState.LAUNCH/*
            TODO: shield fixture?
            shieldFixture.active = !swiping
            shieldFixture.rawShape.color = if (!swiping) Color.BLUE else Color.GRAY
             */
            damageableFixture.active = swiping
            damageableFixture.rawShape.color = if (swiping) Color.PURPLE else Color.GRAY
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getTopCenterPoint(), Position.TOP_CENTER)
            _sprite.translateY(0.35f * ConstVals.PPM)
            _sprite.hidden = damageBlink || !(parent as SigmaRat).ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (state) {
                SigmaRatClawState.ROTATE, SigmaRatClawState.TITTY_GRAB -> "closed"
                SigmaRatClawState.LAUNCH -> "open"
                SigmaRatClawState.SHOCK -> if (shocked) "open" else "shock"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "closed" to Animation(closedRegion!!),
            "open" to Animation(openRegion!!),
            "shock" to Animation(shockRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}