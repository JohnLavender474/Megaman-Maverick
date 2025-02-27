package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.RotatingLine
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
        const val TAG = "BigAssMaverickRobot/Hand"

        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 2f

        private const val SPRITE_SIZE = 3f

        private const val BLOCK_WIDTH = 1.5f
        private const val BLOCK_HEIGHT = 0.25f
        private val BLOCK_POSITION = Position.TOP_CENTER

        private const val LAUNCH_DELAY = 1f
        private const val LAUNCH_SPEED = 8f

        private const val RETURN_DELAY = 1f
        private const val RETURN_SPEED = 4f

        private val animDefs = orderedMapOf<String, AnimationDef>(
            "still" pairTo AnimationDef(),
            "rocket" pairTo AnimationDef(2, 2, 0.05f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class BigAssMaverickRobotHandState { ROTATE, LAUNCH, RETURN }

    override var owner: IGameEntity? = null

    lateinit var state: BigAssMaverickRobotHandState
        private set

    private var block: Block? = null
    private val blockTarget: Vector2
        get() = body.getPositionPoint(Position.TOP_CENTER).sub(0f, 0.5f * ConstVals.PPM)
    private lateinit var rotatingLine: RotatingLine

    private val launchDelay = Timer(LAUNCH_DELAY)
    private val launchTarget = Vector2()

    private val returnDelay = Timer(RETURN_DELAY)
    private val returnTarget = Vector2()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, BigAssMaverickRobot::class)!!

        val origin = spawnProps.get(ConstKeys.ORIGIN, Vector2::class)!!
        val radius = spawnProps.get(ConstKeys.RADIUS, Float::class)!!
        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!
        rotatingLine = RotatingLine(origin.cpy(), radius, speed)

        state = BigAssMaverickRobotHandState.ROTATE

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
                "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false,
                ConstKeys.WIDTH pairTo BLOCK_WIDTH * ConstVals.PPM,
                ConstKeys.HEIGHT pairTo BLOCK_HEIGHT * ConstVals.PPM,
                ConstKeys.BODY_LABELS pairTo objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY),
            )
        )
        this.block = block
    }

    internal fun launch() {
        state = BigAssMaverickRobotHandState.LAUNCH

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
            BigAssMaverickRobotHandState.ROTATE -> {
                rotatingLine.update(delta)
                val center = rotatingLine.getMotionValue()!!
                body.setCenter(center.x, center.y)
            }

            BigAssMaverickRobotHandState.LAUNCH -> {
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
                    state = BigAssMaverickRobotHandState.RETURN

                    returnDelay.reset()
                }
            }

            BigAssMaverickRobotHandState.RETURN -> {
                returnDelay.update(delta)
                if (!returnDelay.isFinished()) {
                    body.physics.velocity.setZero()
                    return@UpdatablesComponent
                }

                body.physics.velocity.set(returnTarget).sub(body.getCenter()).nor().scl(RETURN_SPEED * ConstVals.PPM)

                if (body.getCenter().epsilonEquals(returnTarget, 0.1f * ConstVals.PPM)) {
                    state = BigAssMaverickRobotHandState.ROTATE

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
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            block!!.body.physics.velocity
                .set(blockTarget)
                .sub(block!!.body.getPositionPoint(BLOCK_POSITION))
                .scl(1f / ConstVals.FIXED_TIME_STEP)

            damagerFixture.setActive(state != BigAssMaverickRobotHandState.ROTATE)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.SHIELD))
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(SPRITE_SIZE * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = Position.TOP_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (state == BigAssMaverickRobotHandState.ROTATE) "still" else "rocket" }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG
}
