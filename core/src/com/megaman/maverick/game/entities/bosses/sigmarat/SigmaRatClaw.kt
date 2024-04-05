package com.megaman.maverick.game.entities.bosses.sigmarat

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodyLabel
import com.megaman.maverick.game.world.FixtureLabel
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SigmaRatClaw(game: MegamanMaverickGame) : AbstractEnemy(game), IChildEntity, IAnimatedEntity {

    companion object {
        const val TAG = "SigmaRatClaw"
        private const val DEGREES_ON_RESET = 90f
        private const val LAUNCH_PAUSE_DUR = 0.5f
        private const val SHOCK_PAUSE_DUR = 0.25f
        private const val TITTY_GRAB_PAUSE_DUR = 0.5f
        private var restRegion: TextureRegion? = null
        private var shockRegion: TextureRegion? = null
        private var swipeRegion: TextureRegion? = null
    }

    enum class SigmaRatClawState {
        ROTATE, SHOCK, LAUNCH, TITTY_GRAB
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    override var parent: IGameEntity? = null

    private val launchPauseTimer = Timer(LAUNCH_PAUSE_DUR)
    private val shockPauseTimer = Timer(SHOCK_PAUSE_DUR)
    private val tittyGrabPauseTimer = Timer(TITTY_GRAB_PAUSE_DUR)

    private lateinit var state: SigmaRatClawState
    private lateinit var rotatingLine: RotatingLine

    private var block: Block? = null

    override fun init() {
        if (restRegion == null || shockRegion == null || swipeRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            restRegion = atlas.findRegion("SigmaRat/ClawClosed")
            shockRegion = atlas.findRegion("SigmaRat/ClawOpen")
            swipeRegion = atlas.findRegion("SigmaRat/ClawFlash")
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
                ConstKeys.BOUNDS to GameRectangle().setSize(body.width, 0.15f * ConstVals.PPM)
                    .setTopCenterToPoint(body.getTopCenterPoint()),
                ConstKeys.BODY_LABELS to objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY),
                ConstKeys.FIXTURE_LABELS to objectSetOf(
                    FixtureLabel.NO_PROJECTILE_COLLISION, FixtureLabel.NO_SIDE_TOUCHIE
                )
            )
        )

        state = SigmaRatClawState.ROTATE
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        block?.kill()
        block = null
    }

    internal fun launch() {
        // TODO:
        //  - stop rotating and pause for x seconds
        //  - launch fast towards Megaman's position
        //  - once position is reached, stop and pause for x seconds
        //  - slowly move back to original position
        //  - once position is reached, resume rotating

        launchPauseTimer.reset()
        state = SigmaRatClawState.LAUNCH
    }

    internal fun shock() {
        // TODO:
        //  - open claw and hold for x seconds
        //  - shoot shock bolts up and down
        //  - hold claw at position for x seconds
        //  - close claw
        //  - resume rotating

        shockPauseTimer.reset()
        state = SigmaRatClawState.SHOCK
    }

    internal fun tittyGrab() {
        // TODO:
        //  - open claw and hold for x seconds
        //  - move claw to SigmaRat's titty position (passed as spawn prop)
        //  - when both this claw and the other return true for 'isTittyGrabbed', then SigmaRat will perform titty
        //    shock attack
        //  - when titty shock attack is done, move claw to starting position in rotation
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

                }

                SigmaRatClawState.LAUNCH -> {

                }

                SigmaRatClawState.TITTY_GRAB -> {

                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM, 0.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        body.color = Color.YELLOW
        debugShapes.add { body }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                ConstVals.PPM.toFloat(), 0.35f * ConstVals.PPM
            )
        )
        damagerFixture.offsetFromBodyCenter.y = -0.35f * ConstVals.PPM
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture.getShape() }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().set(body))
        body.addFixture(shieldFixture)
        debugShapes.add { shieldFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) { delta ->
            val target = body.getTopCenterPoint().sub(0f, 0.075f * ConstVals.PPM)
            val current = block!!.body.getTopCenterPoint()
            val diff = target.sub(current)
            block!!.body.physics.velocity = diff.scl(1f / delta)

            val swiping = state == SigmaRatClawState.LAUNCH
            shieldFixture.active = !swiping
            shieldFixture.rawShape.color = if (!swiping) Color.BLUE else Color.GRAY
            damageableFixture.active = swiping
            damageableFixture.rawShape.color = if (swiping) Color.PURPLE else Color.GRAY
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(1.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getTopCenterPoint(), Position.TOP_CENTER)
            _sprite.translateY(0.25f * ConstVals.PPM)
            _sprite.hidden = damageBlink || !(parent as SigmaRat).ready
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (state) {
                SigmaRatClawState.ROTATE, SigmaRatClawState.TITTY_GRAB -> "rest"
                SigmaRatClawState.SHOCK -> "shock"
                SigmaRatClawState.LAUNCH -> "swipe"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "rest" to Animation(restRegion!!),
            "shock" to Animation(shockRegion!!),
            "swipe" to Animation(swipeRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}