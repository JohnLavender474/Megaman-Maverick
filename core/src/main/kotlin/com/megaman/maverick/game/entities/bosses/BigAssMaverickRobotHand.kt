package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.world.body.*

class BigAssMaverickRobotHand(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IDamager,
    IHazard, IOwnable {

    companion object {
        const val TAG = "BigAssMaverickRobotHand"

        private const val BODY_SIZE = 2f

        private const val FIXTURE_RADIUS = 1.5f

        private const val BLOCK_WIDTH = 1.5f
        private const val BLOCK_HEIGHT = 0.25f
        private val BLOCK_POSITION = Position.TOP_CENTER

        private const val LAUNCH_DELAY = 1f
        private const val LAUNCH_SPEED = 8f

        private const val RETURN_DELAY = 1f
        private const val RETURN_SPEED = 4f
    }

    enum class BigAddMaverickRobotHandState { ROTATE, LAUNCH, RETURN }

    override var owner: IGameEntity? = null

    lateinit var state: BigAddMaverickRobotHandState
        private set

    private lateinit var rotatingLine: RotatingLine
    private var block: Block? = null

    private val launchDelay = Timer(LAUNCH_DELAY)
    private val launchTarget = Vector2()

    private val returnDelay = Timer(RETURN_DELAY)
    private val returnTarget = Vector2()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, BigAssMaverickRobot::class)!!

        val origin = spawnProps.get(ConstKeys.ORIGIN, Vector2::class)!!
        val radius = spawnProps.get(ConstKeys.RADIUS, Float::class)!!
        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!
        rotatingLine = RotatingLine(origin.cpy(), radius, speed)

        state = BigAddMaverickRobotHandState.ROTATE

        launchDelay.reset()
        returnDelay.reset()

        val block = MegaEntityFactory.fetch(Block::class)!!
        block.spawn(
            props(
                ConstKeys.BLOCK_FILTERS pairTo TAG,
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                    FixtureLabel.NO_SIDE_TOUCHIE,
                    FixtureLabel.NO_PROJECTILE_COLLISION
                ),
                ConstKeys.WIDTH pairTo BLOCK_WIDTH * ConstVals.PPM,
                "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false,
                ConstKeys.HEIGHT pairTo BLOCK_HEIGHT * ConstVals.PPM,
                ConstKeys.BODY_LABELS pairTo objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY),
            )
        )
        this.block = block
    }

    internal fun launch() {
        state = BigAddMaverickRobotHandState.LAUNCH

        launchTarget.set(megaman.body.getCenter())
        launchDelay.reset()

        returnTarget.set(body.getCenter())
    }

    internal fun isBeingStoodUpon() = megaman.feetFixture.getShape().overlaps(block!!.body.getBounds())

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        block?.destroy()
        block = null
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (state) {
            BigAddMaverickRobotHandState.ROTATE -> {
                rotatingLine.update(delta)
                val center = rotatingLine.getMotionValue()!!
                body.setCenter(center.x, center.y)
            }

            BigAddMaverickRobotHandState.LAUNCH -> {
                launchDelay.update(delta)
                if (!launchDelay.isFinished()) {
                    body.physics.velocity.setZero()
                    return@UpdatablesComponent
                }

                if (launchDelay.isJustFinished()) block!!.body.let { blockBody ->
                    blockBody.physics.collisionOn = false
                    blockBody.forEachFixture { fixture -> fixture.setActive(false) }
                }

                body.physics.velocity.set(launchTarget).sub(body.getCenter()).nor().scl(LAUNCH_SPEED * ConstVals.PPM)

                if (body.getCenter().epsilonEquals(launchTarget, 0.1f * ConstVals.PPM)) {
                    state = BigAddMaverickRobotHandState.RETURN

                    returnDelay.reset()
                }
            }

            BigAddMaverickRobotHandState.RETURN -> {
                returnDelay.update(delta)
                if (!returnDelay.isFinished()) {
                    body.physics.velocity.setZero()
                    return@UpdatablesComponent
                }

                body.physics.velocity.set(returnTarget).sub(body.getCenter()).nor().scl(RETURN_SPEED * ConstVals.PPM)

                if (body.getCenter().epsilonEquals(returnTarget, 0.1f * ConstVals.PPM)) {
                    state = BigAddMaverickRobotHandState.ROTATE

                    block!!.body.let { blockBody ->
                        blockBody.physics.collisionOn = true
                        blockBody.forEachFixture { fixture -> fixture.setActive(true) }
                    }
                }
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        body.preProcess.put(ConstKeys.BLOCK) {
            block!!.body.physics.velocity
                .set(body.getPositionPoint(BLOCK_POSITION))
                .sub(block!!.body.getPositionPoint(BLOCK_POSITION))
                .scl(1f / ConstVals.FIXED_TIME_STEP)
        }

        addComponent(
            DrawableShapesComponent(
                prodShapeSuppliers = debugShapes,
                /* TODO: debugShapeSuppliers = debugShapes, */
                debug = true
            )
        )

        val fixtureShape = GameCircle().setRadius(FIXTURE_RADIUS * ConstVals.PPM / 2f)

        return BodyComponentCreator.create(
            body = body,
            entity = this,
            debugShapes = debugShapes,
            bodyFixtureDefs = BodyFixtureDef.of(
                FixtureType.BODY pairTo fixtureShape.copy(),
                FixtureType.DAMAGER pairTo fixtureShape.copy()
            )
        )
    }

    // TODO
    private fun defineSpritesComponent() = SpritesComponentBuilder().build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG
}
